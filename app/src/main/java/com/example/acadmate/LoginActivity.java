package com.example.acadmate;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle("AcadMate Login");
        UiEffects.applyInteractiveAnimations(findViewById(android.R.id.content));

        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvDemoHint = findViewById(R.id.tvDemoHint);

        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        //tvDemoHint.setText("Teacher: teacher / 1234\nStudent: student / 0000");

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (SessionManager.USER_TEACHER_ID.equals(username)
                    && SessionManager.USER_TEACHER_PASSWORD.equals(password)) {
                SessionManager.login(this, SessionManager.USER_TEACHER_ID, SessionManager.ROLE_TEACHER);
                goToMain();
            } else if (SessionManager.USER_STUDENT_ID.equals(username)
                    && SessionManager.USER_STUDENT_PASSWORD.equals(password)) {
                SessionManager.login(this, SessionManager.USER_STUDENT_ID, SessionManager.ROLE_STUDENT);
                goToMain();
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

