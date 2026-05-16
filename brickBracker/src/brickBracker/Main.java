/**
 * Main.java
 * ---------
 * Entry point for the Brick Breaker game.
 *
 * Responsibilities:
 *   - Create the top-level JFrame window.
 *   - Instantiate the Gameplay panel and attach it to the frame.
 *   - Configure window properties (size, title, close behaviour).
 *
 * Window is fixed at 700 × 600 px and is not resizable so that
 * the brick grid and paddle coordinates stay accurate.
 * setVisible() is called AFTER add(gamePlay) to ensure the panel
 * is fully initialised before the window appears on screen.
 */
package brickBracker;

import javax.swing.JFrame;

public class Main {

    public static void main(String[] args) {
        // Create the application window
        JFrame obj = new JFrame();

        // Create the game panel (handles all drawing and logic)
        Gameplay gamePlay = new Gameplay();

        // Position the window at (10, 10) on the screen, 700 px wide × 600 px tall
        obj.setBounds(10, 10, 700, 600);

        obj.setTitle("Brick Breaker");

        // Prevent resizing — keeps brick/paddle coordinates consistent
        obj.setResizable(false);

        // Terminate the JVM when the window is closed
        obj.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Add the game panel to the frame before making it visible
        obj.add(gamePlay);

        // Show the window (must come after add() so layout is complete)
        obj.setVisible(true);
    }
}
