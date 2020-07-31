package com.pilldetectionapp.pilloid;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pilldetectionapp.pilloid.exceptions.BoundingBoxOutOfPictureException;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Class to run face recognition
 */
public class FaceRecognitionDetector {

    private final String TAG = "FaceRecognitionDetector";
    private FirebaseVisionFaceDetector face_detector;

    private Activity activity;
    private FaceNetModel model;
    private float[] imageData;
    private Rect foundFace = null;
    private Bitmap bitmap;
    private Bitmap savedImage;
    private FirebaseUser user;
    private URI uri;

    /**
     * constructor of the face recognition detector
     * @param activity we need the activity in order to get the assets from that activity
     */
    public FaceRecognitionDetector(final Activity activity) {
        this.activity = activity;
        this.getUsersImage();
        // Creation of the setting of the detector, see the documentation if you want more details
        FirebaseVisionFaceDetectorOptions settings =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                        .build();

        // Creation of the Vision detector with the setting created
        this.face_detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(settings);

    }

    /**
     * analyse a given frame and the profile picture of a the authenticated person
     * @param frame the frame with the user's face on it
     * @return true if the two person are the same
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean analyse(Mat frame) {
        boolean recognitionSucceed = false;

        this.bitmap = bitmapFromMat(frame);

        List<FirebaseVisionFace> facesInInput = null;
        List<FirebaseVisionFace> facesInSaved;

        final FirebaseVisionImage inputImage = FirebaseVisionImage.fromBitmap(bitmap);

        try {
            facesInInput  = Tasks.await(face_detector.detectInImage(inputImage));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (facesInInput != null && !facesInInput.isEmpty()) {
            try{
                this.foundFace = facesInInput.get(0).getBoundingBox();
                Log.e(TAG, "face found on new image");
                this.model = new FaceNetModel(activity.getAssets());

                this.imageData = new float[128];

                FirebaseVisionImage reference = FirebaseVisionImage.fromBitmap(savedImage);

                facesInSaved = Tasks.await(face_detector.detectInImage(reference));

                Log.e(TAG, "face found on saved image");
                imageData = this.model.getFaceEmbedding(savedImage, facesInSaved.get(0).getBoundingBox(), 0f);
                if (FaceRecognitionDetector.this.foundFace != null) {
                    float[] subject = model.getFaceEmbedding(this.bitmap, this.foundFace, 0f);
                    double similarityScore = -1f;
                    String similarityScoreName = "";

                    if (imageData != null) {
                        similarityScore = cosineSimilarity( subject, imageData );

                        Log.e(TAG, String.valueOf(similarityScore));

                        if (similarityScore > 0.85f) {
                            Log.e(TAG, "recog success ");
                            recognitionSucceed = true;
                        }

                    }
                } else {
                    Log.e(TAG, "no face on new image");
                }
            } catch (InterruptedException | ExecutionException | BoundingBoxOutOfPictureException e){
                e.printStackTrace();
            }

        }
        return recognitionSucceed;
    }

    /**
     * Compute cosine Similarity between two embeddings
     * @param source 1st embeddings
     * @param target 2nd embeddings
     * @return the cosine similarity, double between 0 and 1
     */
    protected double cosineSimilarity(float[] source, float[] target) {
        if (source.length != target.length)
            throw new RuntimeException("Arrays must be same size");
        double dotProduct = 0;
        for (int i = 0; i < source.length; i++) {
            dotProduct += source[i] * target[i];
        }
        float normS = .0f;
        float normT = .0f;
        for(int k = 0; k < source.length; ++k) {
            normS += source[k]*source[k];
            normT += target[k]*target[k];
        }
        double euclideanDist = Math.sqrt(normS) * Math.sqrt(normT);
        return dotProduct / euclideanDist;
    }


    /**
     * Convert Mat to Bitmap
     * @param image matrix to be converted
     * @return the converted bitmap
     */
    public static Bitmap bitmapFromMat(Mat image) {
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);

        return bitmap;
    }


    /**
     * Each user has a profile picture
     * retrieve the profile picture, and assign it to a private variable
     */
    private void getUsersImage(){
        long TWO_MEGABYTE = 1024*1024*2;
        user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference gsReference = storage.getReferenceFromUrl("gs://pilloid.appspot.com/profileImages/" + user.getUid() + ".jpeg");
        gsReference.getBytes(TWO_MEGABYTE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        FaceRecognitionDetector.this.savedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "onFailure : Error : " + e.getMessage());
                }
                });

    }

}
