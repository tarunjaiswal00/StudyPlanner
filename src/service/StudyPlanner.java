package service;

import model.Subject;
import model.StudySession;
import model.Task;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central controller — owns the data and coordinates between
 * the GUI, DataManager, and ReminderService.
 *
 * Demonstrates: Facade pattern, ArrayList, HashMap, exception propagation.
 */
public class StudyPlanner {

    private final List<Subject>      subjects;
    private final List<StudySession> sessions;
    private final DataManager        dataManager;
    private final ReminderService    reminderService;

    // Palette of colours to auto-assign to new subjects
    private static final String[] COLOURS = {
        "#4A90E2", "#E24A4A", "#27AE60", "#F39C12",
        "#8E44AD", "#16A085", "#D35400", "#2C3E50"
    };
    private int colourIndex = 0;

    public StudyPlanner() {
        this.subjects        = new ArrayList<>();
        this.sessions        = new ArrayList<>();
        this.dataManager     = new DataManager();
        this.reminderService = new ReminderService(this::getSubjects);
    }

    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

    /** Loads persisted data and starts the reminder service. */
    public void start() {
        try {
            subjects.addAll(dataManager.loadSubjects());
            sessions.addAll(dataManager.loadSessions());
        } catch (IOException e) {
            System.err.println("Warning: could not load saved data — " + e.getMessage());
        }
        reminderService.start(30); // check every 30 minutes
    }

    /** Persists data and shuts down the reminder thread. */
    public void shutdown() {
        save();
        reminderService.stop();
    }

    /** Saves current state to disk. */
    public void save() {
        try {
            dataManager.saveSubjects(subjects);
            dataManager.saveSessions(sessions);
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Subject operations
    // ---------------------------------------------------------------

    public void addSubject(String name) {
        String colour = COLOURS[colourIndex % COLOURS.length];
        colourIndex++;
        subjects.add(new Subject(name, colour));
        save();
    }

    public void removeSubject(int index) {
        if (index >= 0 && index < subjects.size()) {
            subjects.remove(index);
            save();
        }
    }

    public List<Subject> getSubjects() { return subjects; }

    public Subject getSubject(int index) { return subjects.get(index); }

    // ---------------------------------------------------------------
    // Task operations (delegate to Subject)
    // ---------------------------------------------------------------

    public void addTask(Subject subject, Task task) {
        subject.addTask(task);
        save();
    }

    public void removeTask(Subject subject, int taskIndex) {
        subject.removeTask(taskIndex);
        save();
    }

    public void toggleTaskDone(Subject subject, int taskIndex) {
        List<Task> tasks = subject.getTasks();
        if (taskIndex >= 0 && taskIndex < tasks.size()) {
            tasks.get(taskIndex).toggleCompleted();
            save();
        }
    }

    // ---------------------------------------------------------------
    // Session (timer) operations
    // ---------------------------------------------------------------

    public StudySession startSession(String subjectName) {
        StudySession session = new StudySession(subjectName, LocalDateTime.now());
        sessions.add(session);
        return session;
    }

    public void finishSession(StudySession session, String notes) {
        session.finish();
        session.setNotes(notes);
        save();
    }

    public List<StudySession> getSessions() { return sessions; }

    // ---------------------------------------------------------------
    // Stats — used by the dashboard panel
    // ---------------------------------------------------------------

    /** Returns total study minutes per subject (completed sessions only). */
    public Map<String, Long> getStudyMinutesPerSubject() {
        Map<String, Long> map = new HashMap<>();
        for (StudySession s : sessions) {
            if (!s.isActive()) {
                map.merge(s.getSubjectName(), s.getDurationMinutes(), Long::sum);
            }
        }
        return map;
    }

    /** Returns count of all pending tasks across all subjects. */
    public int getTotalPendingTasks() {
        return subjects.stream()
                       .mapToInt(s -> s.getPendingTasks().size())
                       .sum();
    }
}
