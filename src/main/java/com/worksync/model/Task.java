package com.worksync.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jakarta.persistence.Transient;
import java.util.List;
import java.util.ArrayList;

public class Task {

    private String id; // Firestore document ID
    private String title;
    private String description;
    private String assignedToUid; // User UID from Firebase
    private String assignedByUid;
    private String deadline;
    private String status; // "PENDING", "IN_PROGRESS", "COMPLETED"
    private boolean urgent;
    private List<TimeEntry> timeEntries = new ArrayList<>();
    private String inProgressStart;
    private String completionTime;


    @Transient
    private String completionTimeFormatted;

    public Task() {
        this.status = "PENDING";
        this.deadline = LocalDate.now().toString();
    }

    // Getters and Setters

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getAssignedToUid() { return assignedToUid; }

    public void setAssignedToUid(String assignedToUid) { this.assignedToUid = assignedToUid; }

    public String getAssignedByUid() { return assignedByUid; }

    public void setAssignedByUid(String assignedByUid) { this.assignedByUid = assignedByUid; }

    public String getDeadline() { return deadline; }

    public void setDeadline(String deadline) { this.deadline = deadline; }

    public String getStatus() { return status; }

    public void setStatus(String status) {
        this.status = (status == null || status.isBlank()) ? "PENDING" : status;
    }

    public boolean isUrgent() { return urgent; }

    public void setUrgent(boolean urgent) { this.urgent = urgent; }

    public List<TimeEntry> getTimeEntries() { return timeEntries; }

    public void setTimeEntries(List<TimeEntry> timeEntries) { this.timeEntries = timeEntries; }

    public String getInProgressStart() { return inProgressStart; }

    public void setInProgressStart(String inProgressStart) { this.inProgressStart = inProgressStart; }

    public String getCompletionTime() { return completionTime; }

    public void setCompletionTime(String completionTime) { this.completionTime = completionTime; }

    @Transient
    public boolean isLate() {
        if (status != null && !"COMPLETED".equals(status) && deadline != null && !deadline.isBlank()) {
            try {
                LocalDate deadlineDate = LocalDate.parse(deadline, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                return LocalDate.now().isAfter(deadlineDate);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public String getCompletionTimeFormatted() {
        return completionTimeFormatted;
    }

    public void setCompletionTimeFormatted(String completionTimeFormatted) {
        this.completionTimeFormatted = completionTimeFormatted;
    }
    private boolean archived = false;

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

}
