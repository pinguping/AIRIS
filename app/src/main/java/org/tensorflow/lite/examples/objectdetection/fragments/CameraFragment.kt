/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tensorflow.lite.examples.objectdetection.fragments

//import org.tensorflow.lite.examples.objectdetection.BuildConfig

// TTS
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.tensorflow.lite.examples.objectdetection.BuildConfig
import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper
import org.tensorflow.lite.examples.objectdetection.R
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentCameraBinding
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.telephony.SmsManager


class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener, TextToSpeech.OnInitListener {

    private val TAG = "ObjectDetection"
    val phoneNumber = "nil" // Replace with the actual phone number

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    // TTS class properties
    private lateinit var tts: TextToSpeech
    private var isTtsInitialized = false
    private var isTextToSpeechActive = false
    private var isPlayingTTS = false
    private var capturedText: String = ""
    private var isTtsPlayFailDueToTextEmpty = false

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    // Initialize siren MediaPlayer
    private lateinit var sirenMediaPlayer: MediaPlayer
    private var isSirenPlaying: Boolean = false

    private lateinit var mediaPlayer: MediaPlayer
    private var isKotlinFun: Boolean = false
    private var isKotlinOff: Boolean = false

    private var isEnglish: Boolean = true
    private var isJapanese: Boolean = false
    private var isGerman: Boolean = false

    private var minHeight: Int = 20
    private var isSoundEnabled: Boolean = false

