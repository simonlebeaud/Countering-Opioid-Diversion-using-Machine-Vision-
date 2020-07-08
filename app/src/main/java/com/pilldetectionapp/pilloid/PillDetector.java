package com.pilldetectionapp.pilloid;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
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
    public Mat StartPillDetection(Mat frame, int[] mouth) {

        Mat hsv_image = new Mat();
        Mat black_White_image = new Mat();

        // We transform the RGB image to the HSV image format
        Imgproc.cvtColor(frame, hsv_image, Imgproc.COLOR_BGR2HSV);

        // We create our range of white
        Scalar light_white = new Scalar(0, 0, 200);
        Scalar dark_white = new Scalar(145, 30, 255);

        // We apply the filter to our HSV image, we get a filter image (black and white)
        Core.inRange(hsv_image, light_white, dark_white, black_White_image);


        // We create a rectangle with the mouth position
        Rect roi = new Rect(mouth[0], mouth[1], mouth[2], mouth[3]);

        // We crop the image, we just want to analyse mouth area
        Mat reshape_frame = new Mat(black_White_image, roi);

        // We count number of white pixels into the mouth area
        int result = Core.countNonZero(reshape_frame);

        // Debugging message
        Log.e("Pill detection", String.valueOf(result));


        if ((result > 100)&&(result < 500)) {
            setPill_detected(true);
        } else setPill_detected(false);


        return black_White_image;
    }
}
