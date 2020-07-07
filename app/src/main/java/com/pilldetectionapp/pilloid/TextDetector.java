package com.pilldetectionapp.pilloid;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class TextDetector {
    // Attributes

    private String text;

    // Constructor
    public TextDetector(){
        this.text = "";
    }

    // Getters
    public String getText(){
        return this.text;
    }

    // Setters
    public void setText(String t){
        this.text = t;
    }


    public void StartTextDetection(Mat frame) {
        // Creation of the firebase image
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmapFromMat(frame));

        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();

        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                // Task completed successfully
                                setText(firebaseVisionText.getText());
                                // We print a message in the LogCat (debugging)
                                Log.e("Text detection ", getText());

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
}
