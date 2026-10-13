package com.benisalla.glitch.model

/**
 * One launchable entry shown in the menu.
 *
 * Identity is the launcher activity component (package + activity), not the
 * package alone, because one package may expose several launcher entries.
 * This type is plain Kotlin so shuffle/session logic can be unit tested on
 * the JVM; icons are loaded separately through [AppCatalog.loadIcon].
 */
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
) {
    /** Stable unique key: the flattened component name. */
    val key: String get() = "$packageName/$activityName"
}
