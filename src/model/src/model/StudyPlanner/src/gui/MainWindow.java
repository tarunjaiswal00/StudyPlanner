package gui;

import model.Priority;
import model.Subject;
import model.StudySession;
import model.Task;
import service.StudyPlanner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Main application window.
 * Demonstrates: JFrame, JTabbedPane, JTable, JList, Swing event listeners,
 *               MVC-style separation (GUI talks only to StudyPlanner).
 */
public class MainWindow extends JFrame {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final StudyPlanner planner;

    // ---- Subject panel components ----
    private DefaultListModel<String> subjectListModel;
    private JList<String>            subjectList;
    private JTextField               subjectNameField;

    // ---- Task panel components ----
    private JComboBox<String>        taskSubjectCombo;
    private DefaultTableModel        taskTableModel;
    private JTable                   taskTable;
    private JTextField               taskTitleField;
    private JTextField               taskDeadlineField;
    private JComboBox<Priority>      taskPriorityCombo;
    private JTextArea                taskDescArea;

    // ---- Timer panel components ----
    private JComboBox<String>        timerSubjectCombo;
    private JLabel                   timerLabel;
    private JButton                  startTimerBtn;
    private JButton                  stopTimerBtn;
    private StudySession             activeSession;
    private Timer                    swingTimer; // javax.swing.Timer for UI clock
    private long                     timerStartMillis;

    // ---- Stats panel ----
    private JTextArea                statsArea;

    // ---------------------------------------------------------------

    public MainWindow(StudyPlanner planner) {
        this.planner = planner;
        setTitle("Study Planner");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(900, 620);
        setMinimumSize(new Dimension(700, 480));
        setLocationRelativeTo(null);

        // Save & quit when window is closed
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                planner.shutdown();
                dispose();
                System.exit(0);
            }
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Subjects",    buildSubjectPanel());
        tabs.addTab("Tasks",       buildTaskPanel());
        tabs.addTab("Study Timer", buildTimerPanel());
        tabs.addTab("Stats",       buildStatsPanel());

