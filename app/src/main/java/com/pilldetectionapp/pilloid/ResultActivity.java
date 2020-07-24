package com.pilldetectionapp.pilloid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {
    private Boolean step_one_finished, step_two_finished,step_three_finished, step_four_finished,rightTextDetected;
    private Boolean rightFaceDetected;
    private TextView stepOne, stepTwo, stepThree, stepFour, textDetection, faceDetection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        initialise_variable();
        setTextInDifferentTextView();


    }

    private void initialise_variable(){
        Intent intent = getIntent();
        // We get all the results of the detection
        this.step_one_finished = intent.getBooleanExtra("StepOneResult",false);
        this.step_two_finished = intent.getBooleanExtra("StepTwoResult",false);
        this.step_three_finished = intent.getBooleanExtra("StepThreeResult",false);
        this.step_four_finished = intent.getBooleanExtra("StepFourResult",false);
        this.rightTextDetected = intent.getBooleanExtra("TextDetectionResult",false);
        this.rightFaceDetected = intent.getBooleanExtra("FaceVerificationResult",false);

    }

    private void setTextInDifferentTextView(){
        // We put Fail or Succes in all the text text view

        this.stepOne = (TextView) findViewById(R.id.step_One_result);
        setText(stepOne,step_one_finished);

        this.stepTwo = (TextView) findViewById(R.id.step_Two_result);
        setText(stepTwo,step_two_finished);

        this.stepThree = (TextView) findViewById(R.id.step_Three_result);
        setText(stepThree,step_three_finished);

        this.stepFour = (TextView) findViewById((R.id.step_Four_result));
        setText(stepFour,step_four_finished);

        this.textDetection = (TextView) findViewById(R.id.TextDetectionResult);
        setText(textDetection,rightTextDetected);

        this.faceDetection = (TextView) findViewById(R.id.TextDetectionResult);
        setText(faceDetection,rightFaceDetected);
    }

    private void setText(TextView myview, Boolean result){
        if (result) {
            myview.setTextColor(Color.GREEN);
            myview.setText("Success");
        } else {
            myview.setTextColor(Color.RED);
            myview.setText("Fail");
        }
    }
}