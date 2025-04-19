package com.octone.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import mn.tck.semitone.PianoEngine
import com.octone.app.databinding.ActivityOctoneMainBinding

class OctoneMainActivity : AppCompatActivity() {
    private var scaleController: PianoControlScale? = null
    private lateinit var activityBinding: ActivityOctoneMainBinding
    private lateinit var popupSizingMenu: PopupSizingMenu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        activityBinding = DataBindingUtil.inflate(
            layoutInflater, R.layout.activity_octone_main, null, false
        )
        activityBinding.pianoView = activityBinding.piano
        setContentView(activityBinding.root)

        // setup scale UI elements
        if (savedInstanceState == null) {
            scaleController = PianoControlScale()
            supportFragmentManager.beginTransaction()
                .replace(R.id.piano_control_scale_container, scaleController!!)
                .commit()
            scaleController?.setPiano(activityBinding.piano)
        } else {
            scaleController = supportFragmentManager.findFragmentById(R.id.piano_control_scale_container) as? PianoControlScale
            scaleController?.setPiano(activityBinding.piano)
        }

        // configure popup_sizing popup
        popupSizingMenu = PopupSizingMenu(this)
        popupSizingMenu.setOnRowsMinusClickListener { activityBinding.piano.removeRow(it) }
        popupSizingMenu.setOnRowsPlusClickListener { activityBinding.piano.addRow(it) }
        popupSizingMenu.setOnKeysMinusClickListener { activityBinding.piano.removeKey(it) }
        popupSizingMenu.setOnKeysPlusClickListener { activityBinding.piano.addKey(it) }
        popupSizingMenu.setOnResetClickListener { activityBinding.piano.reset(it) }

        activityBinding.piano.setOnClickListener {
            popupSizingMenu.show(it)
        }

        // configure menu/more button
        val moreButton = findViewById<View>(R.id.menu_button)
        moreButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            MenuCompat.setGroupDividerEnabled(popupMenu.menu, true)
            popupMenu.menuInflater.inflate(R.menu.menu_main, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_about -> {
                        startActivity(Intent(applicationContext, AboutOctoneActivity::class.java))
                        true
                    }
                    R.id.menu_switch_labelnotes -> {
                        activityBinding.piano.toggleLabelNotes()
                        true
                    }
                    R.id.menu_switch_labelc -> {
                        activityBinding.piano.toggleLabelC()
                        true
                    }
                    R.id.menu_switch_labelintervals -> {
                        activityBinding.piano.toggleLabelIntervals()
                        true
                    }
                    R.id.menu_switch_rows_top_down -> {
                        activityBinding.piano.toggleRowsTopDown()
                        true
                    }
                    R.id.menu_switch_circleoffifths -> {
                        scaleController?.toggleCircleOfFifthSelector()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        // Hide UI
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onPause() {
        super.onPause()
        PianoEngine.pause()
    }

    override fun onResume() {
        super.onResume()
        if (PianoEngine.isPaused()) PianoEngine.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        PianoEngine.destroy()
    }
} 