package net.currit.tonality

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.PopupWindow
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuCompat
import androidx.databinding.DataBindingUtil
import mn.tck.semitone.PianoEngine
import net.currit.tonality.databinding.ActivityTonalityMainBinding
import net.currit.tonality.databinding.PopupSizingBinding

class TonalityMainActivity : AppCompatActivity() {
    private var scaleController: PianoControlScale? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tonality_main)

        val activityBinding = DataBindingUtil.inflate<ActivityTonalityMainBinding>(
            layoutInflater, R.layout.activity_tonality_main, null, false
        )
        activityBinding.pianoView = activityBinding.piano

        setContentView(activityBinding.root)

        PianoEngine.create(this)

        // setup scale UI elements
        if (savedInstanceState == null) {
            val scaleController = PianoControlScale()
            supportFragmentManager.beginTransaction()
                .replace(R.id.piano_control_scale_container, scaleController)
                .commit()
            scaleController.setPiano(activityBinding.piano)
        } else {
            scaleController = supportFragmentManager.findFragmentById(R.id.piano_control_scale_container) as? PianoControlScale
            scaleController?.setPiano(activityBinding.piano)
        }

        // configure popup_sizing popup
        val binding = DataBindingUtil.inflate<PopupSizingBinding>(
            layoutInflater, R.layout.popup_sizing, null, false
        )
        val popup = PopupWindow(
            binding.root, 
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popup.elevation = 20f
        }
        binding.piano = activityBinding.piano
        binding.popup = popup
        val sizingButton = findViewById<View>(R.id.button_sizing)
        sizingButton.setOnClickListener {
            if (popup.isShowing)
                popup.dismiss()
            else
                // TODO: 2019-05-27 close with back
                popup.showAtLocation(sizingButton, Gravity.CENTER, 0, 0)
        }

        // configure menu/more button
        val moreButton = findViewById<View>(R.id.button_more)
        moreButton.setOnClickListener {
            val popupMenu = PopupMenu(this, moreButton)
            popupMenu.inflate(R.menu.tonality_menu)
            // TODO: 2019-05-28 divider do not show ...
            MenuCompat.setGroupDividerEnabled(popupMenu.menu, true)

            // enable/disable menu entries
            val m = popupMenu.menu
            m.findItem(R.id.menu_switch_labelnotes).isChecked = activityBinding.piano.isLabelNotes()
            m.findItem(R.id.menu_switch_labelc).apply {
                isChecked = activityBinding.piano.isLabelC()
                isEnabled = activityBinding.piano.isLabelNotes()
            }
            m.findItem(R.id.menu_switch_circleoffifths).isChecked = scaleController?.isUseCircleOfFifthSelector() ?: false
            m.findItem(R.id.menu_switch_labelintervals).isChecked = activityBinding.piano.isLabelIntervals()
            m.findItem(R.id.menu_switch_rows_top_down).isChecked = activityBinding.piano.getRowsTopDown()
//            m.findItem(R.id.menu_theme_less).isChecked = activityBinding.piano.theme == TonalityPianoView.THEME.LESS_COLORS

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_about -> {
                        startActivity(Intent(applicationContext, AboutTonalityActivity::class.java))
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
                        scaleController?.toggleCircleOfFifthsSelector()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        // Hide UI
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN) // hide notification bar

        if (!activityBinding.piano.isPaused()) {
            // ... existing code ...
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