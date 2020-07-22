package com.pilldetectionapp.pilloid;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.util.ArrayList;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    int counter = 0;
    private Mat frame, frameT;

    private ArrayList<Mat> frame_text_detection;

    private Boolean mouth_detected,pill_detected,face_detected, hands_detected;
    private Boolean step_one_finished, step_two_finished, step_three_finished,
            step_four_finished, detection_finished;
    private Boolean counter_can_begin, pill_removed, good_finished;
    private Boolean recognitionInProgress =false;
    private Boolean recognitionFinished = false;
    private int shown_time, tolerance;

    private TextView message_view;

    private String pill_text;

    private Boolean rightPerson;


    Detector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_land);

        message_view = (TextView) findViewById(R.id.MessageView);


        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        cameraBridgeViewBase.setCameraIndex(1);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        initialize_variable();


        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch(status){

                    case BaseLoaderCallback.SUCCESS:
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }
            }
        };

        CameraManager manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics("1");
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            Log.e("CAMERA ACTIVITY", sensorOrientation + "esgfdddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        System.gc();
        frame = inputFrame.rgba();


        if (counter % 30 == 0) {
            
            Log.e("Frame took", "frame picked");

            // We process face detection on the frame
            this.detector.getFaceDetector().StartFaceDetection(frame);
            Log.e("Mouth detected ",this.detector.getFaceDetector().getMouth_detected().toString());

            // We process hand detection on the frame
            this.detector.getHandDetector().StartHandDetection(frame);
            Log.e("Hand detected", this.detector.getHandDetector().getHand_detected().toString());

            if (this.detector.getFaceDetector().getMouth_detected()) {
                this.checkPersonsFaceIdentity(frame);
                // If we detect a mouth we try to detect a pill on it
                this.detector.getPillDetector().StartPillDetection(frame, this.detector.getFaceDetector().getMouth_Position());

                // Test the Text detection
                //if (this.detector.getPillDetector().getPill_detected()) this.detector.getTextDetector().StartTextDetection(frame);

            } else this.detector.getPillDetector().setPill_detected(false);

            if (recognitionFinished){
                process_steps();
            }

        }
        counter +=1;
        return frame;
    }


    @Override
    public void onCameraViewStarted(int width, int height) {

    }


    @Override
    public void onCameraViewStopped() {

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()){
            Toast.makeText(getApplicationContext(),"There's a problem, yo!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }

    private void initialize_variable(){
        detector = new Detector(this);
        frame_text_detection = new ArrayList<>();

        pill_detected = false;
        mouth_detected = false;
        face_detected = false;
        hands_detected = false;

        step_one_finished = false;
        step_two_finished = false;
        step_three_finished = false;
        step_four_finished = false;
        detection_finished = false;

        counter_can_begin = false;
        pill_removed = false;
        good_finished = false;
        tolerance = 0;
        shown_time = 0;

        Intent intent = getIntent();
        pill_text = intent.getStringExtra(GetTextActivity.EXTRA_TEXT);

        // Just to verify if it works
        Log.e("text",pill_text);
    }

    private void process_steps(){
        pill_detected = this.detector.getPillDetector().getPill_detected();
        mouth_detected = this.detector.getFaceDetector().getMouth_detected();
        face_detected = this.detector.getFaceDetector().getFace_Detected();
        hands_detected = this.detector.getHandDetector().getHand_detected();



        if (!detection_finished) {
            if (!step_one_finished) {
                // put the text : " Please put the pill in front..."
                message_view.setText("Please, put the pill in front of your mouth, with the text clearly visible  " + String.valueOf(4 - shown_time));
                if (pill_detected && hands_detected) {
                    this.frame_text_detection.add(this.frame);
                    shown_time += 1;

                    // Debugging text
                    Log.e("Step one ", String.valueOf(shown_time));
                }
                if (shown_time > 4) {
                    step_one_finished = true;
                    shown_time = 0;
                }


            } else if (!step_two_finished) {

                // Put the text : " Please put the pill on your tongue,..."
                message_view.setText("Please put the pill on your tongue, and then remove your hands " + String.valueOf(7 - shown_time));


                if ((pill_detected) && (!hands_detected)) {
                    shown_time += 1;
                    // Debugging Text
                    Log.e("Step Two", String.valueOf(shown_time));
                    if (shown_time > 7) {
                        step_two_finished = true;
                        shown_time = 0;
                    }
                }

            } else if (!step_three_finished) {
                // Put the text : " Please keep the pill on your tongue for 10 seconds..."
                message_view.setText("Please keep the pill on your tongue 10 seconds, with your mouth close." + String.valueOf(10 - shown_time));
                if ((!pill_detected) && (!hands_detected) && (!counter_can_begin)) {
                    counter_can_begin = true;
                    // We reset the counter
                    shown_time = 0;
                }

                Log.e("Counter can begin", counter_can_begin.toString());

                if (counter_can_begin) {
                    // If there's hand in the frame during the ten seconds countdown,
                    // we assume the patient took the pill out of the mouth
                    if ((hands_detected) || (pill_detected)) {
                        counter_can_begin = false;
                        pill_removed = true;

                    } else {
                        shown_time += 1;
                        if (shown_time > 10) {
                            step_three_finished = true;
                            shown_time = 0;
                        }
                    }
                }


            } else if (!step_four_finished) {
                // Put the text : " Please open your mouth and show..."
                message_view.setText("Please open your and show the pill still on your tongue " + String.valueOf(3 - shown_time));
                if ((pill_detected) && (!hands_detected)) {
                    shown_time += 1;
                    if (shown_time > 3) {
                        step_four_finished = true;
                        detection_finished = true;
                    }
                }

                // if we can't detect pill in the mouth,
                // it may because the patient is opening his/her mouth and the pill is blocked
                if (!pill_detected) tolerance += 1;

                // if hands show up before the pill is detected,
                // we assume the patient took out the pill
                if (hands_detected) {
                    // the patient didn't follow all the instructions
                    //detection is finished
                    step_four_finished = true;
                    detection_finished = true;
                    good_finished = false;
                }

                if (tolerance > 5) {
                    // the patient didn't follow all the instructions
                    // detection is finished
                    step_four_finished = true;
                    detection_finished = true;
                    good_finished = false;

                }
            }

        } else {
            if (tolerance <= 5) {
                message_view.setText("Thank you, you accomplished all the steps.");
            } else message_view.setText("You didn't respect the rules. ");

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkPersonsFaceIdentity(Mat frame) {
        if(!recognitionInProgress && !recognitionFinished) {
            recognitionInProgress = true;
            this.rightPerson = this.detector.getFaceRecognitionDetector().analyse(frame);
            //recognitionInProgress = false;
            if(rightPerson) {
                this.recognitionFinished = true;
            }else {
                runOnUiThread(new Runnable() {
                    public void run()
                    {
                        Toast.makeText(getApplicationContext(), "That's not the right person", Toast.LENGTH_LONG).show();
                    }
                });
                this.recognitionFinished = true;
            }

        }
    }

}