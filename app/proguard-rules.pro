# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Room database and components
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Database class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Entity class *
-keep class com.example.data.** { *; }

# Keep model classes used for serialization or databases
-keepclassmembers class com.example.data.** {
    <fields>;
    <methods>;
}

# Preserve line numbers for stack traces in release crash logs
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,AnnotationDefault
-renamesourcefileattribute SourceFile

