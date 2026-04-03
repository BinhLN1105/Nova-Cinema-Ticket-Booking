package com.cinema.ticket_booking.util;

import android.graphics.Color;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.cinema.ticket_booking.R;
import com.google.android.material.snackbar.Snackbar;

/**
 * SnackbarHelper handles professional notifications with Nova branding.
 * It automatically adjusts its position to avoid overlapping with the Bottom Navigation bar.
 */
public class SnackbarHelper {

    /**
     * Shows a success message with a dark, professional theme.
     */
    public static void showSuccess(View root, String message) {
        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(Color.parseColor("#333333")); // Dark grey minimalist
        snackbar.setTextColor(Color.WHITE);
        adjustAnchorView(root, snackbar);
        snackbar.show();
    }

    /**
     * Shows an informational message with a modern, professional theme.
     */
    public static void showInfo(View root, String message) {
        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(Color.parseColor("#333333")); // Same as success for info
        snackbar.setTextColor(Color.WHITE);
        adjustAnchorView(root, snackbar);
        snackbar.show();
    }

    /**
     * Shows an error message with Nova's signature red branding.
     */
    public static void showError(View root, String message) {
        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(root.getContext(), R.color.nova_red));
        snackbar.setTextColor(Color.WHITE);
        adjustAnchorView(root, snackbar);
        snackbar.show();
    }

    /**
     * Shows a notification with an optional action button (e.g., Undo).
     */
    public static void showWithAction(View root, String message, String actionText, View.OnClickListener listener) {
        Snackbar snackbar = Snackbar.make(root, message, Snackbar.LENGTH_LONG);
        snackbar.setAction(actionText, listener);
        snackbar.setActionTextColor(Color.parseColor("#FFD700")); // Nova gold for actions
        snackbar.setBackgroundTint(Color.parseColor("#333333"));
        snackbar.setTextColor(Color.WHITE);
        adjustAnchorView(root, snackbar);
        snackbar.show();
    }

    /**
     * Helper to prevent Snackbar from overlapping with the BottomNavigationView.
     */
    private static void adjustAnchorView(View root, Snackbar snackbar) {
        // Nova Optimization: prioritize anchoring to the Scanner FAB to avoid overlap
        View scannerFab = root.getRootView().findViewById(R.id.fabScanner);
        if (scannerFab != null && scannerFab.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(scannerFab);
            return;
        }

        // Fallback to BottomNavigationView
        View bottomNav = root.getRootView().findViewById(R.id.bottomNav);
        if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(bottomNav);
        }
    }
}
