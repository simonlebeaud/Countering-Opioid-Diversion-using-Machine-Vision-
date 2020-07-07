package com.pilldetectionapp.pilloid;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.List;

public class FaceDetector {
    // Attributes
    private int nb_faces_detected;
    private Boolean face_detected;
    private float[] mouth_position;

    private FirebaseVisionFaceDetectorOptions settings;

    private FirebaseVisionFaceDetector face_detector;

    // Constructor
    public FaceDetector(){
        this.nb_faces_detected = 0;
        this.face_detected = false;
        this.mouth_position = new float[4];

        // Creation of the setting of the detector, see the documentation if you want more details
        this.settings =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .build();

        // Creation of the Vision detector with the setting created
        this.face_detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(this.settings);
    }

    // Getters
    public int getNb_Faces_Detected(){
        return this.nb_faces_detected;
    }

    public Boolean getFace_Detected(){
        return this.face_detected;
    }

    public float[] getMouth_Position(){
        return this.mouth_position;
    }

    //Setters
    public void setNb_Faces_Detected(int nb){
        this.nb_faces_detected = nb;
    }

    public void setFace_Detected(Boolean result){
        this.face_detected = result;
    }

    public void setMouth_Position(float[] position){
        this.mouth_position = position;
    }


    // Methods

    public void StartFaceDetection(Mat frame){

        // Creation of the firebase image
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmapFromMat(frame));

        // Start the detection
        Task<List<FirebaseVisionFace>> result =
                this.face_detector.detectInImage(image)
                        // Creation of a "success listener"
                        .addOnSuccessListener(
                                new OnSuccessListener<List<FirebaseVisionFace>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionFace> faces) {
                                        // Task completed successfully

                                        // face number counter
                                        int count = 0;

                                        //
                                        for (FirebaseVisionFace face : faces) {
                                            count = count +1;

                                            // We have detected minimum one face
                                            setFace_Detected(true);

                                            // We print in the logCat that we have detected one face
                                            Log.e("Face Detector", "Detected");

                                            // We set the mouth position only for the first face detected
                                            if (count == 1) Get_SetMouthPositionFromFirebaseVisionFace(face);
                                        }

                                        // We set the number of detected faces
                                        setNb_Faces_Detected(count);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                    }
                                });
    }


    // Method allowing to transform our frame into bitmap
    public static Bitmap bitmapFromMat(Mat mRgba) {
        Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bitmap);
        return bitmap;
    }

    public void Get_SetMouthPositionFromFirebaseVisionFace (FirebaseVisionFace face){
        float[] pos = new float[4];
        FirebaseVisionFaceLandmark mouth_left = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT);
        FirebaseVisionFaceLandmark mouth_right = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT);
        FirebaseVisionPoint pos_mouth_right, pos_mouth_left;
        pos_mouth_left = mouth_left.getPosition();
        pos_mouth_right = mouth_right.getPosition();

        if (pos_mouth_left != null) {
            pos[0] = pos_mouth_left.getX();
            pos[1] = pos_mouth_left.getY();

            // We print a message in the LogCat (debugging)
            Log.e("Mouth detection : left",pos_mouth_left.toString());
        }

        if (pos_mouth_right != null){
            pos[2] = pos_mouth_right.getX();
            pos[3] = pos_mouth_right.getY();

            // We print a message in the LogCat (debugging)
            Log.e("Mouth detection : right",pos_mouth_right.toString());
        }

        this.setMouth_Position(pos);




    }

}
