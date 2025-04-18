package net.thomasphillips.octone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager

class PianoControlScale : Fragment() {
    private var piano: OctonePianoView? = null
    private var useCircleOfFifthSelector = false
    private var scaleSpinner: Spinner? = null
    private var rootSpinner: Spinner? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_piano_control_scale, container, false)

        // Get spinners
        scaleSpinner = view.findViewById(R.id.spinner_scale)
        rootSpinner = view.findViewById(R.id.spinner_root)

        // Setup scale spinner
        val scaleAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.scale_names,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        scaleSpinner?.adapter = scaleAdapter

        // Setup root note spinner
        val rootAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.note_names,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        rootSpinner?.adapter = rootAdapter

        // Load preferences
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        useCircleOfFifthSelector = preferences.getBoolean("circle_of_fifths", false)
        scaleSpinner?.setSelection(preferences.getInt(OctonePianoView.PREF_SCALE, OctonePianoView.PREF_SCALE_DEFAULT))
        rootSpinner?.setSelection(preferences.getInt(OctonePianoView.PREF_SCALE_ROOT, OctonePianoView.PREF_SCALE_ROOT_DEFAULT))

        // Setup listeners
        scaleSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                piano?.setScale(position, rootSpinner?.selectedItemPosition ?: 0)
                preferences.edit().putInt(OctonePianoView.PREF_SCALE, position).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        rootSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                piano?.setScale(scaleSpinner?.selectedItemPosition ?: 0, position)
                preferences.edit().putInt(OctonePianoView.PREF_SCALE_ROOT, position).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        return view
    }

    fun setPiano(piano: OctonePianoView) {
        this.piano = piano
        piano.setScale(
            scaleSpinner?.selectedItemPosition ?: OctonePianoView.PREF_SCALE_DEFAULT,
            rootSpinner?.selectedItemPosition ?: OctonePianoView.PREF_SCALE_ROOT_DEFAULT
        )
    }

    fun isUseCircleOfFifthSelector(): Boolean = useCircleOfFifthSelector

    fun toggleCircleOfFifthSelector() {
        useCircleOfFifthSelector = !useCircleOfFifthSelector
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit()
            .putBoolean("circle_of_fifths", useCircleOfFifthSelector)
            .apply()
    }

    fun setRoot(root: Int) {
        rootSpinner?.setSelection(root)
    }
} 