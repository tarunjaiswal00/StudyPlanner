package service;

import model.Subject;
import model.StudySession;
import model.Task;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all file read/write operations for the app.
 * Demonstrates: File I/O, BufferedReader/Writer, Exception Handling.
 */
public class DataManager {

    // Data files stored in a /data subfolder next to the jar
    private static final String DATA_DIR      = "data/";
    private static final String SUBJECTS_FILE = DATA_DIR + "subjects.txt";
    private static final String SESSIONS_FILE = DATA_DIR + "sessions.txt";

    public DataManager() {
        // Ensure the data directory exists
        new File(DATA_DIR).mkdirs();
    }

    // ---------------------------------------------------------------
    // SUBJECTS
    // ---------------------------------------------------------------

    /**
     * Saves all subjects and their tasks to disk.
     * Each subject is one line; its tasks follow prefixed with "TASK:".
     * Demonstrates: BufferedWriter, try-with-resources, IOException handling.
     */
    public void saveSubjects(List<Subject> subjects) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(SUBJECTS_FILE))) {
            for (Subject s : subjects) {
                bw.write("SUBJECT:" + s.toFileString());
                bw.newLine();
                for (Task t : s.getTasks()) {
                    bw.write("TASK:" + t.toFileString());
                    bw.newLine();
                }
            }
        }
        // IOException propagates to caller — let the GUI show the error
    }

    /**
     * Loads subjects (and their tasks) from disk.
     * Demonstrates: BufferedReader, line parsing, custom exceptions via try-catch.
     */
    public List<Subject> loadSubjects() throws IOException {
        List<Subject> subjects = new ArrayList<>();
        File file = new File(SUBJECTS_FILE);
        if (!file.exists()) return subjects; // fresh install — no data yet

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            Subject current = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("SUBJECT:")) {
                    current = Subject.fromFileString(line.substring(8));
                    subjects.add(current);
                } else if (line.startsWith("TASK:") && current != null) {
                    try {
                        Task t = Task.fromFileString(line.substring(5));
                        current.addTask(t);
                    } catch (Exception e) {
                        // Skip malformed task lines — don't crash the whole load
                        System.err.println("Skipping malformed task line: " + line);
                    }
                }
            }
        }
        return subjects;
    }

    // ---------------------------------------------------------------
    // SESSIONS
    // ---------------------------------------------------------------

    public void saveSessions(List<StudySession> sessions) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(SESSIONS_FILE))) {
            for (StudySession s : sessions) {
                // Only save completed sessions
                if (!s.isActive()) {
                    bw.write(s.toFileString());
                    bw.newLine();
                }
            }
        }
    }

    public List<StudySession> loadSessions() throws IOException {
        List<StudySession> sessions = new ArrayList<>();
        File file = new File(SESSIONS_FILE);
        if (!file.exists()) return sessions;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    try {
                        sessions.add(StudySession.fromFileString(line));
                    } catch (Exception e) {
                        System.err.println("Skipping malformed session: " + line);
                    }
                }
            }
        }
        return sessions;
    }
}
