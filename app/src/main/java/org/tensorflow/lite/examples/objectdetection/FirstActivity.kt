package org.tensorflow.lite.examples.objectdetection

//import org.tensorflow.lite.examples.objectdetection.R

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.media.MediaPlayer
import android.os.Build
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import org.tensorflow.lite.examples.objectdetection.MainActivity

class FirstActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)
        val phoneNumber = ""
        // Initialize the MediaPlayer instance with the audio file
        mediaPlayer = MediaPlayer.create(this, R.raw.welcome) // Replace "welcome" with the name of your audio file in the res/raw folder
        mediaPlayer.setOnCompletionListener(MediaPlayer.OnCompletionListener {
            it // this is MediaPlayer type
            mediaPlayer.pause()
            mediaPlayer.seekTo(0)
        })
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val emergencyContact = sharedPreferences.getString("emergencyContact", null)

        if (emergencyContact == null) {
            showEmergencyContactDialog()
        }


        // Get references to the buttons
        val btnNextPage: Button = findViewById(R.id.btnNextPage)
        val btnNextPage2: Button = findViewById(R.id.btnNextPage2)

        // Set buttons to be non-focusable and non-clickable
        btnNextPage.isFocusable = false
        btnNextPage.isClickable = false
        btnNextPage2.isFocusable = false
        btnNextPage2.isClickable = false

        btnNextPage.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
            // Handle button click, navigate to the next activity (MainActivity)
            val intent = Intent(this@FirstActivity, MainActivity::class.java)
            startActivity(intent)
        }

        btnNextPage2.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
            // Handle button click, navigate to the next activity (MapsActivity)
            val intent = Intent(this@FirstActivity, MapsActivity::class.java)
            startActivity(intent)
        }
    }
    private fun showEmergencyContactDialog() {
        val editText = EditText(this)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Emergency Contact")
            .setMessage("Please enter an emergency contact number:")
            .setView(editText)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val contactNumber = editText.text.toString()
                if (contactNumber.isNotEmpty()) {
                    saveEmergencyContact(contactNumber)
                }
            }
            .create()

        dialog.show()
    }

    private fun saveEmergencyContact(contactNumber: String) {
        val editor = sharedPreferences.edit()
        editor.putString("emergencyContact", contactNumber)
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        // Resume audio playback when the activity is resumed
        mediaPlayer.start()
    }

    override fun onPause() {
        super.onPause()
        // Pause and reset the audio player when the activity is paused
        mediaPlayer.pause()
        mediaPlayer.seekTo(0)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the media player resources when the activity is destroyed
        mediaPlayer.release()
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Workaround for Android Q memory leak issue in IRequestFinishCallback$Stub.
            // (https://issuetracker.google.com/issues/139738913)
            finishAfterTransition()
        } else {
            super.onBackPressed()
        }
    }
}
