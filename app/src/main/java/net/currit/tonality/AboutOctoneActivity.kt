package net.thomasphillips.octone

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import android.webkit.WebView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class AboutOctoneActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create root layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        setContentView(rootLayout)

        // Add version TextView
        val versionTextView = TextView(this).apply {
            text = "1.3-dev"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(16, 16, 16, 16)
        }
        rootLayout.addView(versionTextView)

        // Add WebView
        val wv = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            loadUrl("file:///android_asset/about-octone.html")
            setBackgroundColor(Color.TRANSPARENT)
        }
        rootLayout.addView(wv)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val title = getString(R.string.app_name)
        supportActionBar?.title = getString(R.string.about_title, title)

        // Hide UI
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN) // hide notification bar

        // Handle back button press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 