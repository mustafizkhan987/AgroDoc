# AgroDoc ProGuard / R8 rules

# Keep Room entities and DAOs
-keep class com.agrodoc.app.data.db.** { *; }

# Keep domain models (serialised to/from JSON via Room or Gson)
-keep class com.agrodoc.app.domain.model.** { *; }

# LiteRT / TFLite
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn com.google.ai.edge.litert.**

# Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# DataStore
-keep class androidx.datastore.** { *; }
