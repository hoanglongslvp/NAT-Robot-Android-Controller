package com.worker.natrobotcontroller.models

import org.opencv.core.Mat
import org.opencv.core.RotatedRect

/**
 * Created by hataketsu on 11/19/17.
 */
class MatchSignResult(val sign: TrafficSign, val error: Double, val thumbnail: Mat, val rect: RotatedRect);