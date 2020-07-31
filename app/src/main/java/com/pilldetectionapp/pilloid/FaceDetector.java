package com.pilldetectionapp.pilloid;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
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
import java.util.concurrent.ExecutionException;

public class FaceDetector {
    // Attributes
    private int nb_faces_detected;
    private Boolean face_detected;

    private Boolean mouth_detected;

    private Bitmap bitmap;
    // x, y , width, height
    private int[] mouth_position;
    private FirebaseVisionFaceDetectorOptions settings;

    private FirebaseVisionFaceDetector face_detector;

    /**
     * constructor, initialise the firebase detectors with wanted settings
     */
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
    public FirebaseVisionFaceDetector getFace_detector() { return face_detector; }

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


    /**
     * Detect face presence and prosition in a frame, and also the mouth on that face
     * @param frame matrix in which we want to detect a face
     */
    public void StartFaceDetection(Mat frame){
        this.bitmap = bitmapFromMat(frame);
        // Creation of the firebase image
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(this.bitmap);

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

                                            //Rect Face = faces.get(0).getBoundingBox();

                                            count = count +1;
                                            // We have detected minimum one face
                                            setFace_Detected(true);

                                            // We print in the logCat that we have detected one face
                                            Log.e("Face Detector", "Detected");

                                            // We check if the face is entirely in the picture
                                            //if (!((Face.left+Face.width())>bitmap.getWidth() || (Face.top+Face.height())>bitmap.getHeight())) {
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

    /**
     * method to get the mouth from a face and set it to a private variable
     * @param face
     */
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

    /**
     * acknowledge if there is a face in the desired frame
     * @param frame frame to be analysed
     * @return true if a face is found
     */
    public boolean findFaceInImage(Mat frame) {
        bitmap = bitmapFromMat(frame);
        List<FirebaseVisionFace> faces = null;
        try{
            faces = Tasks.await(this.face_detector.detectInImage(FirebaseVisionImage.fromBitmap(bitmap)));
            if (!faces.isEmpty()) {
                Rect face = faces.get(0).getBoundingBox();
                // If we detect a face but it's not entirely in the picture we return false
                if ((face.left+face.width())>bitmap.getWidth() || (face.top+face.height())>bitmap.getHeight()) {
                    return false;
                }
            }
        } catch (ExecutionException | InterruptedException e) {
          e.printStackTrace();
        }
        assert faces != null;
        return !faces.isEmpty();
    }

    // Method allowing to transform our frame into bitmap
    public static Bitmap bitmapFromMat(Mat mRgba) {
        Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bitmap);
        return bitmap;
    }

}
