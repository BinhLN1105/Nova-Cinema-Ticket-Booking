package com.cinema.ticket_booking.util

import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import com.cinema.ticket_booking.R
import com.google.android.material.snackbar.Snackbar

object SnackbarHelper {

    @JvmStatic
    fun showSuccess(root: View, message: String) {
        val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(Color.parseColor("#333333"))
        snackbar.setTextColor(Color.WHITE)
        adjustAnchorView(root, snackbar)
        snackbar.show()
    }

    @JvmStatic
    fun showInfo(root: View, message: String) {
        val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(Color.parseColor("#333333"))
        snackbar.setTextColor(Color.WHITE)
        adjustAnchorView(root, snackbar)
        snackbar.show()
    }

    @JvmStatic
    fun showError(root: View, message: String) {
        val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG)
        snackbar.setBackgroundTint(ContextCompat.getColor(root.context, R.color.nova_red))
        snackbar.setTextColor(Color.WHITE)
        adjustAnchorView(root, snackbar)
        snackbar.show()
    }

    @JvmStatic
    fun showRaw(root: View, message: String) {
        showInfo(root, message)
    }

    @JvmStatic
    fun showWithAction(root: View, message: String, actionText: String, listener: View.OnClickListener) {
        val snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG)
        snackbar.setAction(actionText, listener)
        snackbar.setActionTextColor(Color.parseColor("#FFD700"))
        snackbar.setBackgroundTint(Color.parseColor("#333333"))
        snackbar.setTextColor(Color.WHITE)
        adjustAnchorView(root, snackbar)
        snackbar.show()
    }

    private fun adjustAnchorView(root: View, snackbar: Snackbar) {
        val scannerFab = root.rootView.findViewById<View>(R.id.fabScanner)
        if (scannerFab != null && scannerFab.visibility == View.VISIBLE) {
            snackbar.anchorView = scannerFab
            return
        }

        val bottomNav = root.rootView.findViewById<View>(R.id.bottomNav)
        if (bottomNav != null && bottomNav.visibility == View.VISIBLE) {
            snackbar.anchorView = bottomNav
        }
    }
}
