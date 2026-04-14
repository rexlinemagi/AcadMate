package com.example.acadmate;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setTitle("About");
        UiEffects.applyInteractiveAnimations(findViewById(android.R.id.content));

        TextView tv = findViewById(R.id.tvAbout);
        tv.setText("AcadMate - Smart Academic Manager\n\nBuilt with Java + SQLite for student productivity.");
    }
}
