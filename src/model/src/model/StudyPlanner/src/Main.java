import gui.MainWindow;
import service.StudyPlanner;

import javax.swing.*;

/**
 * Application entry point.
 * Demonstrates: SwingUtilities.invokeLater for safe GUI initialisation
 *               on the Event Dispatch Thread.
 */
public class Main {
    public static void main(String[] args) {
        StudyPlanner planner = new StudyPlanner();
        planner.start();   // load saved data + start reminder thread

        // All Swing work must happen on the EDT
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fall back to default Swing look-and-feel
            }
            MainWindow window = new MainWindow(planner);
            window.setVisible(true);
        });
    }
}
