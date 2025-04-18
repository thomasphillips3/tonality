package com.octone.app

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.ImageButton

class PopupSizingMenu(context: Context) {
    private val view = LayoutInflater.from(context).inflate(R.layout.popup_sizing, null)
    private val popupWindow = PopupWindow(
        view,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        true
    )

    fun show(anchor: View) {
        popupWindow.showAsDropDown(anchor, 0, 0, Gravity.BOTTOM or Gravity.END)
    }

    fun setOnRowsMinusClickListener(listener: View.OnClickListener) {
        view.findViewById<ImageButton>(R.id.button_rows_minus).setOnClickListener(listener)
    }

    fun setOnRowsPlusClickListener(listener: View.OnClickListener) {
        view.findViewById<ImageButton>(R.id.button_rows_plus).setOnClickListener(listener)
    }

    fun setOnKeysMinusClickListener(listener: View.OnClickListener) {
        view.findViewById<ImageButton>(R.id.button_keys_minus).setOnClickListener(listener)
    }

    fun setOnKeysPlusClickListener(listener: View.OnClickListener) {
        view.findViewById<ImageButton>(R.id.button_keys_plus).setOnClickListener(listener)
    }

    fun setOnResetClickListener(listener: View.OnClickListener) {
        view.findViewById<ImageButton>(R.id.button_reset).setOnClickListener(listener)
    }
} 