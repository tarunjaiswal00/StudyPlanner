# Student Study Planner

A desktop Java application that helps students manage subjects, track tasks with deadlines, time study sessions, and get automatic reminders for overdue work.

---

## Features

- **Subject management** — add and remove the subjects you are studying
- **Task tracker** — add tasks with deadlines, priority levels (Low / Medium / High), and optional notes; mark tasks done, see overdue tasks highlighted
- **Study timer** — start a timed session for any subject; the elapsed time is displayed live; notes can be added when you stop
- **Automatic reminders** — a background thread checks every 30 minutes and shows a pop-up for any overdue or due-today tasks
- **Stats dashboard** — see completion percentage per subject, total study minutes logged, and recent sessions
- **Persistent storage** — all data is saved automatically to plain text files in a `data/` folder next to the application; no database required

---

## Requirements

- Java 17 or later (JDK for compiling, JRE for running)
- Any OS with a desktop environment (Windows / macOS / Linux)

---

## Project Structure

```
StudyPlanner/
├── src/
│   ├── Main.java                   ← Entry point
│   ├── model/
│   │   ├── Priority.java           ← Enum (Low / Medium / High)
│   │   ├── Task.java               ← Task with deadline, priority, completion
│   │   ├── Subject.java            ← Subject containing a list of Tasks
│   │   └── StudySession.java       ← Timed study session record
│   ├── service/
│   │   ├── DataManager.java        ← File I/O (read/write subjects & sessions)
│   │   ├── ReminderService.java    ← Background reminder thread
│   │   └── StudyPlanner.java       ← Central controller / facade
│   └── gui/
│       └── MainWindow.java         ← Swing GUI (tabs, table, timer)
└── data/                           ← Auto-created; stores subjects.txt, sessions.txt
```

---

## How to Use

### Adding subjects
1. Open the **Subjects** tab.
2. Type a subject name and click **Add Subject** (or press Enter).
3. Subjects appear in the list with their current completion percentage.

### Managing tasks
1. Open the **Tasks** tab.
2. Select a subject from the drop-down.
3. Fill in the title, deadline (`dd-MM-yyyy`), priority, and optional notes.
4. Click **Add Task**. Tasks are sorted automatically by deadline then priority.
5. Select a row and click **Toggle Done** to mark a task complete/incomplete.
6. The **Overdue** column is checked automatically for past-due pending tasks.

### Timing a study session
1. Open the **Study Timer** tab.
2. Choose a subject and click **Start Session**.
3. The clock counts up in real time.
4. Click **Stop Session** to finish; you can add notes before saving.

### Viewing stats
Open the **Stats** tab (or click **Refresh**) to see completion percentages, study minutes per subject, and your last 5 sessions.

---

## Data Storage Format

Data is saved in human-readable plain text files inside `data/`.

**subjects.txt** — one subject per block, tasks indented with `TASK:`:
```
SUBJECT:Mathematics|#4A90E2
TASK:Chapter 5 exercises|Practice problems|15-04-2026|HIGH|false
TASK:Past papers|2023 and 2024|20-04-2026|MEDIUM|false
SUBJECT:Physics|#E24A4A
```

**sessions.txt** — one completed session per line:
```
Mathematics|25-03-2026 14:00|25-03-2026 15:30|Covered integration
```

---

## Java Concepts Demonstrated

| Concept | Where |
|---|---|
| OOP (classes, encapsulation, constructors) | All model classes |
| Enums with fields and methods | `Priority.java` |
| `Comparable` interface | `Task.java` |
| `ArrayList`, `HashMap`, Streams | `Subject.java`, `StudyPlanner.java` |
| Generics | `List<Task>`, `Map<String, Long>` |
| File I/O (`BufferedReader`, `BufferedWriter`, try-with-resources) | `DataManager.java` |
| Exception handling (propagation + selective catch) | `DataManager.java` |
| Multithreading (`ScheduledExecutorService`, daemon thread) | `ReminderService.java` |
| Thread-safe GUI updates (`SwingUtilities.invokeLater`) | `ReminderService.java`, `Main.java` |
| Swing (`JFrame`, `JTabbedPane`, `JTable`, `JList`, `Timer`) | `MainWindow.java` |
