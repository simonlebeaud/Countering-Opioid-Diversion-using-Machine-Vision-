package com.pilldetectionapp.pilloid;

import android.app.Activity;

import java.io.IOException;

/**
 * Class regrouping all the detectors
 */
public class Detector {

    // Attributes
    private FaceDetector faceDetector;
    private TextDetector textDetector;
    private PillDetector pillDetector;
    private HandDetector handDetector;
    private FaceRecognitionDetector faceRecognitionDetector;

    // Constants for the handdetector
    private static final int INPUT_SIZE = 256;
    private static final boolean IS_QUANTIZED = false;
    private static final String MODEL_FILENAME = "palm_detection.tflite";
    private static final String LABELS_FILENAME = "file:///android_asset/palm_detection_labelmap.txt";


    /**
     * constructor, initialise all the detectors
     * @param activity
     */
    public Detector(Activity activity) {
        this.faceDetector = new FaceDetector();
        this.textDetector = new TextDetector();
        this.pillDetector = new PillDetector();
        this.faceRecognitionDetector = new FaceRecognitionDetector(activity);
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

    public FaceRecognitionDetector getFaceRecognitionDetector() { return this.faceRecognitionDetector; }

}
