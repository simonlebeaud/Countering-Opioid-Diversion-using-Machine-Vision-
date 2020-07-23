package com.pilldetectionapp.pilloid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class ResultActivity extends AppCompatActivity {
    private Boolean step_one_finished, step_two_finished,step_three_finished, step_four_finished,rightTextDetected;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        initialise_variable();

    }

    private void initialise_variable(){
        Intent intent = getIntent();
        this.step_one_finished = intent.getBooleanExtra("StepOneResult",false);
        this.step_two_finished = intent.getBooleanExtra("StepTwoResult",false);
        this.step_three_finished = intent.getBooleanExtra("StepThreeResult",false);
        this.step_four_finished = intent.getBooleanExtra("StepFourResult",false);

    }
}