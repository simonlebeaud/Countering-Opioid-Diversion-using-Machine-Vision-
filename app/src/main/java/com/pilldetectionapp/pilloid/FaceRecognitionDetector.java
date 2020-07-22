package com.pilldetectionapp.pilloid;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FaceRecognitionDetector {

    private final String TAG = "FaceRecognitionDetector";
    private FirebaseVisionFaceDetector face_detector;

    private Activity activity;
    private FaceNetModel model;
    private Map<String,float[]> imageData;
    private Rect foundFace = null;
    private boolean recogSucess = false;
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
    public boolean analyse(Mat frame){
        this.bitmap = bitmapFromMat(frame);

        FirebaseVisionImage inputImage = FirebaseVisionImage.fromBitmap(bitmap);

        face_detector.detectInImage(inputImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
            @Override
            public void onSuccess(List<FirebaseVisionFace> faces) {
                    FaceRecognitionDetector.this.foundFace = faces.get(0).getBoundingBox();
            }
        });

        this.model = new FaceNetModel(activity.getAssets());

        this.imageData = new HashMap<String, float[]>();

        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED){

            FirebaseVisionImage reference = FirebaseVisionImage.fromBitmap(bitmap);
            OnSuccessListener successListener = new OnSuccessListener<List<FirebaseVisionFace>>() {
                @Override
                public void onSuccess(List<FirebaseVisionFace> faces) {
                    if ( !faces.isEmpty() ) {
                        imageData.put("person", FaceRecognitionDetector.this.model.getFaceEmbedding( savedImage , faces.get(0).getBoundingBox() , true ));
                        if(FaceRecognitionDetector.this.foundFace != null) {
                            float[] subject = model.getFaceEmbedding(FaceRecognitionDetector.this.bitmap, FaceRecognitionDetector.this.foundFace, true);
                            double highestSimilarityScore = -1f;
                            String highestSimilarityScoreName = "";
                            float[] person = imageData.get("person");
                            if(person !=null){
                                double p = cosineSimilarity(subject, person);
                                if ( p > highestSimilarityScore ) {
                                    highestSimilarityScore = p;
                                    Log.e(TAG, String.valueOf(highestSimilarityScore));
                                }
                                if (highestSimilarityScore>0.7f) {
                                    FaceRecognitionDetector.this.recogSucess = true;
                                    Log.e(TAG, "recog success ");
                                }
                            }
                        }
                    }
                }
            };

            FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                    .setWidth(savedImage.getWidth())
                    .setHeight(savedImage.getHeight())
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .setRotation(FirebaseVisionImageMetadata.ROTATION_90)
                    .build();


            face_detector.detectInImage(reference).addOnSuccessListener(successListener);
        }
    return this.recogSucess;
    }

    protected double cosineSimilarity(float[] source, float[] target) {
        if (source.length != target.length)
            throw new RuntimeException("Arrays must be same size");
        double dotProduct = 0;
        for (int i = 0; i < source.length; i++)
            dotProduct += source[i] * target[i];
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
        long ONE_MEGABYTE = 1024*1024;
        user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        Uri uri = user.getPhotoUrl();
        StorageReference gsReference = storage.getReferenceFromUrl("gs://pilloid.appspot.com/profileImages/" + user.getUid() + ".jpeg");
        gsReference.getBytes(ONE_MEGABYTE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        savedImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(activity, "Couldn't download Image", Toast.LENGTH_SHORT);
                    Log.e(TAG, "onFailure : Error : " + e.getMessage());
                }
                });

    }

}
