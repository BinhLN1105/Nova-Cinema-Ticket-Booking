package com.cinema.ticket_booking.ui.scanner

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * Nova Enterprise: Custom Capture Activity to force Portrait orientation.
 * By default, ZXing-Android-Embedded uses sensors which can cause landscape flipping.
 */
class PortraitCaptureActivity : CaptureActivity()
