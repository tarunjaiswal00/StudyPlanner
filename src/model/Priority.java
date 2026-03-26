package model;

/**
 * Enum representing the priority level of a task.
 * Demonstrates: Enums with fields, constructors, and methods.
 */
public enum Priority {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3);

    private final String label;
    private final int level; // used for sorting

    Priority(String label, int level) {
        this.label = label;
        this.level = level;
    }

    public String getLabel() { return label; }
    public int getLevel()    { return level; }

    @Override
    public String toString() { return label; }
}
