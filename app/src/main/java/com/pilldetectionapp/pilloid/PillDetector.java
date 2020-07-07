package com.pilldetectionapp.pilloid;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class PillDetector {
    // Attributes
    private Boolean pill_detected;
    private float[] pill_position;

    // Constructor
    public PillDetector(){
        this.pill_detected = false;
        this.pill_position = new float[4];
    }

    // Getters
    public Boolean getPill_detected(){
        return this.pill_detected;
    }

    public float[] getPill_position(){
        return this.pill_position;
    }

    // Setters
    public void setPill_detected(Boolean result){
        this.pill_detected = result;
    }

    public void setPill_position(float[] pos){
        this.pill_position = pos;
    }

    // Methods
    public Mat StartPillDetection(Mat frame){
        Mat hsv_image = new Mat();
        Mat mask = new Mat();
        Imgproc.cvtColor(frame, hsv_image, Imgproc.COLOR_BGR2HSV);

        // We create our range of white
        Scalar light_white = new Scalar(0, 0, 200);
        Scalar dark_white = new Scalar(145, 30, 255);

        // We apply the filter to our HSV image, we get a filter image
        Core.inRange(hsv_image, light_white,dark_white, mask);

        //Mat result = Core.bitwise_and(frame, frame, mask=mask);
        return mask;

    }
}
