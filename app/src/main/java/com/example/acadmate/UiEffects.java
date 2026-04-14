package com.example.acadmate;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class UiEffects {

    private UiEffects() {
    }

    public static void applyInteractiveAnimations(View root) {
        if (root == null) {
            return;
        }
        walkAndApply(root);
    }

    private static void walkAndApply(View view) {
        if (view instanceof MaterialButton || view instanceof CardView) {
            attachPressScale(view);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                walkAndApply(group.getChildAt(i));
            }
        }
    }

    private static void attachPressScale(View view) {
        view.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            }
            return false;
        });
    }
}
