package com.octone.app

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class AboutOctoneActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_octone)

        val webView = findViewById<WebView>(R.id.webview)
        webView.loadUrl("file:///android_asset/${getString(R.string.about_tonality_file)}")
    }
} 