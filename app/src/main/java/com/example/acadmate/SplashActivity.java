package com.example.acadmate;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Class<?> target = SessionManager.isLoggedIn(SplashActivity.this)
                        ? MainActivity.class
                        : LoginActivity.class;
                Intent intent = new Intent(SplashActivity.this, target);
                startActivity(intent);
                finish();
            }
        }, SPLASH_TIME);
    }
}