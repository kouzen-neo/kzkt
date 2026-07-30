package com.cypy.app

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class CypyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Load native library explicitly (prior to initLocal)
        try {
            System.loadLibrary("opencv_java4")
            Log.d("CYPY", "Native opencv_java4 loaded via System.loadLibrary")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("CYPY", "CRITICAL: System.loadLibrary opencv_java4 failed: ${e.message}", e)
        }

        // 2. Initialize OpenCV JNI bindings (registers Mat.n_Mat(), etc.)
        if (OpenCVLoader.initLocal()) {
            Log.d("CYPY", "OpenCV JNI initialized via initLocal()")
        } else {
            Log.e("CYPY", "OpenCV initLocal() returned false")
        }
    }

    companion object {
        lateinit var instance: CypyApplication
            private set
    }
}
