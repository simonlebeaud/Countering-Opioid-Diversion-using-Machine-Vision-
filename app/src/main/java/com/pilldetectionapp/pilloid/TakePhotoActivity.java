package com.pilldetectionapp.pilloid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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

import java.io.ByteArrayOutputStream;

public class TakePhotoActivity extends AppCompatActivity {
    private ImageView my_image;
    private com.google.android.material.floatingactionbutton.FloatingActionButton Return_Button;
    private Toast toast;

    private int REQUEST_CODE_FOR_IMAGE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo_activty);
        my_image = (ImageView) findViewById(R.id.imageView);
        // set button
        Return_Button=findViewById(R.id.ReturnButton);
        Return_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // WHen the user click on the the run_button we launch the detection activity
                Intent intent = new Intent(TakePhotoActivity.this, ActivityChoicePage.class);
                startActivity(intent);
            }
        });

    }

    public void takePhoto(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity((getPackageManager())) != null) {
            startActivityForResult(intent, REQUEST_CODE_FOR_IMAGE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_FOR_IMAGE) {
            switch (resultCode) {
                case RESULT_OK:
                    Log.i("Photo Activity", "Result OK");
                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                    my_image.setImageBitmap(bitmap);
                    uploadImage(bitmap);
                    break;
                case RESULT_CANCELED:
                    Log.i("Photo Activity", "Result Cancelled");
                    break;
                default:
                    break;
            }
        }
    }

    // Upload the image on the cloud
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
                        Toast.makeText(TakePhotoActivity.this, "Saving successed", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(TakePhotoActivity.this, "Profile Image Setting failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void ShowToast(final String text){
        runOnUiThread(new Runnable() {
            public void run()
            {
                int toastDurationInMilliSeconds = 2500;
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

}