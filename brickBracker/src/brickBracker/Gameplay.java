/**
 * Gameplay.java
 * -------------
 * Core game panel: handles all rendering, physics, collision detection,
 * input processing, and game-state management.
 *
 * Extends JPanel so it can be embedded directly in the JFrame.
 * Implements:
 *   KeyListener    – arrow keys move the paddle; ENTER starts/restarts/advances level
 *   ActionListener – javax.swing.Timer fires every BASE_DELAY ms to drive the game loop
 *
 * Game loop overview (actionPerformed fires ~125 times/second at delay=8 ms):
 *   1. Move the ball by (ballXdir * speed, ballYdir * speed) pixels.
 *   2. Check wall / paddle / brick collisions and reverse direction as needed.
 *   3. Detect ball-out-of-bounds (life lost) and win condition (all bricks gone).
 *   4. Call repaint() → triggers paintComponent() on the EDT.
 *
 * Coordinate system (all values in pixels, origin top-left):
 *   Canvas   : 692 × 592 px (inside the 700 × 600 JFrame with borders)
 *   Bricks   : 80 ≤ x ≤ 620,  50 ≤ y ≤ 200
 *   Paddle   : y = 550, height = 8 px, width = 100 px
 *   Ball     : 20 × 20 px oval
 */
package brickBracker;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Gameplay extends JPanel implements KeyListener, ActionListener {

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Timer interval in milliseconds (~125 FPS). */
    private static final int BASE_DELAY = 8;

    /** Starting number of brick rows on level 1. Grows by 1 each level. */
    private static final int BASE_ROWS  = 3;

    /** Starting number of brick columns on level 1. Grows by 1 each level. */
    private static final int BASE_COLS  = 7;

    /** Number of lives the player starts with. */
    private static final int MAX_LIVES  = 3;

    // -----------------------------------------------------------------------
    // Game state
    // -----------------------------------------------------------------------

    /** True while the ball is in motion; false on death, win, or before first move. */
    private boolean play      = false;

    /** Cumulative score across all levels. */
    private int score         = 0;

    /** Current level (starts at 1, increments on each level clear). */
    private int level         = 1;

    /** Remaining lives. Game over when this reaches 0. */
    private int lives         = MAX_LIVES;

    /** Number of bricks still alive in the current level. */
    private int totalBricks;

    // -----------------------------------------------------------------------
    // Timing
    // -----------------------------------------------------------------------

    /** Swing timer that drives the game loop at BASE_DELAY ms per tick. */
    private Timer timer;

    // -----------------------------------------------------------------------
    // Entities
    // -----------------------------------------------------------------------

    /** X position of the left edge of the paddle. */
    private int playerX   = 310;

    /** X position of the left edge of the ball bounding box. */
    private int ballposX  = 120;

    /** Y position of the top edge of the ball bounding box. */
    private int ballposY  = 350;

    /**
     * Horizontal direction multiplier for the ball (+1 = right, -1 = left).
     * Multiplied by speed each tick.
     */
    private int ballXdir  = -1;

    /**
     * Vertical direction multiplier for the ball (+1 = down, -1 = up).
     * Multiplied by speed each tick.
     */
    private int ballYdir  = -2;

    /** Current brick grid for the active level. */
    private MapGenerator map;

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Initialises the game panel for level 1 and starts the game timer.
     * The panel is marked focusable so it receives keyboard events directly.
     */
    public Gameplay() {
        initLevel();                         // build the brick grid for level 1
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false); // prevent TAB stealing focus
        timer = new Timer(BASE_DELAY, this);
        timer.start();
    }

    // -----------------------------------------------------------------------
    // Level management
    // -----------------------------------------------------------------------

    /**
     * Creates a new MapGenerator sized for the current level.
     * Grid grows by one row and one column per level, making each
     * successive level harder (more bricks, denser layout).
     */
    private void initLevel() {
        int rows    = BASE_ROWS + (level - 1);
        int cols    = BASE_COLS + (level - 1);
        map         = new MapGenerator(rows, cols);
        totalBricks = rows * cols;           // track how many bricks remain
    }

    /**
     * Returns the ball's movement speed (pixels per tick) for the current level.
     * Speed increases by 1 each level so higher levels feel faster.
     */
    private int speedForLevel() {
        return 1 + level;
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    /**
     * Paints the entire game frame.
     * Using paintComponent() (not paint()) is the correct Swing override:
     * it cooperates with double-buffering, preventing flicker.
     * super.paintComponent() clears the background before we draw.
     *
     * Draw order (painter's algorithm – back to front):
     *   1. Dark background fill
     *   2. Left / top / right border strips
     *   3. Brick grid (via MapGenerator.draw)
     *   4. HUD (score, level, lives)
     *   5. Paddle
     *   6. Ball
     *   7. Overlay messages (start screen, level-clear, game-over)
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // let Swing clear the panel first

        Graphics2D g2 = (Graphics2D) g;

        // Smooth edges on the ball and rounded paddle corners
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- 1. Background ---
        g2.setColor(new Color(30, 30, 40));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // --- 2. Borders (left, top, right walls; no bottom wall — ball can fall through) ---
        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRect(0,   0, 3,   592); // left wall
        g2.fillRect(0,   0, 692, 3);   // top wall
        g2.fillRect(689, 0, 3,   592); // right wall

        // --- 3. Bricks ---
        map.draw(g2);

        // --- 4. HUD ---
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("Score: " + score, 20, 30);   // top-left: cumulative score
        g2.drawString("Level: " + level, 310, 30);  // centre: current level
        drawLives(g2);                               // top-right: life circles

        // --- 5. Paddle ---
        g2.setColor(Color.CYAN);
        // Rounded rectangle gives a softer look than a plain rect
        g2.fillRoundRect(playerX, 550, 100, 8, 8, 8);

        // --- 6. Ball ---
        g2.setColor(Color.YELLOW);
        g2.fillOval(ballposX, ballposY, 20, 20);

        // --- 7. Overlay messages ---

        // Start screen shown on very first launch (play=false, full lives, no score)
        if (!play && lives > 0 && totalBricks > 0 && score == 0 && level == 1) {
            drawCenteredMessage(g2,
                "BRICK BREAKER",
                "Press ENTER to Start",
                new Color(0, 200, 255));
        }

        // All bricks destroyed → level cleared
        if (totalBricks <= 0) {
            play     = false;
            ballXdir = 0;
            ballYdir = 0;
            drawCenteredMessage(g2,
                "Level " + level + " Complete!",
                "Press ENTER for Next Level",
                new Color(100, 255, 100));
        }

        // Ball below the bottom edge AND no lives remain → game over
        if (ballposY > 570 && lives <= 0) {
            play     = false;
            ballXdir = 0;
            ballYdir = 0;
            drawCenteredMessage(g2,
                "Game Over!  Score: " + score,
                "Press ENTER to Restart",
                Color.RED);
        }
    }

    /**
     * Draws the remaining-lives indicator in the top-right corner.
     * Each life is represented by a small filled circle.
     *
     * @param g  Graphics2D context
     */
    private void drawLives(Graphics2D g) {
        g.setColor(Color.PINK);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString("Lives:", 580, 30);

        // Draw one circle per remaining life, spaced 18 px apart
        for (int i = 0; i < lives; i++) {
            g.fillOval(645 + i * 18, 15, 12, 12);
        }
    }

    /**
     * Renders a semi-transparent overlay box with a large title and
     * a smaller subtitle centred in the game area.
     * Used for the start screen, level-clear, and game-over states.
     *
     * @param g      Graphics2D context
     * @param title  Large primary text
     * @param sub    Smaller instruction text below the title
     * @param color  Colour used for the title text
     */
    private void drawCenteredMessage(Graphics2D g, String title, String sub, Color color) {
        // Semi-transparent dark background box so text is legible over bricks
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(100, 260, 490, 110);

        // Title text (large, coloured)
        g.setColor(color);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (692 - tw) / 2, 305); // horizontally centred

        // Subtitle text (smaller, white)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        int sw = g.getFontMetrics().stringWidth(sub);
        g.drawString(sub, (692 - sw) / 2, 345);
    }

    // -----------------------------------------------------------------------
    // Game loop (Timer callback)
    // -----------------------------------------------------------------------

    /**
     * Called by the Swing Timer every BASE_DELAY ms.
     * Drives all physics and state transitions when the game is active.
     *
     * Steps performed each tick:
     *   1. Paddle collision check  – reverse ball's Y direction if it hits the paddle.
     *   2. Brick collision check   – destroy the first brick the ball overlaps; reverse
     *                                 ball direction based on which face was hit.
     *   3. Move the ball           – add direction × speed to position.
     *   4. Wall collision check    – bounce off left, top, and right walls.
     *   5. Fall detection          – deduct a life; respawn or trigger game-over.
     *   6. repaint()               – queue a fresh frame.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (play) {
            int speed = speedForLevel();

            // -------------------------------------------------------------------
            // 1. Paddle collision
            // Using Math.abs ensures the ball always bounces upward off the
            // paddle, preventing a corner-case where the ball could pass through
            // if it entered from below (e.g., after a very fast bounce).
            // -------------------------------------------------------------------
            if (new Rectangle(ballposX, ballposY, 20, 20)
                    .intersects(new Rectangle(playerX, 550, 100, 8))) {
                ballYdir = -Math.abs(ballYdir); // always send ball upward
            }

            // -------------------------------------------------------------------
            // 2. Brick collision
            // Labelled break (A:) exits both loops as soon as one brick is hit,
            // preventing the ball from destroying multiple bricks in a single tick.
            // Direction reversal logic:
            //   - If the ball's right edge is at or before the brick's left edge,
            //     OR the ball's left edge is at or after the brick's right edge,
            //     the ball hit a vertical face → reverse X direction.
            //   - Otherwise the ball hit a horizontal face → reverse Y direction.
            // -------------------------------------------------------------------
            A: for (int i = 0; i < map.map.length; i++) {
                for (int j = 0; j < map.map[0].length; j++) {
                    if (map.map[i][j] > 0) {
                        // Compute this brick's pixel bounding box
                        int bx = j * map.brickWidth  + 80;
                        int by = i * map.brickHeight + 50;
                        Rectangle brickRect = new Rectangle(bx, by, map.brickWidth, map.brickHeight);
                        Rectangle ballRect  = new Rectangle(ballposX, ballposY, 20, 20);

                        if (ballRect.intersects(brickRect)) {
                            map.setBrickValue(0, i, j); // destroy the brick
                            totalBricks--;
                            score += 5 * level;         // higher levels score more

                            // Determine which face was hit to reverse the correct axis
                            if (ballposX + 19 <= brickRect.x ||
                                ballposX + 1  >= brickRect.x + brickRect.width) {
                                ballXdir = -ballXdir; // hit left or right face
                            } else {
                                ballYdir = -ballYdir; // hit top or bottom face
                            }

                            break A; // stop after the first brick hit this tick
                        }
                    }
                }
            }

            // -------------------------------------------------------------------
            // 3. Move ball
            // -------------------------------------------------------------------
            ballposX += ballXdir * speed;
            ballposY += ballYdir * speed;

            // -------------------------------------------------------------------
            // 4. Wall collisions
            // Math.abs forces the direction away from the wall, which prevents
            // the ball from tunnelling through if speed > 1.
            // Left wall (x < 3) and right wall (x > 669) bounce X.
            // Top wall (y < 3) bounces Y.  There is intentionally no bottom wall.
            // -------------------------------------------------------------------
            if (ballposX < 3)   ballXdir =  Math.abs(ballXdir); // left wall
            if (ballposY < 3)   ballYdir =  Math.abs(ballYdir); // top wall
            if (ballposX > 669) ballXdir = -Math.abs(ballXdir); // right wall

            // -------------------------------------------------------------------
            // 5. Fall detection (ball exits below the bottom of the canvas)
            // -------------------------------------------------------------------
            if (ballposY > 570) {
                lives--;
                if (lives > 0) {
                    // Player still has lives — respawn ball and paddle at start positions
                    ballposX = 120;
                    ballposY = 350;
                    ballXdir = -1;
                    ballYdir = -(1 + level); // speed matches current level
                    playerX  = 310;
                    play     = false;        // pause until player presses a key
                } else {
                    // No lives left — freeze ball and show game-over overlay
                    play     = false;
                    ballXdir = 0;
                    ballYdir = 0;
                }
            }
        }

        // Always repaint, even when paused, so overlays are drawn correctly
        repaint();
    }

    // -----------------------------------------------------------------------
    // Input handling
    // -----------------------------------------------------------------------

    /**
     * Handles key-press events for paddle movement and game control.
     *
     * RIGHT arrow  : move paddle 20 px to the right (capped at x=590)
     * LEFT arrow   : move paddle 20 px to the left  (capped at x=10)
     * Moving the paddle while paused (after a life is lost) resumes play,
     * giving the player a natural way to restart the ball after a death.
     *
     * ENTER key behaviour depends on current game state:
     *   - totalBricks == 0           : level just cleared → advance to next level
     *   - lives == 0 (game over)     : full restart (level 1, score 0, 3 lives)
     *   - otherwise (!play)          : start / resume the current level
     */
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            playerX = Math.min(playerX + 20, 590); // clamp to right boundary
            // Moving the paddle also resumes the ball after a life is lost
            if (!play && lives > 0 && totalBricks > 0) play = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            playerX = Math.max(playerX - 20, 10);  // clamp to left boundary
            if (!play && lives > 0 && totalBricks > 0) play = true;
        }

        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (!play) {
                if (totalBricks <= 0) {
                    // Advance to the next level
                    level++;
                    initLevel();
                } else if (lives <= 0) {
                    // Full restart from scratch
                    level  = 1;
                    score  = 0;
                    lives  = MAX_LIVES;
                    initLevel();
                }
                // Reset ball and paddle to starting positions
                ballposX = 120;
                ballposY = 350;
                ballXdir = -1;
                ballYdir = -(1 + level); // speed scales with new level
                playerX  = 310;
                play     = true;
            }
        }
    }

    /** Not used but required by the KeyListener interface. */
    @Override public void keyReleased(KeyEvent e) {}

    /** Not used but required by the KeyListener interface. */
    @Override public void keyTyped(KeyEvent e)    {}
}
