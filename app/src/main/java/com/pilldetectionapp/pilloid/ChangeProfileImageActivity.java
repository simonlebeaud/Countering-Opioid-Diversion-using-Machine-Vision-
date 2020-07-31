package com.pilldetectionapp.pilloid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;

/**
 * Activity to change the user's profile picture
 */
public class ChangeProfileImageActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    FaceDetector faceDetector;

    Mat frame;
    Bitmap bitmap;
    Toast toast;
    Boolean takePhoto, face_detected;

    /**
     * Class constructor, initialise the camera
     * @param savedInstanceState Data from the previous activity ChoicePageActivity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_profile_image);

        this.face_detected = false;
        this.takePhoto = false;

        cameraBridgeViewBase = (JavaCameraView)findViewById(R.id.CameraView);
        // 1 correspond to frontal Camera
        cameraBridgeViewBase.setCameraIndex(1);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

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

        faceDetector = new FaceDetector();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    /**
     * Method to analyse each frame from the camera
     * here we check if we find a face on the frame if button is pressed and then we upload it
     * @param inputFrame frame given by the Camera view listener from OpenCv
     * @return the current frame available
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        System.gc();
        this.frame = inputFrame.rgba();

        if (takePhoto) {
            takePhoto = false;

            face_detected = this.faceDetector.findFaceInImage(this.frame);
            if(face_detected){
                this.bitmap = bitmapFromMat(this.frame);
                // We upload the image on the firebase storage
                uploadImage(this.bitmap);
            }
            else {
                ShowToast("We couldn't find you on that picture, please position yourself better");
            }

        }
        return this.frame;
    }

    /**
     * Resume activity when you come back to the application
     */
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

    /**
     * Pause the process when the application is paused
     */
    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }

    }

    /**
     * When button is pressed changes boolean value to capture frame
     * @param view
     */
    public void TakeProfilePhoto(View view) {
        takePhoto = true;
    }

    /**
     * When return button is pressed, go back to previous activity
     * @param view
     */
    public void Return(View view) {
        Intent intent = new Intent(ChangeProfileImageActivity.this, ActivityChoicePage.class);
        startActivity(intent);
    }

    /**
     * Upload image on the Firebase cloud
     * @param bitmap
     */
    private void uploadImage(Bitmap bitmap) {
        ShowToast("Saving...");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        String uid = FirebaseAuth.getInstance().getUid();
        final StorageReference reference = FirebaseStorage.getInstance().getReference()
                .child("profileImages")
                .child(uid + ".jpeg");
        reference.putBytes(baos.toByteArray())
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        getDownloadUrl(reference);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("Take photo acitivty ", "fail", e.getCause());
                    }
                });

    }

    /**
     * get download Url used to upload image
     * @param reference
     */
    private void getDownloadUrl (StorageReference reference){
        reference.getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("Take Photo Activity", "on succes "+ uri);
                        // We change the profile image of the current user
                        setUserProfileUrl(uri);
                    }
                });
    }

    /**
     *
     * @param uri
     */
    private void setUserProfileUrl(Uri uri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // We change the profile Image of the user
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setPhotoUri(uri)
                .build();
        user.updateProfile(request)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(ChangeProfileImageActivity.this, "Saving successed", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChangeProfileImageActivity.this, "Profile Image Setting failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Method to transform matrix to bitmap
     * @param frame matrix to be converted
     * @return transfromed bitmap
     */
    // Method allowing to transform our frame into bitmap
    public static Bitmap bitmapFromMat(Mat frame) {
        Bitmap bitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(frame, bitmap);
        return bitmap;
    }

    /**
     * method to show Toast on screen
     * @param text text to be shown
     */
    public void ShowToast(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                int toastDurationInMilliSeconds = 2500;
                CountDownTimer toastCountDown;
                toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
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


}