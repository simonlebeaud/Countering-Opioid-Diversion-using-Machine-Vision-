package com.pilldetectionapp.pilloid;

public class Detector {

    // Attributes
    private FaceDetector faceDetector;
    private TextDetector textDetector;
    private PillDetector pillDetector;

    // Constructor
    public Detector(){
        this.faceDetector = new FaceDetector();
        this.textDetector = new TextDetector();
        this.pillDetector = new PillDetector();
    }

    // Getters
    public FaceDetector getFaceDetector(){
        return this.faceDetector;
    }

    public TextDetector getTextDetector(){
        return this.getTextDetector();
    }

    public PillDetector getPillDetector(){
        return this.pillDetector;
    }

}
