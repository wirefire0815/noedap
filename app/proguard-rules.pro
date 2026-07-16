# Android default proguard rules
-keep class androidx.** { *; }
-keep class com.google.** { *; }

# Kotlin specific rules
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }

# Room database
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.Database { *; }
-keep class * extends androidx.room.Entity { *; }
-keep class * extends androidx.room.Dao { *; }

# Keep model classes for Room
-keep class dev.whitefire.noedap.domain.model.** { *; }
-keep class dev.whitefire.noedap.data.local.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Keep R classes
-keep class **.* { *; }

# Keep all activities
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# Keep all view models
-keep public class * extends androidx.lifecycle.ViewModel

# Keep all fragments
-keep public class * extends androidx.fragment.app.Fragment

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
