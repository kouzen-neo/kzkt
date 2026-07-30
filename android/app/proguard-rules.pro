# ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# OpenCV
-keep class org.opencv.** { *; }
-keepclasseswithmembernames class org.opencv.** {
    native <methods>;
}
-keepclasseswithmembernames class * {
    native <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }

# Keep our model classes
-keep class com.cypy.app.core.** { *; }
