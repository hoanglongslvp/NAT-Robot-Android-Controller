package com.worker.natrobotcontroller.customView

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent


/**
 * Created by hataketsu on 11/15/17.
 */

class NoSwipeViewPager(context: Context?, attrs: AttributeSet?) : ViewPager(context, attrs) {

    override fun onInterceptTouchEvent(event: MotionEvent) = false

    override fun onTouchEvent(event: MotionEvent) = false
}