    // TTS -------- start
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.voices.firstOrNull { it.name.contains(Regex("en-us-x-iom-local|en-us-x-iol-local|en-us-x-tpc-local")) }
                ?.let { voice ->
                    tts.voice = voice
                }
            isTtsInitialized = true
        } else {
            // TTS initialization failed, handle the error if necessary
        }
    }
    // TTS -------- end

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()

        // TTS ------- Shut down Text-to-Speech engine and release resources
        tts.stop()
        tts.shutdown()
        // TTS ------- end
    }

    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        fragmentCameraBinding.volume.switch1.setOnLongClickListener {
            // Toggle the sound option when the switch is long pressed
            isSoundEnabled = !isSoundEnabled
            // Perform your action here based on the isSoundEnabled state
            if (isSoundEnabled) {
                playSirenSound()
            }
            // Return true to indicate that the long press event is consumed
            true
        }
        fragmentCameraBinding.langMenu.langMenu.setOnClickListener{
            val popupMenu = PopupMenu(activity?.applicationContext, it)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId){
                    R.id.eng_ -> {
                        isEnglish = true
                        isJapanese = false
                        isGerman = false
                        Toast.makeText(activity?.applicationContext, "English",Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.cn_ -> {
                        Toast.makeText(activity?.applicationContext, "Chinese",Toast.LENGTH_SHORT).show()
                        isEnglish = false
                        isJapanese = true
                        isGerman = false
                        true
                    }
                    R.id.mal_ -> {
                        Toast.makeText(activity?.applicationContext, "Malay",Toast.LENGTH_SHORT).show()
                        isEnglish = false
                        isJapanese = false
                        isGerman = true
                        true
                    }
                    else -> false
                }
            }
            popupMenu.inflate(R.menu.lang_menu)
            popupMenu.show()
        }

        return fragmentCameraBinding.root
    }


    // Function to play the siren sound
    private fun playSirenSound() {
        if (!isSirenPlaying) {
            // Check if the MediaPlayer is already initialized and not playing
            if (!this::sirenMediaPlayer.isInitialized) {
                sirenMediaPlayer = MediaPlayer.create(requireContext(), R.raw.siren_sound)
            } else if (!sirenMediaPlayer.isPlaying) {
                sirenMediaPlayer.prepare()
                sirenMediaPlayer.seekTo(0)
            }

            sirenMediaPlayer.setOnCompletionListener {
                // Reset the MediaPlayer after the siren sound completes playing
                sirenMediaPlayer.pause()
                sirenMediaPlayer.seekTo(0)
                isSirenPlaying = false
            }

            sirenMediaPlayer.start()
            isSirenPlaying = true
        if(phoneNumber != "nil") {
            // Send the SMS when the siren starts playing (on long press)
            sendSMS(phoneNumber, "Help, I'm blind and in trouble")
        }
        }
    }
    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val smsManager: SmsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Toast.makeText(
                    requireContext(),
                    "SMS sent successfully to $phoneNumber",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Request the SMS permission at runtime
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.SEND_SMS),
                    PERMISSION_SEND_SMS_REQUEST_CODE
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "SMS sending failed: ${e.message}")
            Toast.makeText(requireContext(), "SMS sending failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makePhoneCall() {
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber"))
        startActivity(callIntent)
    }

    // Inside your class
    private val PERMISSION_MAKE_CALL_REQUEST_CODE = 124 // You can use any value

    // Inside your class, where you want to initiate the phone call
    private fun initiatePhoneCall() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted, make the phone call
            makePhoneCall()
        } else {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CALL_PHONE),
                PERMISSION_MAKE_CALL_REQUEST_CODE
            )
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_MAKE_CALL_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, make the phone call
                makePhoneCall()
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(requireContext(), "Call permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val PERMISSION_SEND_SMS_REQUEST_CODE = 123 // Choose any value you prefer
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this)

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }

        // Attach listeners to UI control widgets
        initBottomSheetControls()

        // TTS ----------- Initialize Text-to-Speech
        tts = TextToSpeech(requireContext(), this)

        // Set up double tap gesture listener on camera preview view
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent?) {
                // Toggle the sound option when the switch is long pressed
                isSoundEnabled = !isSoundEnabled
                // Perform your action here based on the isSoundEnabled state
                if (isSoundEnabled) {
                    playSirenSound()
                } else {
                    sirenMediaPlayer.stop()
                    isSirenPlaying = false
                }
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (isTtsInitialized) {
                    // Toggle Text-to-Speech activation
                    isTextToSpeechActive = !isTextToSpeechActive
                    if (isTextToSpeechActive) {
                        // Start Text-to-Speech
                        isPlayingTTS = true
                        tts.speak("Switching to text reading mode.", TextToSpeech.QUEUE_FLUSH, null, null)
                        readTextFromCamera()
                    } else {
                        // Stop Text-to-Speech
                        tts.stop()
                        tts.speak("Switching to object detection mode.", TextToSpeech.QUEUE_FLUSH, null, "switch")
                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String) {}

                            override fun onDone(utteranceId: String) {
                                // TTS playback completed for the given utterance
                                if (utteranceId == "switch") {
                                    isPlayingTTS = false
                                }
                            }

                            override fun onError(utteranceId: String) {
                                isPlayingTTS = false
                            }
                        })
                    }
                }
                return super.onDoubleTap(e)
            }
        })

        fragmentCameraBinding.volume.switch1.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        // TTS -------- end

    }

    // TTS -------- start
    private fun readTextFromCamera() {
        // Check if there is any text to read
        if (capturedText.isNotBlank()) {
            // Speak the captured text
            tts.speak(capturedText, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            isTtsPlayFailDueToTextEmpty = true
        }
    }
    // TTS -------- end

    private fun initBottomSheetControls() {
        // When clicked, lower detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1) {
                objectDetectorHelper.threshold -= 0.1f
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8) {
                objectDetectorHelper.threshold += 0.1f
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of objects that can be detected at a time
        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        // When clicked, decrease the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
            if (objectDetectorHelper.numThreads > 1) {
                objectDetectorHelper.numThreads--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of threads used for detection
        fragmentCameraBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
            if (objectDetectorHelper.numThreads < 4) {
                objectDetectorHelper.numThreads++
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU
        // GPU, and NNAPI
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentDelegate = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }

        // When clicked, change the underlying model used for object detection
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    objectDetectorHelper.currentModel = p2
                    updateControlsUi()
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    /* no op */
                }
            }
    }

    // Update the values displayed in the bottom sheet. Reset detector.
    private fun updateControlsUi() {
        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text =
            objectDetectorHelper.maxResults.toString()
        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
            String.format("%.2f", objectDetectorHelper.threshold)
        fragmentCameraBinding.bottomSheetLayout.threadsValue.text =
            objectDetectorHelper.numThreads.toString()

        fragmentCameraBinding.volume.switch1.setOnLongClickListener {
            // Toggle the sound option when the switch is long pressed
            isSoundEnabled = !isSoundEnabled
            // Perform your action here based on the isSoundEnabled state
            if (isSoundEnabled) {
                playSirenSound()
            }
            // Return true to indicate that the long press event is consumed
            true
        }

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        objectDetectorHelper.clearObjectDetector()
        fragmentCameraBinding.overlay.clear()
    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                              image.width,
                              image.height,
                              Bitmap.Config.ARGB_8888
                            )
                        }

                        detectObjects(image)
                    }
                }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        recognizeText()
        if (isPlayingTTS) return
        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    private fun recognizeText() {
        val options: TextRecognizerOptions = TextRecognizerOptions.Builder().build()
        val recognizer = TextRecognition.getClient(options)
        val result: Task<Text> = recognizer.process(InputImage.fromBitmap(bitmapBuffer, 0))
        result.addOnSuccessListener { text: Text ->
            // Process the recognized text
            capturedText = text.text
            if (isTtsPlayFailDueToTextEmpty) {
                isTtsPlayFailDueToTextEmpty = false
                readTextFromCamera()
            }
        }.addOnFailureListener { e: Exception? ->
            Log.e("ReadTextError", e.toString())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResults(
      results: MutableList<Detection>?,
      inferenceTime: Long,
      imageHeight: Int,
      imageWidth: Int,
    ) {

        // TTS -------- start
        if (isTextToSpeechActive) {
            // If TTS is active, read the detected text
            readTextFromCamera()
        }
        // TTS --------- end

        activity?.runOnUiThread {
            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
                    String.format("%d ms", inferenceTime)

            val listObjects = mutableListOf<String>()
            for (result in results ?: LinkedList<Detection>()) {

                if (isEnglish) {
                    val inputStreamChange = resources.openRawResource(R.raw.eng_labels0)
                    val bufferedReaderChange = BufferedReader(InputStreamReader(inputStreamChange))

                    val inputStream = resources.openRawResource(R.raw.labels)
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    var i = 0
                    var thresholdAudio = 0.50F
                    for (line in bufferedReader.lines()) {
                        if (line == result.categories[0].label){

                            var j = 0
                            for (lineC in bufferedReaderChange.lines()) {
                                if (j == i) {
                                    val drawableTextLabel =
                                            lineC + " " +
                                                    String.format("%.2f", result.categories[0].score)
                                    if (result.categories[0].score >= thresholdAudio) {
                                        listObjects.add(lineC)

                                        var audioFileName = "eng_" + (i + 1).toString()
                                        val resId: Int = resources.getIdentifier(
                                                audioFileName,
                                                "raw",
                                                BuildConfig.APPLICATION_ID
                                        )
                                        println(result.categories[0].score)
                                        if (!this::mediaPlayer.isInitialized) {
                                            mediaPlayer = MediaPlayer.create(requireContext(), resId)
                                        } else if (!mediaPlayer.isPlaying) {
                                            mediaPlayer = MediaPlayer.create(requireContext(), resId)
                                        }

                                        mediaPlayer.setOnCompletionListener(MediaPlayer.OnCompletionListener {
                                            it // this is MediaPlayer type
                                            mediaPlayer.pause()
                                            mediaPlayer.seekTo(0)
                                        })
                                        mediaPlayer.start()


                                    }

                                }
                                j++
                            }
                        }


                        i++
                    }
                } else if(isJapanese) {
                    val inputStreamChange = resources.openRawResource(R.raw.cn_labels0)
                    val bufferedReaderChange = BufferedReader(InputStreamReader(inputStreamChange))

                    val inputStream = resources.openRawResource(R.raw.labels)
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    var i = 0
                    var thresholdAudio = 0.50F
                    for (line in bufferedReader.lines()) {
                        if (line == result.categories[0].label){

                            var j = 0
                            for (lineC in bufferedReaderChange.lines()) {
                                if (j == i) {
                                    val drawableTextLabel =
                                            lineC + " " +
                                                    String.format("%.2f", result.categories[0].score)
                                    if (result.categories[0].score >= thresholdAudio) {
                                        listObjects.add(lineC)

                                        var audioFileName = "cn_" + (i + 1).toString()
                                        val resId: Int = resources.getIdentifier(
                                                audioFileName,
                                                "raw",
                                                BuildConfig.APPLICATION_ID
                                        )
                                        println(result.categories[0].score)
                                        if (!this::mediaPlayer.isInitialized) {
                                            mediaPlayer = MediaPlayer.create(requireContext(), resId)
                                        } else if (!mediaPlayer.isPlaying) {
                                            mediaPlayer = MediaPlayer.create(requireContext(), resId)
                                        }

                                        mediaPlayer.setOnCompletionListener(MediaPlayer.OnCompletionListener {
                                            it // this is MediaPlayer type
                                            mediaPlayer.pause()
                                            mediaPlayer.seekTo(0)
                                        })
                                        mediaPlayer.start()


                                    }

                                }
                                j++
                            }
                        }


                        i++
                    }
                } else if(isGerman) {
                    val inputStreamChange = resources.openRawResource(R.raw.mal_labels0)
                    val bufferedReaderChange = BufferedReader(InputStreamReader(inputStreamChange))

                    val inputStream = resources.openRawResource(R.raw.labels)
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    var i = 0
                    var thresholdAudio = 0.50F
                    for (line in bufferedReader.lines()) {
                        if (line == result.categories[0].label){

                            var j = 0
                            for (lineC in bufferedReaderChange.lines()) {
                                if (j == i) {
                                    val drawableTextLabel =
                                            lineC + " " +
                                                    String.format("%.2f", result.categories[0].score)
                                    if (result.categories[0].score >= thresholdAudio) {
                                        listObjects.add(lineC)

                                        var audioFileName = "mal_" + (i + 1).toString()
                                        val resId: Int = resources.getIdentifier(
                                                audioFileName,
                                                "raw",
                                                BuildConfig.APPLICATION_ID
                                        )
                                        println(result.categories[0].score)
                                        if (!this::mediaPlayer.isInitialized) {
                                            mediaPlayer = MediaPlayer.create(requireContext(), resId)
                                        } else if (!mediaPlayer.isPlaying) {
                                            mediaPlayer = MediaPlayer.create(requireContext(), resId)
                                        }

                                        mediaPlayer.setOnCompletionListener(MediaPlayer.OnCompletionListener {
                                            it // this is MediaPlayer type
                                            mediaPlayer.pause()
                                            mediaPlayer.seekTo(0)
                                        })
                                        mediaPlayer.start()


                                    }

                                }
                                j++
                            }
                        }


                        i++
                    }
                }


            }
            // Pass necessary information to OverlayView for drawing on the canvas
            fragmentCameraBinding.overlay.setResults(
                    listObjects,
                    imageHeight,
                    imageWidth,
                    results ?: LinkedList<Detection>()
            )

            // Force a redraw
            fragmentCameraBinding.overlay.invalidate()
            val size = listObjects.size
            val arrayList = ArrayList(listObjects)
            if (size == 1) {
                fragmentCameraBinding.results.tv1.text = String.format(listObjects[0])
                fragmentCameraBinding.results.tv2.text = null
                fragmentCameraBinding.results.tv3.text = null
                fragmentCameraBinding.results.tv4.text = null
                fragmentCameraBinding.results.tv1.background = resources.getDrawable(R.drawable.rounded_corner)
                fragmentCameraBinding.results.tv2.background = null
                fragmentCameraBinding.results.tv3.background = null
                fragmentCameraBinding.results.tv4.background = null
            } else if (size == 2) {
                fragmentCameraBinding.results.tv1.text = String.format(listObjects[1])
                fragmentCameraBinding.results.tv2.text = String.format(listObjects[0])
                fragmentCameraBinding.results.tv3.text = null
                fragmentCameraBinding.results.tv4.text = null
                fragmentCameraBinding.results.tv1.background = resources.getDrawable(R.drawable.rounded_corner)
                fragmentCameraBinding.results.tv2.background = resources.getDrawable(R.drawable.rounded_corner)
                fragmentCameraBinding.results.tv3.background = null
                fragmentCameraBinding.results.tv4.background = null
            } else if (size == 3) {
                fragmentCameraBinding.results.tv1.text = String.format(listObjects[2])
                fragmentCameraBinding.results.tv2.text = String.format(listObjects[1])
                fragmentCameraBinding.results.tv3.text = String.format(listObjects[0])
                fragmentCameraBinding.results.tv4.text = null
                fragmentCameraBinding.results.tv1.background = resources.getDrawable(R.drawable.rounded_corner)
                fragmentCameraBinding.results.tv2.background = resources.getDrawable(R.drawable.rounded_corner)
                fragmentCameraBinding.results.tv3.background = resources.getDrawable(R.drawable.rounded_corner)
                fragmentCameraBinding.results.tv4.background = null
            } else if (size >= 4) {
                fragmentCameraBinding.results.tv1.text = String.format(listObjects[3])
                fragmentCameraBinding.results.tv2.text = String.format(listObjects[2])
                fragmentCameraBinding.results.tv3.text = String.format(listObjects[1])
                fragmentCameraBinding.results.tv4.text = String.format(listObjects[0])
                fragmentCameraBinding.results.tv1.background = resources.getDrawable(R.drawable.rounded_corner)
                fragmentCameraBinding.results.tv2.background = resources.getDrawable(R.drawable.rounded_corner)
                fragmentCameraBinding.results.tv3.background = resources.getDrawable(R.drawable.rounded_corner)
                fragmentCameraBinding.results.tv4.background = resources.getDrawable(R.drawable.rounded_corner)
            } else {
                fragmentCameraBinding.results.tv1.text = null
                fragmentCameraBinding.results.tv2.text = null
                fragmentCameraBinding.results.tv3.text = null
                fragmentCameraBinding.results.tv4.text = null
                fragmentCameraBinding.results.tv1.background = null
                fragmentCameraBinding.results.tv2.background = null
                fragmentCameraBinding.results.tv3.background = null
                fragmentCameraBinding.results.tv4.background = null
            }

        }

    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
}
