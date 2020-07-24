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

    private String[] text = new String[5];
    private Boolean textDetectionResult;
    private int pos;

    // Constructor
    public TextDetector(){
        for(int i =0;i<5;i++){
            this.text[i] = "";
        }
        this.textDetectionResult = false;
    }

    // Getters
    public String getText(int i){
        return this.text[i];
    }

    public Boolean getTextDetectionResult(){ return this.textDetectionResult;}

    // Setters
    public void setText(int i, String t){this.text[i] = t;}

    public void setTextDetectionResult(Boolean result){this.textDetectionResult = result;}

    public void StartTextDetection(Mat[] frame, final String pillText) {
        // We have only 5 frame with text
        for (int i = 0; i<5; i++) {
            this.pos = i;
            // Creation of the firebase image
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmapFromMat(frame[i]));

            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                    .getOnDeviceTextRecognizer();

            Task<FirebaseVisionText> result =
                    detector.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                    // Task completed successfully
                                    setText(pos,firebaseVisionText.getText());

                                    // if that's the good text we put the result to True
                                    if (getText(pos).equals(pillText)) setTextDetectionResult(true);

                                    // We print a message in the LogCat (debugging)
                                    Log.e("Text detection ", getText(pos));
                                    Log.e("Text detection Result Text", getTextDetectionResult().toString());

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

    }

    // Method allowing to transform our frame into bitmap
    public static Bitmap bitmapFromMat(Mat mRgba) {
        Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bitmap);

        return bitmap;
    }
}