        // Refresh stats whenever that tab is selected
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 3) refreshStats();
        });

        add(tabs);
    }

    // ---------------------------------------------------------------
    // SUBJECT PANEL
    // ---------------------------------------------------------------

    private JPanel buildSubjectPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // List of subjects
        subjectListModel = new DefaultListModel<>();
        refreshSubjectList();
        subjectList = new JList<>(subjectListModel);
        subjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(subjectList), BorderLayout.CENTER);

        // Add / Remove controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        subjectNameField = new JTextField(16);
        JButton addBtn    = new JButton("Add Subject");
        JButton removeBtn = new JButton("Remove Selected");

        addBtn.addActionListener(e -> addSubject());
        removeBtn.addActionListener(e -> removeSubject());

        // Allow pressing Enter in the text field to add
        subjectNameField.addActionListener(e -> addSubject());

        controls.add(new JLabel("Name:"));
        controls.add(subjectNameField);
        controls.add(addBtn);
        controls.add(removeBtn);

        panel.add(controls, BorderLayout.SOUTH);

        JLabel hint = new JLabel("Add the subjects you are studying this semester.");
        hint.setForeground(Color.GRAY);
        hint.setBorder(new EmptyBorder(0, 0, 6, 0));
        panel.add(hint, BorderLayout.NORTH);

        return panel;
    }

    private void addSubject() {
        String name = subjectNameField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter a subject name.");
            return;
        }
        planner.addSubject(name);
        subjectNameField.setText("");
        refreshSubjectList();
        refreshSubjectCombos();
    }

    private void removeSubject() {
        int idx = subjectList.getSelectedIndex();
        if (idx < 0) { showError("Select a subject first."); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Remove \"" + planner.getSubject(idx).getName() + "\" and all its tasks?",
            "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            planner.removeSubject(idx);
            refreshSubjectList();
            refreshSubjectCombos();
        }
    }

    private void refreshSubjectList() {
        subjectListModel = new DefaultListModel<>();
        for (Subject s : planner.getSubjects()) {
            subjectListModel.addElement(
                s.getName() + "  (" + s.getProgressPercent() + "% done)");
        }
        if (subjectList != null) subjectList.setModel(subjectListModel);
    }

    // ---------------------------------------------------------------
    // TASK PANEL
    // ---------------------------------------------------------------

    private JPanel buildTaskPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // --- Subject selector ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        taskSubjectCombo = new JComboBox<>();
        refreshSubjectCombos();
        taskSubjectCombo.addActionListener(e -> refreshTaskTable());
        topBar.add(new JLabel("Subject:"));
        topBar.add(taskSubjectCombo);
        panel.add(topBar, BorderLayout.NORTH);

        // --- Task table ---
        String[] cols = {"#", "Title", "Deadline", "Priority", "Done", "Overdue"};
        taskTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 4 || c == 5) ? Boolean.class : String.class;
            }
        };
        taskTable = new JTable(taskTableModel);
        taskTable.getColumnModel().getColumn(0).setMaxWidth(30);
        taskTable.getColumnModel().getColumn(4).setMaxWidth(50);
        taskTable.getColumnModel().getColumn(5).setMaxWidth(60);
        refreshTaskTable();
        panel.add(new JScrollPane(taskTable), BorderLayout.CENTER);

        // --- Add / action buttons ---
        JPanel south = new JPanel(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        taskTitleField    = new JTextField(18);
        taskDeadlineField = new JTextField("dd-MM-yyyy", 10);
        taskPriorityCombo = new JComboBox<>(Priority.values());
        taskDescArea      = new JTextArea(2, 18);
        taskDescArea.setLineWrap(true);

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Title:"),    gbc);
        gbc.gridx = 1;                form.add(taskTitleField,            gbc);
        gbc.gridx = 2; gbc.gridy = 0; form.add(new JLabel("Deadline:"), gbc);
        gbc.gridx = 3;                form.add(taskDeadlineField,         gbc);
        gbc.gridx = 4; gbc.gridy = 0; form.add(new JLabel("Priority:"), gbc);
        gbc.gridx = 5;                form.add(taskPriorityCombo,         gbc);
        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Notes:"),    gbc);
        gbc.gridx = 1; gbc.gridwidth = 4; form.add(new JScrollPane(taskDescArea), gbc);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addTaskBtn    = new JButton("Add Task");
        JButton toggleDoneBtn = new JButton("Toggle Done");
        JButton removeTaskBtn = new JButton("Remove Task");

        addTaskBtn.addActionListener(e    -> addTask());
        toggleDoneBtn.addActionListener(e -> toggleTask());
        removeTaskBtn.addActionListener(e -> removeTask());

        btnRow.add(addTaskBtn);
        btnRow.add(toggleDoneBtn);
        btnRow.add(removeTaskBtn);

        south.add(form,   BorderLayout.NORTH);
        south.add(btnRow, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshSubjectCombos() {
        List<Subject> subs = planner.getSubjects();

        if (taskSubjectCombo != null) {
            String prev = (String) taskSubjectCombo.getSelectedItem();
            taskSubjectCombo.removeAllItems();
            for (Subject s : subs) taskSubjectCombo.addItem(s.getName());
            if (prev != null) taskSubjectCombo.setSelectedItem(prev);
        }
        if (timerSubjectCombo != null) {
            String prev = (String) timerSubjectCombo.getSelectedItem();
            timerSubjectCombo.removeAllItems();
            for (Subject s : subs) timerSubjectCombo.addItem(s.getName());
            if (prev != null) timerSubjectCombo.setSelectedItem(prev);
        }
    }

    private void refreshTaskTable() {
        taskTableModel.setRowCount(0);
        int subIdx = taskSubjectCombo.getSelectedIndex();
        if (subIdx < 0 || subIdx >= planner.getSubjects().size()) return;

        Subject subject = planner.getSubject(subIdx);
        List<Task> tasks = subject.getSortedTasks();

        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            taskTableModel.addRow(new Object[]{
                i + 1,
                t.getTitle(),
                t.getDeadline().format(DATE_FMT),
                t.getPriority().getLabel(),
                t.isCompleted(),
                t.isOverdue()
            });
        }
    }

    private void addTask() {
        int subIdx = taskSubjectCombo.getSelectedIndex();
        if (subIdx < 0) { showError("Select a subject first."); return; }

        String title = taskTitleField.getText().trim();
        if (title.isEmpty()) { showError("Task title cannot be empty."); return; }

        LocalDate deadline;
        try {
            deadline = LocalDate.parse(taskDeadlineField.getText().trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            showError("Deadline must be in dd-MM-yyyy format.");
            return;
        }

        Priority priority = (Priority) taskPriorityCombo.getSelectedItem();
        String   desc     = taskDescArea.getText().trim();

        Task newTask = new Task(title, desc, deadline, priority);
        planner.addTask(planner.getSubject(subIdx), newTask);

        taskTitleField.setText("");
        taskDeadlineField.setText("dd-MM-yyyy");
        taskDescArea.setText("");

        refreshTaskTable();
        refreshSubjectList(); // update progress %
    }

    private void toggleTask() {
        int row    = taskTable.getSelectedRow();
        int subIdx = taskSubjectCombo.getSelectedIndex();
        if (row < 0 || subIdx < 0) { showError("Select a task first."); return; }
        planner.toggleTaskDone(planner.getSubject(subIdx), row);
        refreshTaskTable();
        refreshSubjectList();
    }

    private void removeTask() {
        int row    = taskTable.getSelectedRow();
        int subIdx = taskSubjectCombo.getSelectedIndex();
        if (row < 0 || subIdx < 0) { showError("Select a task first."); return; }
        planner.removeTask(planner.getSubject(subIdx), row);
        refreshTaskTable();
        refreshSubjectList();
    }

    // ---------------------------------------------------------------
    // TIMER PANEL
    // ---------------------------------------------------------------

    private JPanel buildTimerPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        timerSubjectCombo = new JComboBox<>();
        refreshSubjectCombos();

        timerLabel = new JLabel("00:00:00", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Monospaced", Font.BOLD, 48));

        startTimerBtn = new JButton("Start Session");
        stopTimerBtn  = new JButton("Stop Session");
        stopTimerBtn.setEnabled(false);

        startTimerBtn.addActionListener(e -> startTimer());
        stopTimerBtn.addActionListener(e  -> stopTimer());

        // Swing timer ticks every second to update the clock label
        swingTimer = new Timer(1000, e -> updateTimerDisplay());

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1;
        panel.add(timerSubjectCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        panel.add(timerLabel, gbc);
        gbc.gridy = 2; gbc.gridwidth = 1;
        panel.add(startTimerBtn, gbc);
        gbc.gridx = 1;
        panel.add(stopTimerBtn, gbc);

        return panel;
    }

    private void startTimer() {
        int subIdx = timerSubjectCombo.getSelectedIndex();
        if (subIdx < 0 || planner.getSubjects().isEmpty()) {
            showError("Add a subject first.");
            return;
        }
        String subjectName = planner.getSubject(subIdx).getName();
        activeSession      = planner.startSession(subjectName);
        timerStartMillis   = System.currentTimeMillis();

        swingTimer.start();
        startTimerBtn.setEnabled(false);
        stopTimerBtn.setEnabled(true);
        timerSubjectCombo.setEnabled(false);
    }

    private void stopTimer() {
        swingTimer.stop();
        String notes = JOptionPane.showInputDialog(this,
            "Session ended! Add notes (optional):", "Session Notes",
            JOptionPane.PLAIN_MESSAGE);
        planner.finishSession(activeSession, notes == null ? "" : notes);
        activeSession = null;

        timerLabel.setText("00:00:00");
        startTimerBtn.setEnabled(true);
        stopTimerBtn.setEnabled(false);
        timerSubjectCombo.setEnabled(true);
    }

    private void updateTimerDisplay() {
        long elapsed  = System.currentTimeMillis() - timerStartMillis;
        long hrs  = elapsed / 3_600_000;
        long mins = (elapsed % 3_600_000) / 60_000;
        long secs = (elapsed % 60_000)    / 1_000;
        timerLabel.setText(String.format("%02d:%02d:%02d", hrs, mins, secs));
    }

    // ---------------------------------------------------------------
    // STATS PANEL
    // ---------------------------------------------------------------

    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        panel.add(new JScrollPane(statsArea), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshStats());
        panel.add(refreshBtn, BorderLayout.SOUTH);

        refreshStats();
        return panel;
    }

    private void refreshStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== STUDY PLANNER STATS ===\n\n");

        sb.append("Total pending tasks : ").append(planner.getTotalPendingTasks()).append("\n\n");

        sb.append("--- Per-subject progress ---\n");
        for (Subject s : planner.getSubjects()) {
            sb.append(String.format("  %-22s  %3d%%  (%d pending, %d overdue)\n",
                s.getName(),
                s.getProgressPercent(),
                s.getPendingTasks().size(),
                s.getOverdueTasks().size()));
        }

        sb.append("\n--- Study time logged (minutes) ---\n");
        Map<String, Long> minutes = planner.getStudyMinutesPerSubject();
        if (minutes.isEmpty()) {
            sb.append("  No completed sessions yet.\n");
        } else {
            minutes.forEach((name, mins) ->
                sb.append(String.format("  %-22s  %d min\n", name, mins)));
        }

        sb.append("\n--- Recent sessions ---\n");
        List<StudySession> sessions = planner.getSessions();
        int start = Math.max(0, sessions.size() - 5);
        for (int i = start; i < sessions.size(); i++) {
            sb.append("  ").append(sessions.get(i)).append("\n");
        }

        statsArea.setText(sb.toString());
        statsArea.setCaretPosition(0);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
