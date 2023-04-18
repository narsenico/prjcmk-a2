package it.amonshore.comikkua

import androidx.annotation.IntDef

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class BackupExclude

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
@IntDef(
    RELEASE_LOST,
    RELEASE_MISSING,
    RELEASE_DATED,
    RELEASE_DATED_NEXT,
    RELEASE_NOT_PURCHASED,
    RELEASE_PURCHASED,
    RELEASE_OTHER,
    RELEASE_NEW
)
annotation class ReleaseTypeDef