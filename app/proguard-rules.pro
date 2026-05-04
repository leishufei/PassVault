# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @javax.inject.* *;
    @dagger.* *;
}

-keep class androidx.security.crypto.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

-keepclassmembers class * extends androidx.room.RoomDatabase {
    *;
}
