package com.worker.natrobotcontroller.customView;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NoSwipeViewPager extends ViewPager {

    public NoSwipeViewPager(@Nullable Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onTouchEvent(@NotNull MotionEvent event) {
        return false;
    }

}
