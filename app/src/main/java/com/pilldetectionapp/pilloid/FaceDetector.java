package com.pilldetectionapp.pilloid;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

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

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

public class FaceDetector {
    // Attributes
    private int nb_faces_detected;
    private Boolean face_detected;

    private Boolean mouth_detected;
    // x, y , width, height
    private int[] mouth_position;
    private FirebaseVisionFaceDetectorOptions settings;

    private FirebaseVisionFaceDetector face_detector;

    // Constructor
    public FaceDetector(){
        this.nb_faces_detected = 0;
        this.face_detected = false;
        this.mouth_position = new int[4];
        this.mouth_detected = false;

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

    public int[] getMouth_Position(){
        return this.mouth_position;
    }

    public Boolean getMouth_detected() { return this.mouth_detected; }

    //Setters
    public void setNb_Faces_Detected(int nb){
        this.nb_faces_detected = nb;
    }

    public void setFace_Detected(Boolean result){
        this.face_detected = result;
    }

    public void setMouth_Position(int[] position){
        this.mouth_position = position;
    }

    public void setMouth_detected(Boolean result){
        this.mouth_detected = result;
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

                                        }

                                        // We set the mouth position only for the first face detected
                                        if (count == 1) {
                                            Get_SetMouthPositionFromFirebaseVisionFace(faces.get(0));
                                        } else {
                                            setMouth_detected(false);
                                            setFace_Detected(false);
                                        }
                                        // We set the number of detected faces
                                        setNb_Faces_Detected(count);
                                    }
                                })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
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
        float[] pos = new float[6];
        int[] result = new int[4];
        FirebaseVisionFaceLandmark mouth_left = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT);
        FirebaseVisionFaceLandmark mouth_right = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT);
        FirebaseVisionFaceLandmark mouth_bottom = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM);

        FirebaseVisionPoint pos_mouth_right, pos_mouth_left, pos_mouth_bottom;
        pos_mouth_left = mouth_left.getPosition();
        pos_mouth_right = mouth_right.getPosition();
        pos_mouth_bottom = mouth_bottom.getPosition();


        if ((pos_mouth_right != null)&&(pos_mouth_bottom != null)&&(pos_mouth_left != null)){
            pos[0] = pos_mouth_left.getX();
            pos[1] = pos_mouth_left.getY();
            // We print a message in the LogCat (debugging)
            //Log.e("Mouth detection : left",pos_mouth_left.toString());

            pos[2] = pos_mouth_right.getX();
            pos[3] = pos_mouth_right.getY();

            // We print a message in the LogCat (debugging)
            //Log.e("Mouth detection : right",pos_mouth_right.toString());

            pos[4] = pos_mouth_bottom.getX();
            pos[5] = pos_mouth_bottom.getY();

            // We print a message in the LogCat (debugging)
            //Log.e("Mouth detection: bottom",pos_mouth_bottom.toString());
            setMouth_detected(true);

        } else setMouth_detected(false);

        Log.e("Pill ",getMouth_detected().toString());

        // X and Y position of right top mouth ( * tolerance )
        result[0] = (int) ((pos[0])*0.95);
        result[1] = (int ) (pos[1]*0.95);

        // width and height of the mouth
        result[2] = (int) ((pos[2] - pos[0])*1.05);
        result[3] = (int) ((pos[5] - pos[1])*1.05);
        this.setMouth_Position(result);




    }

}
