package com.pilldetectionapp.pilloid;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
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
    int recogCount= 0;

    private Mat frame;
    private Mat[] frame_for_text_detection;

    private ArrayList<Mat> frame_text_detection;

    private Boolean mouth_detected,pill_detected,face_detected, hands_detected;
    private Boolean step_one_finished, step_two_finished, step_three_finished,
            step_four_finished, detection_finished;
    private Boolean counter_can_begin, pill_removed, good_finished;
    private Boolean start_button_finished;

    private boolean textDetection_finished;
    private boolean rightTextDetected;
    private int shown_time, tolerance, no_face_detected;
    private Toast toast;

    private TextView message_view,counter_view;

    private String pill_text;

    private Boolean rightPerson = false;


    Detector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_land);

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

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        System.gc();
        frame = inputFrame.rgba();

        if ((counter % 30 == 0)&&(start_button_finished)) {
            if( !rightPerson || recogCount < 3  ) {
                this.checkPersonsFaceIdentity(frame);
                recogCount += 1;
            } else {
                Log.e("Frame took", "frame picked");

                // We process face detection on the frame
                this.detector.getFaceDetector().StartFaceDetection(frame);
                face_detected = this.detector.getFaceDetector().getFace_Detected();
                // Debugging message
                Log.e("Face detected ", face_detected.toString());

                if (face_detected) {
                    //  Debugging message
                    Log.e("Mouth detected ", this.detector.getFaceDetector().getMouth_detected().toString());
                    // We process hand detection on the frame
                    this.detector.getHandDetector().StartHandDetection(frame);
                    //  Debugging message
                    Log.e("Hand detected", this.detector.getHandDetector().getHand_detected().toString());

                    if (this.detector.getFaceDetector().getMouth_detected()) {

                        // If we detect a mouth we try to detect a pill on it
                        this.detector.getPillDetector().StartPillDetection(frame, this.detector.getFaceDetector().getMouth_Position());

                    } else this.detector.getPillDetector().setPill_detected(false);

                    process_steps();
                } else {
                    no_face_detected++;
                    ShowToast("No Face Detected");
                }
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
        message_view = (TextView) findViewById(R.id.MessageView);
        counter_view = (TextView) findViewById(R.id.CounterView);

        this.detector = new Detector(this);
        this.frame_text_detection = new ArrayList<>();

        this.pill_detected = false;
        this.mouth_detected = false;
        this.face_detected = false;
        this.hands_detected = false;

        this.step_one_finished = false;
        this.step_two_finished = false;
        this.step_three_finished = false;
        this.step_four_finished = false;
        this.detection_finished = false;
        this.textDetection_finished = false;

        this.counter_can_begin = false;
        this.pill_removed = false;
        this.good_finished = false;
        this.rightTextDetected = false;
        this.tolerance = 0;
        this.shown_time = 0;
        this.no_face_detected = 0;

        this.start_button_finished = false;

        Intent intent = getIntent();
        this.pill_text = intent.getStringExtra(GetTextActivity.EXTRA_TEXT);

        this.frame_for_text_detection = new Mat[5];

    }

    private void process_steps(){
        pill_detected = this.detector.getPillDetector().getPill_detected();
        mouth_detected = this.detector.getFaceDetector().getMouth_detected();
        hands_detected = this.detector.getHandDetector().getHand_detected();



        if (!detection_finished) {
            if (!step_one_finished) {
                // put the text : " Please put the pill in front..."
                message_view.setText("Please, put the pill in front of your mouth, with the text clearly visible  " );
                counter_view.setText(String.valueOf(4 - shown_time));
                if (pill_detected && hands_detected) {
                    this.frame_text_detection.add(this.frame);
                    frame_for_text_detection[shown_time] = frame;
                    shown_time += 1;
                }

                if (shown_time > 4) {
                    step_one_finished = true;
                    shown_time = 0;
                }
            } else if (!step_two_finished) {

                // Put the text : " Please put the pill on your tongue,..."
                message_view.setText("Please put the pill on your tongue, and then remove your hands " );
                counter_view.setText(String.valueOf(7 - shown_time));


                if ((pill_detected) && (!hands_detected)) {
                    shown_time += 1;
                    // Debugging Text
                    Log.e("Step Two", String.valueOf(shown_time));
                    if (shown_time > 7) {
                        step_two_finished = true;
                        shown_time = 0;
                    }
                }
                if (hands_detected) ShowToast("Hand detected !");


            } else if (!step_three_finished) {
                // Put the text : " Please keep the pill on your tongue for 10 seconds..."
                message_view.setText("Please keep the pill on your tongue 10 seconds, with your mouth close." );
                counter_view.setText(String.valueOf(10 - shown_time));
                if ((!pill_detected) && (!hands_detected) && (!counter_can_begin)) {
                    counter_can_begin = true;
                    // We reset the counter
                    shown_time = 0;
                }

                if (hands_detected) ShowToast("Hand detected !");
                if (pill_detected) ShowToast("Pill detected !");

                Log.e("Counter can begin", counter_can_begin.toString());

                if (counter_can_begin) {
                    // If there's hand in the frame during the ten seconds countdown,
                    // we assume the patient took the pill out of the mouth
                    if ((hands_detected) || (pill_detected)) {
                        // The countdown restarts
                        counter_can_begin = false;

                        // We assume that the pill could have been removed
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
                message_view.setText("Please open your and show the pill still on your tongue " );
                counter_view.setText(String.valueOf(3 - shown_time));
                if ((pill_detected) && (!hands_detected)) {
                    shown_time += 1;
                    if (shown_time > 3) {
                        step_four_finished = true;
                        good_finished = true;
                        detection_finished = true;
                    }
                }

                // if we can't detect pill in the mouth,
                // it may because the patient is opening his/her mouth and the pill is blocked
                if (!pill_detected) {
                    tolerance += 1;
                    ShowToast("Please show the pill ");
                }

                // if hands show up before the pill is detected,
                // we assume the patient took out the pill
                if (hands_detected) {
                    // the patient didn't follow all the instructions
                    //detection is finished
                    step_four_finished = true;
                    detection_finished = true;
                    good_finished = false;
                    ShowToast("Hand detected !");
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
            message_view.setText("Thank you, you accomplished all the steps.");

            //debugging text
            Log.e("Tolerance",String.valueOf(tolerance));

            if (!textDetection_finished) {
                // We now process the Text detection
                this.detector.getTextDetector().StartTextDetection(frame_for_text_detection, this.pill_text);
                this.textDetection_finished = true;
            } else {
                // We change the result to true if it's the right text (doesn't work for the moment)
                if (this.detector.getTextDetector().getTextDetectionResult()) this.rightTextDetected = true;
                // We launch the result activity with all our results
                Intent intent = new Intent(CameraActivity.this, ResultActivity.class);
                intent.putExtra("StepOneResult",step_one_finished);
                intent.putExtra("StepTwoResult",step_two_finished);
                intent.putExtra("StepThreeResult",!pill_removed);
                intent.putExtra("StepFourResult",good_finished);
                intent.putExtra("TextDetectionResult",rightTextDetected);
                intent.putExtra("FaceVerificationResult",rightPerson);
                // We launch the result activity
                startActivity(intent);
            }

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkPersonsFaceIdentity(Mat frame) {
        rightPerson = false;
        if(!rightPerson) {
            this.rightPerson = this.detector.getFaceRecognitionDetector().analyse(frame);

            if (this.rightPerson) ShowToast("Right Person !");
            else ShowToast("This is not the right person");
        }
    }

    public void ShowToast(final String text){
        runOnUiThread(new Runnable() {
            public void run()
            {
                int toastDurationInMilliSeconds = 500;
                CountDownTimer toastCountDown;
                toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
                // Set the countdown to display the toast

                toastCountDown = new CountDownTimer(toastDurationInMilliSeconds, 1000 /*Tick duration*/) {
                    public void onTick(long millisUntilFinished) {
                        toast.show();
                    }
                    public void onFinish() {
                        toast.cancel();
                    }
                };
                toast.show();
                toastCountDown.start();

            }
        });
    }

    public void Start(View view) {
        this.start_button_finished = true;
        Button start_button;
        start_button = (Button) findViewById(R.id.StartButton);
        start_button.setVisibility(view.GONE);
    }
}