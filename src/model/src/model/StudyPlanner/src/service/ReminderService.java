package service;

import model.Subject;
import model.Task;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Runs a background thread that periodically checks for overdue or
 * due-today tasks and shows a Swing dialog notification.
 *
 * Demonstrates: Multithreading via ScheduledExecutorService,
 *               SwingUtilities.invokeLater for thread-safe GUI updates.
 */
public class ReminderService {

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?>             currentTask;

    // Callback so the service can fetch the latest subject list from the planner
    private final SubjectProvider subjectProvider;

    /** Functional interface — implemented by StudyPlanner as a lambda. */
    public interface SubjectProvider {
        List<Subject> getSubjects();
    }

    public ReminderService(SubjectProvider subjectProvider) {
        this.subjectProvider = subjectProvider;
        // Single daemon thread — won't prevent JVM shutdown
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ReminderThread");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts the reminder check every {@code intervalMinutes} minutes.
     * Also fires once immediately on start.
     */
    public void start(int intervalMinutes) {
        currentTask = scheduler.scheduleAtFixedRate(
            this::checkReminders,
            0,
            intervalMinutes,
            TimeUnit.MINUTES
        );
    }

    /** Stops the reminder service gracefully. */
    public void stop() {
        if (currentTask != null) currentTask.cancel(false);
        scheduler.shutdown();
    }

    /**
     * Core reminder logic — runs on the background thread.
     * GUI interaction is always dispatched back to the Event Dispatch Thread.
     */
    private void checkReminders() {
        List<Subject> subjects = subjectProvider.getSubjects();
        StringBuilder overdueMsg  = new StringBuilder();
        StringBuilder dueTodayMsg = new StringBuilder();

        for (Subject subject : subjects) {
            for (Task task : subject.getPendingTasks()) {
                if (task.isOverdue()) {
                    overdueMsg.append("  • [").append(subject.getName()).append("] ")
                              .append(task.getTitle()).append("\n");
                } else if (task.getDeadline().equals(java.time.LocalDate.now())) {
                    dueTodayMsg.append("  • [").append(subject.getName()).append("] ")
                               .append(task.getTitle()).append("\n");
                }
            }
        }

        if (overdueMsg.length() > 0 || dueTodayMsg.length() > 0) {
            StringBuilder message = new StringBuilder("Study Planner Reminder\n\n");
            if (overdueMsg.length() > 0) {
                message.append("OVERDUE:\n").append(overdueMsg).append("\n");
            }
            if (dueTodayMsg.length() > 0) {
                message.append("DUE TODAY:\n").append(dueTodayMsg);
            }

            final String msg = message.toString();

            // Must update Swing components on the Event Dispatch Thread
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                    null,
                    msg,
                    "Study Planner — Reminder",
                    JOptionPane.WARNING_MESSAGE
                )
            );
        }
    }
}
