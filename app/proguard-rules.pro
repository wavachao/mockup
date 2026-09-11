# Keep Room generated implementations.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# WorkManager reflectively instantiates Workers and Receivers.
-keep class * extends androidx.work.ListenableWorker { <init>(...); }
-keep class com.wavachao.timeblock.reminder.** { *; }

# Compose
-dontwarn androidx.compose.**
