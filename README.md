# Brick Breaker

A classic Brick Breaker game built with Java Swing. Destroy all bricks using a bouncing ball and a paddle. Clear levels to progress through increasingly challenging grids.

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Project Structure](#project-structure)
- [How to Run](#how-to-run)
- [Controls](#controls)
- [Gameplay](#gameplay)
- [Scoring](#scoring)
- [Architecture](#architecture)
  - [Main.java](#mainjava)
  - [Gameplay.java](#gameplayjava)
  - [MapGenerator.java](#mapgeneratorjava)
- [Technical Details](#technical-details)
- [Game States](#game-states)

---

## Features

- Multi-level progression — the brick grid grows larger each level
- Lives system — 3 lives per game; ball respawns after each death
- Score multiplier — each brick is worth more on higher levels
- On-screen HUD — live display of score, current level, and remaining lives
- Anti-aliased rendering — smooth ball and rounded paddle corners
- Colorful brick rows — six distinct row colors cycling across the grid
- Overlay screens — start, level-clear, and game-over messages

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java JDK    | 8 or later |
| Java Swing  | Included in JDK (no extra dependencies) |

---

## Project Structure

```
Games-BrickBracker/
└── brickBracker/
    ├── src/
    │   └── brickBracker/
    │       ├── Main.java          # Entry point — creates the JFrame window
    │       ├── Gameplay.java      # Core game loop, rendering, and input
    │       └── MapGenerator.java  # Brick grid data model and renderer
    ├── bin/
    │   └── brickBracker/
    │       ├── Main.class
    │       ├── Gameplay.class
    │       └── MapGenerator.class
    ├── .classpath
    └── .project
```

---

## How to Run

### Using Eclipse

1. Clone the repository:
   ```bash
   git clone https://github.com/ashrafjaouadi/Games-BrickBracker.git
   ```
2. Open Eclipse and go to **File → Import → Existing Projects into Workspace**.
3. Select the `brickBracker/` folder.
4. Run `Main.java` as a Java Application.

### Using the Command Line

```bash
# Compile
javac -d brickBracker/bin brickBracker/src/brickBracker/*.java

# Run
java -cp brickBracker/bin brickBracker.Main
```

---

## Controls

| Key         | Action                                              |
|-------------|-----------------------------------------------------|
| `→` Right   | Move paddle right (20 px per press; max x = 590)    |
| `←` Left    | Move paddle left  (20 px per press; min x = 10)     |
| `Enter`     | Start game / advance to next level / restart after game over |

Moving the paddle with arrow keys also resumes the ball after a life is lost, giving a natural way to restart without pressing Enter.

---

## Gameplay

1. **Start** — Press `Enter` or move the paddle to launch the ball.
2. **Break bricks** — The ball bounces off walls, the paddle, and bricks. Each brick it hits is destroyed.
3. **Don't let the ball fall** — If the ball passes below the paddle, you lose a life. The ball and paddle reset to their starting positions.
4. **Clear the level** — Destroy every brick to complete the level. Press `Enter` to advance.
5. **Game Over** — Losing all 3 lives ends the game. Press `Enter` to restart from level 1.

---

## Scoring

| Action              | Points                      |
|---------------------|-----------------------------|
| Destroy one brick   | `5 × current level`         |

Score accumulates across all levels and is shown in the top-left corner of the screen throughout the session.

**Examples:**
- Level 1 brick → 5 points
- Level 2 brick → 10 points
- Level 3 brick → 15 points

---

## Architecture

### Main.java

The application entry point. Creates the `JFrame` window (700 × 600 px, non-resizable), instantiates the `Gameplay` panel, and attaches it to the frame. `setVisible()` is called after `add()` to ensure the panel is fully laid out before the window appears.

### Gameplay.java

The core of the game. Extends `JPanel` and implements:

- **`KeyListener`** — handles arrow keys (paddle movement) and Enter (state transitions).
- **`ActionListener`** — receives ticks from a `javax.swing.Timer` every 8 ms (~125 FPS) to drive the game loop.

**Game loop steps (per tick):**

```
1. Paddle collision  → reverse ball's Y direction (Math.abs ensures upward bounce)
2. Brick collision   → destroy brick, update score, reverse ball axis based on face hit
3. Move ball         → ballposX += ballXdir * speed,  ballposY += ballYdir * speed
4. Wall collision    → bounce off left, top, and right walls; no bottom wall
5. Fall detection    → deduct a life; respawn or trigger game-over
6. repaint()         → queue a fresh frame on the Event Dispatch Thread
```

**Level progression:**

| Level | Rows | Cols | Bricks | Speed (px/tick) |
|-------|------|------|--------|-----------------|
| 1     | 3    | 7    | 21     | 2               |
| 2     | 4    | 8    | 32     | 3               |
| 3     | 5    | 9    | 45     | 4               |
| N     | N+2  | N+6  | (N+2)×(N+6) | N+1       |

**Rendering order (painter's algorithm — back to front):**

1. Dark background fill `(30, 30, 40)`
2. Left / top / right border strips (3 px, light gray)
3. Brick grid via `MapGenerator.draw()`
4. HUD — score (top-left), level (top-center), lives (top-right)
5. Paddle — cyan rounded rectangle at y = 550
6. Ball — yellow oval (20 × 20 px)
7. Overlay message (start screen / level-clear / game-over)

**Key constants:**

| Constant     | Value | Description                          |
|--------------|-------|--------------------------------------|
| `BASE_DELAY` | 8 ms  | Timer interval (~125 FPS)            |
| `BASE_ROWS`  | 3     | Brick rows on level 1                |
| `BASE_COLS`  | 7     | Brick columns on level 1             |
| `MAX_LIVES`  | 3     | Starting lives per game              |

### MapGenerator.java

Manages the brick grid for the current level.

**Data model:**
```
map[row][col] == 1  →  brick alive   (drawn and collidable)
map[row][col] == 0  →  brick destroyed (invisible, skipped)
```

**Layout:** The entire grid is drawn within a fixed 540 × 150 px area starting at canvas position (80, 50). Brick dimensions are computed as:
```
brickWidth  = 540 / cols
brickHeight = 150 / rows
```

**Row colors** (cycle with modulo for grids deeper than 6 rows):

| Row index (mod 6) | Color  |
|-------------------|--------|
| 0                 | Red    |
| 1                 | Orange |
| 2                 | Yellow |
| 3                 | Green  |
| 4                 | Blue   |
| 5                 | Purple |

---

## Technical Details

**Canvas coordinate system** (origin at top-left):

| Element | Position                          |
|---------|-----------------------------------|
| Canvas  | 692 × 592 px (inside 700 × 600 frame) |
| Bricks  | x: 80 – 620, y: 50 – 200         |
| Paddle  | y = 550, width = 100 px, height = 8 px |
| Ball    | 20 × 20 px oval                   |

**Collision detection** uses `java.awt.Rectangle.intersects()` for both paddle and brick checks. The ball-to-brick face detection logic:

- If the ball's right edge ≤ brick's left edge **OR** ball's left edge ≥ brick's right edge → side face hit → reverse X direction.
- Otherwise → top/bottom face hit → reverse Y direction.

Only one brick is destroyed per tick (labeled `break A:` exits both loops on first hit), preventing the ball from tunneling through multiple bricks simultaneously.

**Wall bounce** uses `Math.abs()` to force direction away from the wall, which prevents the ball from tunneling through at high speeds:

```java
if (ballposX < 3)   ballXdir =  Math.abs(ballXdir); // left wall
if (ballposY < 3)   ballYdir =  Math.abs(ballYdir); // top wall
if (ballposX > 669) ballXdir = -Math.abs(ballXdir); // right wall
```

---

## Game States

```
                    ┌─────────────────┐
                    │   START SCREEN  │  (press Enter or move paddle)
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
              ┌────►│    PLAYING      │◄────────────────────┐
              │     └────────┬────────┘                     │
              │              │                              │
              │    ┌─────────┴─────────┐                   │
              │    │                   │                   │
              │  ball falls         all bricks             │
              │  (lives > 0)         destroyed             │
              │    │                   │                   │
              │    ▼                   ▼                   │
              │  RESPAWN        ┌──────────────┐           │
              └────────────────►│ LEVEL CLEAR  │           │
                                └──────┬───────┘           │
                                       │ press Enter        │
                                       └───────────────────►│ (level++)
              ball falls                                    
              (lives == 0)                                  
                    │                                       
                    ▼                                       
           ┌─────────────────┐                             
           │    GAME OVER    │ ──── press Enter ──── (full restart)
           └─────────────────┘
```

---

## Repository

[https://github.com/ashrafjaouadi/Games-BrickBracker](https://github.com/ashrafjaouadi/Games-BrickBracker)
