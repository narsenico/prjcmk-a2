package it.amonshore.comikkua.workers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.CancellationSignal
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsRepository
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.data.release.ReleaseRepository
import it.amonshore.comikkua.data.web.AvailableComics
import it.amonshore.comikkua.data.web.CmkWebRepository
import it.amonshore.comikkua.toLocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class ImportFromOldDatabaseWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val _cancellationSignal = CancellationSignal()
    private val _job = Job().also {
        it.invokeOnCompletion { cause ->
            when (cause) {
                null -> {}
                is CancellationException -> _cancellationSignal.cancel()
                else -> LogHelper.e("Job failed", cause)
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO + _job) context@{
        getOldDatabasePath()?.let {
            try {
                import(it)
            } catch (ex: Exception) {
                LogHelper.e("Error importing old database data", ex)
                Result.failure(workDataOf("reason" to "error"))
            }
        } ?: Result.failure(workDataOf("reason" to "old database not found"))
    }

    private suspend fun import(oldDatabasePath: String): Result {
        val cmkWebRepository = CmkWebRepository(applicationContext)
        val availableCount = cmkWebRepository.refreshAvailableComics().getOrThrow()
        if (availableCount == 0) {
            LogHelper.w("import failed: available comics not found")
            return Result.failure(workDataOf("reason" to "Available comics not found"))
        }

        val comicsRepository = ComicsRepository(applicationContext)
        if (comicsRepository.count() > 0) {
            LogHelper.w("import failed: current db not empty")
            return Result.failure(workDataOf("reason" to "Current database is not empty"))
        }

        LogHelper.d { "starting import from old database..." }
        return openOldDatabase(oldDatabasePath).use { db ->
            val releaseRepository = ReleaseRepository(applicationContext)
            val availableComicsList = cmkWebRepository.getAvailableComicsList()

            val count = readAsFlow(db)
                .dropWhile {
                    _cancellationSignal.isCanceled
                }
                .map { cr ->
                    availableComicsList.findClosestAndAssignTo(cr).ensureImage()
                }
                .onEach { cr ->
                    comicsRepository.insert(cr.comics)
                    releaseRepository.insertReleases(cr.releases)
                }
                .count()

            LogHelper.i("import complete count=$count")
            Result.success(workDataOf("count" to count))
        }
    }

    private fun getOldDatabasePath(): String? {
        val file = applicationContext.getDatabasePath(OLD_DATABASE_NAME)
        return if (file.exists()) file.path else null
    }

    private fun openOldDatabase(path: String): SQLiteDatabase =
        SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)

    private fun readAsFlow(
        oldDatabase: SQLiteDatabase
    ): Flow<ComicsWithReleases> =
        oldDatabase.readComics()
            .asFlow()
            .map { oldDatabase.readReleasesOf(it) }
            .cancellable()

    private fun SQLiteDatabase.readComics(): List<Comics> = rawQuery(
        READ_COMICS_QUERY,
        emptyArray(),
        _cancellationSignal
    ).use { cursor ->
        generateSequence { if (cursor.moveToNext()) cursor else null }
            .dropWhile { _cancellationSignal.isCanceled }
            .map { it.toComics() }
            .toList()
    }

    private fun Cursor.toComics() = Comics(
        id = getLong(0),
        name = getString(1),
        series = getStringOrDefault(2),
        publisher = getStringOrDefault(3),
        authors = getStringOrDefault(4),
        price = getDoubleOrDefault(5),
        periodicity = getStringOrNull(6),
        reserved = getBoolean(7),
        notes = getStringOrDefault(8),
        image = getStringOrNull(9),
        selected = true,
    )

    private fun SQLiteDatabase.readReleases(comicsId: Long): List<Release> = rawQuery(
        READ_RELEASES_QUERY,
        arrayOf(comicsId.toString()),
        _cancellationSignal
    ).use { cursor ->
        generateSequence { if (cursor.moveToNext()) cursor else null }
            .dropWhile { _cancellationSignal.isCanceled }
            .map { it.toRelease() }
            .toList()
    }

    private fun Cursor.toRelease() = Release(
        id = getLong(0),
        comicsId = getLong(1),
        number = getInt(2),
        date = getDateOrNull(3),
        price = getDoubleOrDefault(4),
        purchased = getBoolean(5),
        ordered = getBoolean(6),
        notes = getStringOrNull(7),
        lastUpdate = getLong(8),
        tag = getStringOrNull(9),
    )

    private fun SQLiteDatabase.readReleasesOf(comics: Comics): ComicsWithReleases {
        val releases = readReleases(comics.id)
        return ComicsWithReleases(comics, releases)
    }

    private fun List<AvailableComics>.findClosestAndAssignTo(comicsWithReleases: ComicsWithReleases): ComicsWithReleases {
        val name = comicsWithReleases.comics.name.lowercase()
        val sourceComics = find { name == it.name.lowercase() }
        return if (sourceComics == null) {
            comicsWithReleases
        } else {
            comicsWithReleases.copy(comics = comicsWithReleases.comics.copy(sourceId = sourceComics.sourceId))
        }
    }

    private fun ComicsWithReleases.ensureImage(): ComicsWithReleases {
        return comics.image?.toUri()?.path?.let { File(it) }?.let {
            val fileName = it.name
            val localFile = File(applicationContext.filesDir, fileName)
            if (localFile.exists()) {
                LogHelper.d { "image for '${comics.name}' found at $localFile" }
                return copy(comics = comics.copy(image = Uri.fromFile(localFile).toString()))
            }

            return copy(comics = comics.copy(image = null))
        } ?: this
    }

    private fun Cursor.getStringOrDefault(index: Int): String = getStringOrNull(index) ?: ""

    private fun Cursor.getDoubleOrDefault(index: Int): Double = getDoubleOrNull(index) ?: 0.0

    private fun Cursor.getDateOrNull(index: Int): LocalDate? = getStringOrNull(index)?.toLocalDate()

    private fun Cursor.getBoolean(index: Int): Boolean = getInt(index) == 1

    companion object {
        val WORK_NAME: String = ImportFromOldDatabaseWorker::class.java.name

        private const val OLD_DATABASE_NAME = "comikku_database"

        private const val READ_COMICS_QUERY = """
            SELECT 
            id, name, series, publisher, authors, price, periodicity, reserved, notes, image
            FROM tComics 
            WHERE removed=0
            """

        private const val READ_RELEASES_QUERY = """
            SELECT 
            id, comicsId, number, date, price, purchased, ordered, notes, lastUpdate, tag
            FROM tReleases 
            WHERE 
            comicsId=?
            AND removed=0            
        """
    }
}