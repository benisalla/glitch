# R8 rules for Shuffle App Menu release builds.
#
# Compose, AndroidX and Kotlin ship their own consumer rules, and the app uses no
# reflection, serialization or JNI, so no extra keep rules are needed.
#
# Keep source file and line numbers so Play Console stack traces can be
# de-obfuscated with the uploaded mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
