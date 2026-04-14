package com.example.acadmate;

public class AssignmentItem {
    public final int id;
    public final String title;
    public final String subject;
    public final String description;
    public final String dueDate;
    public final boolean reminderEnabled;
    public final boolean completed;

    public AssignmentItem(int id, String title, String subject, String description, String dueDate, boolean reminderEnabled, boolean completed) {
        this.id = id;
        this.title = title;
        this.subject = subject;
        this.description = description;
        this.dueDate = dueDate;
        this.reminderEnabled = reminderEnabled;
        this.completed = completed;
    }
}
