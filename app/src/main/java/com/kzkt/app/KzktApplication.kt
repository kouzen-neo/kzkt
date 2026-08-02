package com.kzkt.app

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class KzktApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Load native library explicitly (prior to initLocal)
        try {
            System.loadLibrary("opencv_java4")
            Log.d("KZKT", "Native opencv_java4 loaded via System.loadLibrary")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("KZKT", "CRITICAL: System.loadLibrary opencv_java4 failed: ${e.message}", e)
        }

        // 2. Initialize OpenCV JNI bindings (registers Mat.n_Mat(), etc.)
        if (OpenCVLoader.initLocal()) {
            Log.d("KZKT", "OpenCV JNI initialized via initLocal()")
        } else {
            Log.e("KZKT", "OpenCV initLocal() returned false")
        }
    }

    companion object {
        lateinit var instance: KzktApplication
            private set
    }
}
