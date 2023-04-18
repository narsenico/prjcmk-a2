package it.amonshore.comikkua

import android.content.Context
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.`is`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.Month
import java.util.Locale

const val LANG: String = "en"
const val COUNTRY: String = "EN"

@RunWith(AndroidJUnit4::class)
class DateExtTest {

    private lateinit var _context: Context

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val locale = Locale(LANG, COUNTRY).also { Locale.setDefault(it) }
        val res = context.resources
        val config = res.configuration.apply { setLocales(LocaleList(locale)) }
        _context = context.createConfigurationContext(config)
    }

    @Test
    fun release_date_to_local_date() {
        // Arrange
        val releaseDate = "20230415"
        val expected = LocalDate.of(2023, Month.APRIL, 15)

        // Act
        val date = releaseDate.toLocalDate()

        // Assert
        assertThat(date, `is`(expected))
    }

    @Test
    fun today_to_human_readable_string() {
        // Arrange
        val today = LocalDate.now()
        val expected = "Today"

        // Act
        val str = today.toHumanReadable(_context)
        val strLong = today.toHumanReadableLong(_context)

        // Assert
        assertThat(str, `is`(expected))
        assertThat(strLong, `is`(expected))
    }

    @Test
    fun tomorrow_to_human_readable_string() {
        // Arrange
        val tomorrow = LocalDate.now().plusDays(1)
        val expected = "Tomorrow"

        // Act
        val str = tomorrow.toHumanReadable(_context)
        val strLong = tomorrow.toHumanReadableLong(_context)

        // Assert
        assertThat(str, `is`(expected))
        assertThat(strLong, `is`(expected))
    }

    @Test
    fun date_to_human_readable_string() {
        // Arrange
        val date = LocalDate.of(2023, Month.APRIL, 15)
        val expected = "Sat 15 Apr"

        // Act
        val str = date.toHumanReadable(_context)

        // Assert
        assertThat(str, `is`(expected))
    }

    @Test
    fun date_to_human_readable_string_long() {
        // Arrange
        val date = LocalDate.of(2023, Month.APRIL, 15)
        val expected = "Saturday, April 15, 2023"

        // Act
        val str = date.toHumanReadableLong(_context)

        // Assert
        assertThat(str, `is`(expected))
    }
}