# Retrofit / OkHttp / Moshi keep rules (release builds if minify is enabled).
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, RuntimeVisibleAnnotations, AnnotationDefault
-keep class com.basemax.smsforwarder.data.model.** { *; }
-keep class kotlin.Metadata { *; }
