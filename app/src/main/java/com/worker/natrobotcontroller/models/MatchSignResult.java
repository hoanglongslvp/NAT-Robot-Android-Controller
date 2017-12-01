package com.worker.natrobotcontroller.models;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

public final class MatchSignResult {
    @NotNull
    private final TrafficSign sign;
    private final double error;
    @NotNull
    private final Mat thumbnail;
    @NotNull
    private final RotatedRect rect;

    @NotNull
    public final TrafficSign getSign() {
        return this.sign;
    }

    public final double getError() {
        return this.error;
    }

    @NotNull
    public final Mat getThumbnail() {
        return this.thumbnail;
    }

    @NotNull
    public final RotatedRect getRect() {
        return this.rect;
    }

    public MatchSignResult(@NotNull TrafficSign sign, double error, @NotNull Mat thumbnail, @NotNull RotatedRect rect) {
        this.sign = sign;
        this.error = error;
        this.thumbnail = thumbnail;
        this.rect = rect;
    }
}
