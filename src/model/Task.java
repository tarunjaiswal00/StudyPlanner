package model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Represents a study task with a deadline and priority.
 * Demonstrates: Encapsulation, constructor overloading, Comparable.
 */
public class Task implements Comparable<Task> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private String title;
    private String description;
    private LocalDate deadline;
    private Priority priority;
    private boolean completed;

    // Full constructor
    public Task(String title, String description, LocalDate deadline, Priority priority) {
        this.title       = title;
        this.description = description;
        this.deadline    = deadline;
        this.priority    = priority;
        this.completed   = false;
    }

    // Minimal constructor — overloading demo
    public Task(String title, LocalDate deadline) {
        this(title, "", deadline, Priority.MEDIUM);
    }

    // --- Getters and Setters ---
    public String    getTitle()       { return title; }
    public String    getDescription() { return description; }
    public LocalDate getDeadline()    { return deadline; }
    public Priority  getPriority()    { return priority; }
    public boolean   isCompleted()    { return completed; }

    public void setTitle(String title)            { this.title = title; }
    public void setDescription(String description){ this.description = description; }
    public void setDeadline(LocalDate deadline)   { this.deadline = deadline; }
    public void setPriority(Priority priority)    { this.priority = priority; }
    public void setCompleted(boolean completed)   { this.completed = completed; }

    /** Toggles completion status. */
    public void toggleCompleted() { this.completed = !this.completed; }

    /** Checks if the task is overdue (deadline passed, not yet done). */
    public boolean isOverdue() {
        return !completed && deadline.isBefore(LocalDate.now());
    }

    /**
     * Natural order: by deadline first, then by priority level descending.
     * Demonstrates: Comparable interface.
     */
    @Override
    public int compareTo(Task other) {
        int dateCompare = this.deadline.compareTo(other.deadline);
        if (dateCompare != 0) return dateCompare;
        return Integer.compare(other.priority.getLevel(), this.priority.getLevel());
    }

    /**
     * Serialize to a single CSV line for file storage.
     * Format: title|description|deadline|priority|completed
     */
    public String toFileString() {
        return title + "|" + description + "|" +
               deadline.format(FORMATTER) + "|" +
               priority.name() + "|" + completed;
    }

    /**
     * Deserialize from a CSV line (mirrors toFileString).
     * Demonstrates: static factory method pattern.
     */
    public static Task fromFileString(String line) {
        String[] parts = line.split("\\|", -1);
        String    title       = parts[0];
        String    description = parts[1];
        LocalDate deadline    = LocalDate.parse(parts[2], FORMATTER);
        Priority  priority    = Priority.valueOf(parts[3]);
        boolean   completed   = Boolean.parseBoolean(parts[4]);

        Task t = new Task(title, description, deadline, priority);
        t.setCompleted(completed);
        return t;
    }

    @Override
    public String toString() {
        return "[" + (completed ? "X" : " ") + "] " +
               title + " | " + priority + " | Due: " + deadline.format(FORMATTER);
    }
}
