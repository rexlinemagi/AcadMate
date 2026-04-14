package com.example.acadmate;

public class TimetableModel {
    int id;
    String subject, day, time;

    public TimetableModel(int id, String subject, String day, String time) {
        this.id = id;
        this.subject = subject;
        this.day = day;
        this.time = time;
    }
}