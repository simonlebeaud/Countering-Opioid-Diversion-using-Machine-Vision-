package com.pilldetectionapp.pilloid;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class GetTextActivity extends AppCompatActivity {
    public static final String EXTRA_TEXT = "Pill.Text";
    private EditText WrittenText;
    private String text;
    private Button mybutton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_text);

        WrittenText = (EditText)findViewById(R.id.Edit);
        WrittenText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                text = WrittenText.getText().toString();
                if (!text.equals("Text")){
                    mybutton = (Button)findViewById(R.id.ChangeActivity);
                    mybutton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(GetTextActivity.this, CameraActivity.class);
                            intent.putExtra(EXTRA_TEXT,text);
                            startActivity(intent);
                        }
                    });

                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }
}