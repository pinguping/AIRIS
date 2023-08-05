/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.*
import android.media.MediaPlayer
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.math.max


class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var labels: MutableList<String> = mutableListOf<String>()
    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var rsltBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var textrsltPaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = Rect()
    private lateinit var mediaPlayer: MediaPlayer
    

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textrsltPaint.reset()
        textBackgroundPaint.reset()
        rsltBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        rsltBackgroundPaint.color = Color.CYAN
        rsltBackgroundPaint.style = Paint.Style.FILL
        rsltBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        textrsltPaint.color = Color.BLACK
        textrsltPaint.style = Paint.Style.FILL
        textrsltPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun draw(canvas: Canvas) {
        super.draw(canvas)


//        Log.d("Labels", bufferedReader.readText())
        var n = 0
        for (result in results) {
            val boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside detected objects
            val drawableText =
                    labels[n] + " " +
                        String.format("%.2f", result.categories[0].score)

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            val drawableTextLabel =
                    labels[n] + " " +
                            String.format("%.2f", result.categories[0].score)
            canvas.drawText(drawableTextLabel, left, top + bounds.height(), textPaint)

            n++



        }
    }


    private fun requireContext(): Context {
        return this.context
                ?: throw IllegalStateException("Fragment $this not attached to a context.")
    }

    fun setResults(
            categoryResults: MutableList<String>,
            imageHeight: Int,
            imageWidth: Int,
            detections: MutableList<Detection>,

            ) {
        labels = categoryResults
        results = detections

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}

