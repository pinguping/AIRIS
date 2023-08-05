package org.tensorflow.lite.examples.objectdetection.fragments

import android.view.GestureDetector
import android.view.MotionEvent

class GestureHelper(private val listener: OnDoubleTapListener) : GestureDetector.SimpleOnGestureListener() {

    interface OnDoubleTapListener {
        fun onDoubleTap()
    }

    private var lastTapTime: Long = 0

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTapTime < 500) {
            listener.onDoubleTap()
        }
        lastTapTime = currentTime
        return true
    }
}
