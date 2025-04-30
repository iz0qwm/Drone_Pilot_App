package com.kwos.dronepilotapp

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class TransparentTouchView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onTouchInterceptListener: ((Boolean) -> Unit)? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                onTouchInterceptListener?.invoke(true) // Blocca lo scroll
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                onTouchInterceptListener?.invoke(false) // Sblocca lo scroll
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
