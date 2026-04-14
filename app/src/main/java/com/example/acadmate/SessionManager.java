package com.example.acadmate;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "acadmate_auth";
    private static final String KEY_USER_ID = "current_user_id";
    private static final String KEY_ROLE = "current_role";

    public static final String ROLE_TEACHER = "teacher";
    public static final String ROLE_STUDENT = "student";

    // Pre-seeded users
    public static final String USER_TEACHER_ID = "teacher";
    public static final String USER_TEACHER_PASSWORD = "1234";
    public static final String USER_STUDENT_ID = "student";
    public static final String USER_STUDENT_PASSWORD = "0000";

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void login(Context context, String userId, String role) {
        prefs(context).edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_ROLE, role)
                .apply();
    }

    public static void logout(Context context) {
        prefs(context).edit()
                .remove(KEY_USER_ID)
                .remove(KEY_ROLE)
                .apply();
    }

    public static boolean isLoggedIn(Context context) {
        return getCurrentUserId(context) != null;
    }

    public static String getCurrentUserId(Context context) {
        String id = prefs(context).getString(KEY_USER_ID, null);
        if (id == null || id.trim().isEmpty()) return null;
        return id;
    }

    public static String getCurrentRole(Context context) {
        String role = prefs(context).getString(KEY_ROLE, null);
        if (role == null || role.trim().isEmpty()) return null;
        return role;
    }

    public static boolean isTeacher(Context context) {
        return ROLE_TEACHER.equals(getCurrentRole(context));
    }

    public static boolean isStudent(Context context) {
        return ROLE_STUDENT.equals(getCurrentRole(context));
    }
}

