package com.pilldetectionapp.pilloid;

import android.app.Activity;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;

import static com.firebase.ui.auth.AuthUI.TAG;

public class Detector {

    // Attributes
    private FaceDetector faceDetector;
    private TextDetector textDetector;
    private PillDetector pillDetector;
    private HandDetector handDetector;

    // Constants for the handdetector
    private static final int INPUT_SIZE = 256;
    private static final boolean IS_QUANTIZED = false;
    private static final String MODEL_FILENAME = "palm_detection.tflite";
    private static final String LABELS_FILENAME = "file:///android_asset/palm_detection_labelmap.txt";



    // Constructor
    public Detector(Activity activity) {
        this.faceDetector = new FaceDetector();
        this.textDetector = new TextDetector();
        this.pillDetector = new PillDetector();
        try {
            this.handDetector = new HandDetector(activity,
                    activity.getAssets(),
                    MODEL_FILENAME,
                    LABELS_FILENAME,
                    INPUT_SIZE,
                    IS_QUANTIZED);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Getters
    public FaceDetector getFaceDetector(){
        return this.faceDetector;
    }

    public TextDetector getTextDetector(){
        return this.textDetector;
    }

    public PillDetector getPillDetector(){
        return this.pillDetector;
    }

    public HandDetector getHandDetector(){
        return this.handDetector;
    }

}
