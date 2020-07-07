package com.pilldetectionapp.pilloid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class FirstPageActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_page);

        // We disconnect the user if there is one
        FirebaseAuth.getInstance().signOut();
    }


    public void StartAuthentication(View view) {
        Intent intent = new Intent(FirstPageActivity.this, ActivityChoicePage.class);
        startActivity(intent);
    }
}