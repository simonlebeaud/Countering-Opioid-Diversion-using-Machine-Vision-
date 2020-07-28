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

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean analyse(Mat frame) {
        boolean recogSucess = false;

        this.bitmap = bitmapFromMat(frame);

        List<FirebaseVisionFace> facesInInput = null;
        List<FirebaseVisionFace> facesInSaved = null;

        final FirebaseVisionImage inputImage = FirebaseVisionImage.fromBitmap(bitmap);

        try {
            facesInInput  = Tasks.await(face_detector.detectInImage(inputImage));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (facesInInput != null && !facesInInput.isEmpty()) {
            this.foundFace = facesInInput.get(0).getBoundingBox();
            Log.e(TAG, "face found on new image");
            this.model = new FaceNetModel(activity.getAssets());

            this.imageData = new float[128];

            FirebaseVisionImage reference = FirebaseVisionImage.fromBitmap(savedImage);

            try {
                facesInSaved = Tasks.await(face_detector.detectInImage(reference));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if (!facesInSaved.isEmpty()) {
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
                            recogSucess = true;
                        }

                    }
                }
            } else {
                Log.e(TAG, "no face on new image");
            }
        }
        return recogSucess;
    }

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



    public static Bitmap bitmapFromMat(Mat mRgba) {
        Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bitmap);

        return bitmap;
    }


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
