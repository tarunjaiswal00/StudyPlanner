package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents an academic subject (e.g., "Data Structures").
 * Demonstrates: OOP composition, ArrayList, Collections sorting.
 */
public class Subject {

    private String     name;
    private String     colorHex; // for GUI display (e.g. "#4A90E2")
    private List<Task> tasks;

    public Subject(String name, String colorHex) {
        this.name     = name;
        this.colorHex = colorHex;
        this.tasks    = new ArrayList<>();
    }

    // --- Task management ---

    /** Adds a task to this subject. */
    public void addTask(Task task) {
        tasks.add(task);
    }

    /** Removes a task by index. */
    public void removeTask(int index) {
        if (index >= 0 && index < tasks.size()) {
            tasks.remove(index);
        }
    }

    /**
     * Returns tasks sorted by deadline then priority.
     * Demonstrates: Collections.sort with Comparable.
     */
    public List<Task> getSortedTasks() {
        List<Task> sorted = new ArrayList<>(tasks);
        Collections.sort(sorted);
        return sorted;
    }

    /** Returns only pending (not completed) tasks. */
    public List<Task> getPendingTasks() {
        return tasks.stream()
                    .filter(t -> !t.isCompleted())
                    .collect(Collectors.toList());
    }

    /** Returns only overdue tasks. */
    public List<Task> getOverdueTasks() {
        return tasks.stream()
                    .filter(Task::isOverdue)
                    .collect(Collectors.toList());
    }

    /** Returns completion progress as a percentage (0–100). */
    public int getProgressPercent() {
        if (tasks.isEmpty()) return 0;
        long done = tasks.stream().filter(Task::isCompleted).count();
        return (int) (done * 100 / tasks.size());
    }

    // --- Getters / Setters ---
    public String     getName()     { return name; }
    public String     getColorHex() { return colorHex; }
    public List<Task> getTasks()    { return tasks; }

    public void setName(String name)         { this.name = name; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    /**
     * Serialize to file-safe string.
     * Format: name|colorHex
     * (Tasks are stored separately in DataManager.)
     */
    public String toFileString() {
        return name + "|" + colorHex;
    }

    public static Subject fromFileString(String line) {
        String[] parts = line.split("\\|");
        return new Subject(parts[0], parts[1]);
    }

    @Override
    public String toString() { return name; }
}
