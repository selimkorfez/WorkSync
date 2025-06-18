package com.worksync.model;

public class TimeEntry {
    private String userUid;
    private String date;  // yyyy-MM-dd
    private double hours;
    private String notes;

    public TimeEntry() {}

    public TimeEntry(String userUid, String date, double hours, String notes) {
        this.userUid = userUid;
        this.date = date;
        this.hours = hours;
        this.notes = notes;
    }

    public String getUserUid() { return userUid; }
    public void setUserUid(String userUid) { this.userUid = userUid; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getHours() { return hours; }
    public void setHours(double hours) { this.hours = hours; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
