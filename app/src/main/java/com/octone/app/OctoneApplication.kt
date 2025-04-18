package net.thomasphillips.octone

import android.app.Application
import mn.tck.semitone.PianoEngine

class OctoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PianoEngine.create(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        PianoEngine.destroy()
    }
} 