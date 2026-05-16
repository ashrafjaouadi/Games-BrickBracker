/**
 * MapGenerator.java
 * -----------------
 * Manages the brick grid that the player must destroy.
 *
 * Data model:
 *   map[row][col] == 1  → brick is alive (drawn and collidable)
 *   map[row][col] == 0  → brick has been destroyed (invisible / non-collidable)
 *
 * Layout:
 *   The grid is drawn inside a 540 × 150 px area that starts at
 *   x=80, y=50 on the game canvas.  Each brick's pixel dimensions
 *   are computed by dividing that area evenly across the requested
 *   number of columns / rows, so the grid always fills the same
 *   screen region regardless of level size.
 *
 * Colours:
 *   ROW_COLORS cycles through six distinct colours so every row
 *   is visually distinct.  The index wraps with modulo so grids
 *   with more than six rows still get a colour assigned.
 */
package brickBracker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class MapGenerator {

    // 2-D grid: 1 = brick present, 0 = brick destroyed
    public int map[][];

    // Pixel dimensions of a single brick (calculated from grid size)
    public int brickWidth;
    public int brickHeight;

    /**
     * Six colours used to tint each row of bricks.
     * Index is (rowIndex % ROW_COLORS.length), so it wraps on deep grids.
     */
    private static final Color[] ROW_COLORS = {
        new Color(220, 50,  50),   // row 0 – red
        new Color(220, 130, 40),   // row 1 – orange
        new Color(200, 200, 40),   // row 2 – yellow
        new Color(50,  180, 50),   // row 3 – green
        new Color(50,  130, 220),  // row 4 – blue
        new Color(150, 50,  220),  // row 5 – purple
    };

    /**
     * Constructs a new brick grid and pre-fills every cell with 1 (alive).
     *
     * @param row  Number of brick rows
     * @param col  Number of brick columns
     */
    public MapGenerator(int row, int col) {
        map = new int[row][col];

        // Mark every brick as alive
        for (int i = 0; i < row; i++)
            for (int j = 0; j < col; j++)
                map[i][j] = 1;

        // Divide the 540 × 150 px area evenly across the grid
        brickWidth  = 540 / col;
        brickHeight = 150 / row;
    }

    /**
     * Renders all living bricks to the screen.
     * Each brick gets a filled rectangle in its row colour with a
     * black border drawn on top to create a grid effect.
     *
     * @param g  Graphics2D context provided by the Gameplay panel
     */
    public void draw(Graphics2D g) {
        for (int i = 0; i < map.length; i++) {
            // Determine the colour for this entire row
            Color rowColor = ROW_COLORS[i % ROW_COLORS.length];

            for (int j = 0; j < map[0].length; j++) {
                if (map[i][j] > 0) {  // only draw bricks that are still alive
                    // Pixel top-left corner of this brick
                    int x = j * brickWidth  + 80;
                    int y = i * brickHeight + 50;

                    // Filled body of the brick
                    g.setColor(rowColor);
                    g.fillRect(x, y, brickWidth, brickHeight);

                    // Black border to separate bricks visually
                    g.setStroke(new BasicStroke(2));
                    g.setColor(Color.BLACK);
                    g.drawRect(x, y, brickWidth, brickHeight);
                }
            }
        }
    }

    /**
     * Sets the state of a specific brick cell.
     * Called by Gameplay when the ball hits a brick (value → 0 to destroy it).
     *
     * @param value  New state (0 = destroyed, 1 = alive)
     * @param row    Row index of the target brick
     * @param col    Column index of the target brick
     */
    public void setBrickValue(int value, int row, int col) {
        map[row][col] = value;
    }
}
