package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Records a single timed study session for a subject.
 * Demonstrates: LocalDateTime usage, encapsulation.
 */
public class StudySession {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private String        subjectName;  // link to Subject by name
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String        notes;

    public StudySession(String subjectName, LocalDateTime startTime) {
        this.subjectName = subjectName;
        this.startTime   = startTime;
        this.notes       = "";
    }

    /** Marks the session as finished at the current time. */
    public void finish() {
        this.endTime = LocalDateTime.now();
    }

    /** Returns duration in minutes, or -1 if session not yet finished. */
    public long getDurationMinutes() {
        if (endTime == null) return -1;
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    public boolean isActive() { return endTime == null; }

    // --- Getters / Setters ---
    public String        getSubjectName() { return subjectName; }
    public LocalDateTime getStartTime()   { return startTime; }
    public LocalDateTime getEndTime()     { return endTime; }
    public String        getNotes()       { return notes; }
    public void          setNotes(String notes) { this.notes = notes; }
    public void          setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    /** Serialize: subjectName|startTime|endTime|notes */
    public String toFileString() {
        String end = (endTime != null) ? endTime.format(FORMATTER) : "ACTIVE";
        return subjectName + "|" + startTime.format(FORMATTER) + "|" + end + "|" + notes;
    }

    public static StudySession fromFileString(String line) {
        String[] parts = line.split("\\|", 4);
        String        subject = parts[0];
        LocalDateTime start   = LocalDateTime.parse(parts[1], FORMATTER);
        StudySession  session = new StudySession(subject, start);
        if (!parts[2].equals("ACTIVE")) {
            session.setEndTime(LocalDateTime.parse(parts[2], FORMATTER));
        }
        if (parts.length > 3) session.setNotes(parts[3]);
        return session;
    }

    @Override
    public String toString() {
        String dur = (getDurationMinutes() >= 0) ?
                     getDurationMinutes() + " min" : "ongoing";
        return subjectName + " | " + startTime.format(FORMATTER) + " | " + dur;
    }
}
