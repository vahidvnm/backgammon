# Persisted settings and saved matches use enum names. Keep valueOf/values stable.
-keepclassmembers enum com.arena.backgammon.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Android ViewModel constructors discoverable.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Preserve source/line information for locally inspected release crashes.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
