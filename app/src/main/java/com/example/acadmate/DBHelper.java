package com.example.acadmate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 4;

    public DBHelper(Context context) {
        super(context, resolveDbName(context), null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE subjects (" +
                "subject_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject_name TEXT UNIQUE NOT NULL)");
        db.execSQL("CREATE TABLE timetable (" +
                "timetable_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject_name TEXT NOT NULL," +
                "day_order INTEGER NOT NULL," +
                "time_slot TEXT NOT NULL," +
                "classroom TEXT NOT NULL)");
        db.execSQL("CREATE TABLE assignments (" +
                "assignment_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "subject TEXT NOT NULL," +
                "description TEXT," +
                "due_date TEXT NOT NULL," +
                "reminder_enabled INTEGER NOT NULL DEFAULT 0," +
                "is_completed INTEGER NOT NULL DEFAULT 0," +
                "reminder_sent_on TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE attendance (" +
                "attendance_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject TEXT UNIQUE NOT NULL," +
                "total_classes INTEGER NOT NULL," +
                "attended_classes INTEGER NOT NULL," +
                "last_updated_by TEXT NOT NULL DEFAULT ''," +
                "last_updated_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE marks (" +
                "mark_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "subject TEXT NOT NULL," +
                "assessment_type TEXT NOT NULL," +
                "marks REAL NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            ensureAttendanceAuditColumns(db);
        }
        if (oldVersion < 4) {
            ensureAssignmentColumns(db);
        }
    }

    private static String resolveDbName(Context context) {
        String userId = SessionManager.getCurrentUserId(context);
        if (userId == null) {
            return "AcadMate_default.db";
        }
        return "AcadMate_" + userId + ".db";
    }

    public long addTimetable(String subjectName, int dayOrder, String timeSlot) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues subjectValues = new ContentValues();
        subjectValues.put("subject_name", subjectName);
        db.insertWithOnConflict("subjects", null, subjectValues, SQLiteDatabase.CONFLICT_IGNORE);

        ContentValues values = new ContentValues();
        values.put("subject_name", subjectName);
        values.put("day_order", dayOrder);
        values.put("time_slot", timeSlot);
        values.put("classroom", "");
        return db.insert("timetable", null, values);
    }

    public List<String> getSubjects() {
        List<String> subjects = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT subject_name FROM subjects ORDER BY subject_name ASC", null);
        if (cursor.moveToFirst()) {
            do {
                subjects.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return subjects;
    }

    public Cursor getTimetableByDay(int dayOrder) {
        return getReadableDatabase().rawQuery(
                "SELECT timetable_id, subject_name, time_slot, classroom FROM timetable " +
                        "WHERE day_order = ? ORDER BY time_slot ASC",
                new String[]{String.valueOf(dayOrder)});
    }

    public Cursor getAllTimetableRows() {
        return getReadableDatabase().rawQuery(
                "SELECT timetable_id, subject_name, day_order, time_slot FROM timetable " +
                        "ORDER BY day_order ASC, time_slot ASC",
                null);
    }

    public boolean hasTimetableConflict(int dayOrder, String timeSlot, Integer excludeTimetableId) {
        String sql = "SELECT timetable_id FROM timetable WHERE day_order = ? AND time_slot = ?";
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(dayOrder));
        args.add(timeSlot);
        if (excludeTimetableId != null) {
            sql += " AND timetable_id != ?";
            args.add(String.valueOf(excludeTimetableId));
        }
        Cursor cursor = getReadableDatabase().rawQuery(sql, args.toArray(new String[0]));
        boolean conflict = cursor.moveToFirst();
        cursor.close();
        return conflict;
    }

    public int updateTimetable(int timetableId, String subjectName, int dayOrder, String timeSlot) {
        ContentValues values = new ContentValues();
        values.put("subject_name", subjectName);
        values.put("day_order", dayOrder);
        values.put("time_slot", timeSlot);
        values.put("classroom", "");
        return getWritableDatabase().update("timetable", values, "timetable_id = ?", new String[]{String.valueOf(timetableId)});
    }

    public int deleteTimetable(int timetableId) {
        return getWritableDatabase().delete("timetable", "timetable_id = ?", new String[]{String.valueOf(timetableId)});
    }

    public Cursor getAllAssignmentsSorted() {
        ensureAssignmentColumns(getWritableDatabase());
        return getReadableDatabase().rawQuery(
                "SELECT assignment_id, title, subject, description, due_date, reminder_enabled, is_completed, reminder_sent_on " +
                        "FROM assignments ORDER BY due_date ASC", null);
    }

    public long addAssignment(String title, String subject, String description, String dueDate, boolean reminder) {
        ensureAssignmentColumns(getWritableDatabase());
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("subject", subject);
        values.put("description", description);
        values.put("due_date", dueDate);
        values.put("reminder_enabled", reminder ? 1 : 0);
        values.put("is_completed", 0);
        values.put("reminder_sent_on", "");
        return getWritableDatabase().insert("assignments", null, values);
    }

    public int updateAssignmentCompleted(int assignmentId, boolean completed) {
        ensureAssignmentColumns(getWritableDatabase());
        ContentValues values = new ContentValues();
        values.put("is_completed", completed ? 1 : 0);
        return getWritableDatabase().update("assignments", values, "assignment_id = ?", new String[]{String.valueOf(assignmentId)});
    }

    public int updateAssignment(int assignmentId, String title, String subject, String description, String dueDate, boolean reminderEnabled) {
        ensureAssignmentColumns(getWritableDatabase());
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("subject", subject);
        values.put("description", description);
        values.put("due_date", dueDate);
        values.put("reminder_enabled", reminderEnabled ? 1 : 0);
        values.put("reminder_sent_on", "");
        return getWritableDatabase().update("assignments", values, "assignment_id = ?", new String[]{String.valueOf(assignmentId)});
    }

    public int deleteAssignment(int assignmentId) {
        return getWritableDatabase().delete("assignments", "assignment_id = ?", new String[]{String.valueOf(assignmentId)});
    }

    public Cursor getAssignmentsForReminderDate(String targetDate) {
        ensureAssignmentColumns(getWritableDatabase());
        return getReadableDatabase().rawQuery(
                "SELECT assignment_id, title, subject, due_date, reminder_sent_on " +
                        "FROM assignments " +
                        "WHERE due_date = ? AND reminder_enabled = 1 AND is_completed = 0 " +
                        "ORDER BY due_date ASC",
                new String[]{targetDate});
    }

    public int markAssignmentReminderSent(int assignmentId, String sentOnDate) {
        ensureAssignmentColumns(getWritableDatabase());
        ContentValues values = new ContentValues();
        values.put("reminder_sent_on", sentOnDate);
        return getWritableDatabase().update("assignments", values, "assignment_id = ?", new String[]{String.valueOf(assignmentId)});
    }

    public long upsertAttendance(String subject, int totalClasses, int attendedClasses, String updatedBy, long updatedAt) {
        SQLiteDatabase db = getWritableDatabase();
        ensureAttendanceAuditColumns(db);
        ContentValues values = new ContentValues();
        values.put("subject", subject);
        values.put("total_classes", totalClasses);
        values.put("attended_classes", attendedClasses);
        values.put("last_updated_by", updatedBy);
        values.put("last_updated_at", updatedAt);
        return db.insertWithOnConflict("attendance", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor getAttendanceRows() {
        ensureAttendanceAuditColumns(getWritableDatabase());
        return getReadableDatabase().rawQuery(
                "SELECT attendance_id, subject, total_classes, attended_classes, last_updated_by, last_updated_at " +
                        "FROM attendance ORDER BY subject ASC",
                null);
    }

    public int deleteAttendanceBySubject(String subject) {
        return getWritableDatabase().delete("attendance", "subject = ?", new String[]{subject});
    }

    public Cursor getLowAttendanceRows(float thresholdPercent) {
        return getReadableDatabase().rawQuery(
                "SELECT subject, total_classes, attended_classes FROM attendance " +
                        "WHERE total_classes > 0 AND ((attended_classes * 100.0) / total_classes) < ? " +
                        "ORDER BY subject ASC",
                new String[]{String.valueOf(thresholdPercent)});
    }

    public long addMark(String subject, String assessmentType, float marks) {
        ContentValues values = new ContentValues();
        values.put("subject", subject);
        values.put("assessment_type", assessmentType);
        values.put("marks", marks);
        return getWritableDatabase().insert("marks", null, values);
    }

    public Cursor getAllMarksDetailed() {
        return getReadableDatabase().rawQuery(
                "SELECT mark_id, subject, assessment_type, marks FROM marks ORDER BY subject ASC, assessment_type ASC",
                null);
    }

    public int updateMarkById(int markId, float marks) {
        ContentValues values = new ContentValues();
        values.put("marks", marks);
        return getWritableDatabase().update("marks", values, "mark_id = ?", new String[]{String.valueOf(markId)});
    }

    public int deleteMarkById(int markId) {
        return getWritableDatabase().delete("marks", "mark_id = ?", new String[]{String.valueOf(markId)});
    }

    public Cursor getMarksBySubject() {
        return getReadableDatabase().rawQuery(
                "SELECT subject, " +
                        "MAX(CASE WHEN assessment_type='CA1' THEN marks END) AS ca1, " +
                        "MAX(CASE WHEN assessment_type='CA2' THEN marks END) AS ca2, " +
                        "MAX(CASE WHEN assessment_type='Component 1' THEN marks END) AS comp1, " +
                        "MAX(CASE WHEN assessment_type='Component 2' THEN marks END) AS comp2 " +
                        "FROM marks GROUP BY subject ORDER BY subject ASC",
                null);
    }

    public Cursor getAssignmentEventsForDate(String date) {
        return getReadableDatabase().rawQuery(
                "SELECT title, subject FROM assignments WHERE due_date = ? ORDER BY title ASC",
                new String[]{date});
    }

    public List<Integer> getAssignmentDaysForMonth(int year, int monthOneBased) {
        LinkedHashSet<Integer> uniqueDays = new LinkedHashSet<>();
        String prefix = String.format(Locale.US, "%04d-%02d-", year, monthOneBased);
        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT due_date FROM assignments WHERE due_date LIKE ? ORDER BY due_date ASC",
                new String[]{prefix + "%"});
        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(0);
                if (date != null && date.length() >= 10) {
                    try {
                        uniqueDays.add(Integer.parseInt(date.substring(8, 10)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return new ArrayList<>(uniqueDays);
    }

    public Cursor getDueTodayOrOverdueAssignments(String todayDate) {
        ensureAssignmentColumns(getWritableDatabase());
        return getReadableDatabase().rawQuery(
                "SELECT title, subject, due_date FROM assignments " +
                        "WHERE is_completed = 0 AND due_date <= ? ORDER BY due_date ASC",
                new String[]{todayDate});
    }

    public void clearAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("marks", null, null);
        db.delete("attendance", null, null);
        db.delete("assignments", null, null);
        db.delete("timetable", null, null);
        db.delete("subjects", null, null);
    }

    private void ensureAttendanceAuditColumns(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE attendance ADD COLUMN last_updated_by TEXT NOT NULL DEFAULT ''");
        } catch (Exception ignored) {
            // Column already exists or attendance table not ready.
        }
        try {
            db.execSQL("ALTER TABLE attendance ADD COLUMN last_updated_at INTEGER NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
            // Column already exists or attendance table not ready.
        }
    }

    private void ensureAssignmentColumns(SQLiteDatabase db) {
        try {
            db.execSQL("ALTER TABLE assignments ADD COLUMN is_completed INTEGER NOT NULL DEFAULT 0");
        } catch (Exception ignored) {
            // Column already exists or assignments table not ready.
        }
        try {
            db.execSQL("ALTER TABLE assignments ADD COLUMN reminder_sent_on TEXT NOT NULL DEFAULT ''");
        } catch (Exception ignored) {
            // Column already exists or assignments table not ready.
        }
    }
}