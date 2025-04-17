package net.currit.tonality

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import net.currit.tonality.databinding.PopupScaleBinding

class PianoControlScale : Fragment() {

    private val PREF_CIRCLEOFFIFTHSSELECTOR = "circleoffifths_selector"
    private val PREF_CIRCLEOFFIFTHSSELECTOR_DEFAULT = true

    private var activity: TonalityMainActivity? = null
    private var popup: PopupWindow? = null

    private var rootNoteButton: Button? = null
    private var scaleNameButton: Button? = null

    private lateinit var noteNames: Array<String>
    private lateinit var scaleNames: Array<String>

    private var useCircleOfFifthSelector: Boolean = false
    private var piano: TonalityPianoView? = null

    fun setPiano(piano: TonalityPianoView) {
        this.piano = piano
        piano.getRootNote()?.let { rootNote ->
            rootNoteButton?.text = noteNames[rootNote]
        }
        piano.getScale()?.let { scale ->
            scaleNameButton?.text = scaleNames[scale]
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TonalityMainActivity) {
            activity = context
        }
        noteNames = resources.getStringArray(R.array.noteNames)
        scaleNames = resources.getStringArray(R.array.scaleNames)
    }

    override fun onDetach() {
        super.onDetach()
        activity = null
        piano = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_piano_control_scale, container, false)

        // setup root note selection dialogue
        val sp = PreferenceManager.getDefaultSharedPreferences(requireContext())
        useCircleOfFifthSelector = sp.getBoolean(PREF_CIRCLEOFFIFTHSSELECTOR, PREF_CIRCLEOFFIFTHSSELECTOR_DEFAULT)

        // Initialize buttons
        rootNoteButton = view.findViewById(R.id.button_root_note)
        scaleNameButton = view.findViewById(R.id.button_scale)

        setupNoteSelector(view)

        // setup scale dialog
        scaleNameButton?.setOnClickListener {
            val builder = AlertDialog.Builder(requireActivity())
            builder.setTitle(R.string.title_scale_name)

            builder.setItems(scaleNames) { dialog, item ->
                piano?.setScale(item)
                scaleNameButton?.text = scaleNames[item]
            }

            val alert = builder.create()
            alert.show()
        }

        return view
    }

    fun toggleCircleOfFifthsSelector() {
        useCircleOfFifthSelector = !useCircleOfFifthSelector

        // store in preferences
        val editor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
        editor.putBoolean(PREF_CIRCLEOFFIFTHSSELECTOR, useCircleOfFifthSelector)
        editor.apply()

        view?.let { setupNoteSelector(it) }
    }

    private fun setupNoteSelector(rootView: View) {
        if (useCircleOfFifthSelector) {
            // configure scale popup
            val binding = DataBindingUtil.inflate<PopupScaleBinding>(
                layoutInflater, R.layout.popup_scale, null, false
            )
            popup = PopupWindow(
                binding.root, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            binding.popup = popup
            binding.handler = this
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                popup?.elevation = 20f
            }
            rootNoteButton?.setOnClickListener {
                if (popup?.isShowing == true)
                    popup?.dismiss()
                else {
                    // late binding of piano, because during onCreateView it is not yet known to us
                    binding.piano = piano

                    // TODO: 2019-05-27 close with back
                    rootNoteButton?.let { button ->
                        popup?.showAtLocation(button, Gravity.CENTER, 0, 0)
                    }
                }
            }
        } else {
            rootNoteButton?.setOnClickListener {
                val builder = AlertDialog.Builder(requireActivity())
                builder.setTitle(R.string.title_root_note)

                builder.setItems(noteNames) { dialog, item ->
                    piano?.setRoot(item)
                    rootNoteButton?.text = noteNames[item]
                }

                val alert = builder.create()
                alert.show()
            }
        }
    }

    fun isUseCircleOfFifthSelector(): Boolean {
        return useCircleOfFifthSelector
    }

    fun setRoot(newRoot: Int) {
        piano?.setRoot(newRoot)
        popup?.dismiss()
        piano?.getRootNote()?.let { rootNote ->
            rootNoteButton?.text = noteNames[rootNote]
        }
    }
} 