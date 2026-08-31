// Fabrizzio Capocci
// Assignment 8
// COP3330

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Comets - A variation of the classic Asteroids game using Java Swing.
 */
@SuppressWarnings("serial")
public class Comets extends JFrame {
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 600;

    public Comets() {
        setTitle("Comets");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        add(gamePanel);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Start the game loop thread
        gamePanel.startGame();
    }

    public static void main(String[] args) {
        // Run the game on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(Comets::new);
    }
}

/**
 * Base class representing the basic state of any object in the game.
 */
class GameObject {
    private double x, y, dx, dy, rotation;

    public GameObject(double x, double y, double dx, double dy, double rotation) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.rotation = rotation;
    }

    // Accessor methods
    public double getX() { return x; }
    public double getY() { return y; }
    public double getDx() { return dx; }
    public double getDy() { return dy; }
    public double getRotation() { return rotation; }

    // Setters
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setDx(double dx) { this.dx = dx; }
    public void setDy(double dy) { this.dy = dy; }
    public void setRotation(double rotation) { this.rotation = rotation; }

    /**
     * Updates the existing GameObject's position based on its velocity and applies
     * screen wrapping.
     * @param width The game width for screen wrapping.
     * @param height The game height for screen wrapping.
     */
    public void move(int width, int height) {
        x += dx;
        y += dy;
        // Screen wrapping logic. if x/y are < 0, add the width/height to
        //x/y respectively. If x/y > width/height, subtract the width/height respectively.
        if (x < 0) {
            x += width;
        } else if (x > width) {
            x -= width;
        }
        if (y < 0) {
            y += height;
        } else if (y > height) {
            y -= height;
        }
    }
}

/**
 * Represents the player's ship.
 */
class Ship extends GameObject {
    private final double thrust = 0.15;
    private final double friction = 0.99;
    private final double maxSpeed = 8.0;
    private final double turnRate = Math.toRadians(5);
    private final long FIRE_COOLDOWN = 200; // ms
    private long lastFireTime;

    public Ship(double x, double y) {
        super(x, y, 0, 0, 0); // Initial state
        this.lastFireTime = 0;
    }

    /**
     * Updates the ship's movement, rotation, and velocity
     */
    public void update(int width, int height, boolean isThrusting, boolean turnLeft, boolean turnRight) {
        // Implement ship's movement physics.
        double newDx = getDx();
        double newDy = getDy();
        double newRotation = getRotation();

        // Rotation
        if (turnLeft) {
            newRotation -= turnRate;
        }
        if (turnRight) {
            newRotation += turnRate;
        }

        // Thrust. thrust in the new direction. (add thrust to newDx and
        // newDy) (x_component = thrust * Math.cos(newRotation), y_component = thrust *
        // Math.sin(newRotation))
        if (isThrusting) {
            newDx += thrust * Math.cos(newRotation);
            newDy += thrust * Math.sin(newRotation);
        }

        // Apply friction
        newDx *= friction;
        newDy *= friction;

        // Limit speed (speed is the length of the diagonal formed by the x/y
        // velocities). If the speed > maxSpeed, scale the speed in each direction based on
        // the ratio related to the current direction.
        double speed = Math.sqrt(newDx * newDx + newDy * newDy);
        if (speed > maxSpeed) {
            double scale = maxSpeed / speed;
            newDx *= scale;
            newDy *= scale;
        }

        // Update velocity and rotation
        setDx(newDx);
        setDy(newDy);
        setRotation(newRotation);

        // Update position (with wrapping)
        move(width, height);
    }

    /**
     * Attempts to fire a bullet. Updates the ship's lastFireTime upon successful
     * fire.
     * @param isFiring True if the fire key is currently pressed.
     * @return The new Bullet object, or null if on cooldown.
     */
    public Bullet fire(boolean isFiring) {
        long currentTime = System.currentTimeMillis();
        if (isFiring && currentTime - lastFireTime > FIRE_COOLDOWN) {
            // Firing successful: Update the lastFireTime
            lastFireTime = currentTime;
            double currentRotation = getRotation();
            double bulletSpeed = maxSpeed + 5;
            double bulletDx = bulletSpeed * Math.cos(currentRotation) + getDx();
            double bulletDy = bulletSpeed * Math.sin(currentRotation) + getDy();
            // Start bullet slightly ahead of the ship's nose
            double startX = getX() + 20 * Math.cos(currentRotation);
            double startY = getY() + 20 * Math.sin(currentRotation);
            // Bullet remains simple and is created once
            return new Bullet(startX, startY, bulletDx, bulletDy);
        }
        return null;
    }
}

/**
 * Represents a bullet fired by the ship.
 */
class Bullet extends GameObject {
    public static final int LIFESPAN = 120; // Ticks
    public static final int RADIUS = 2;

    // Ticks remaining before removal
    private int lifeRemaining;

    public Bullet(double x, double y, double dx, double dy) {
        super(x, y, dx, dy, 0);
        this.lifeRemaining = LIFESPAN;
    }

    public int getLifeRemaining() {
        return lifeRemaining;
    }

    /**
     * Update the Bullet's position and decrements its life counter.
     */
    public void update(int width, int height) {
        move(width, height);
        lifeRemaining--; // Update life remaining
    }

    public boolean isExpired() {
        return lifeRemaining <= 0;
    }
}

/**
 * Represents a cometobject.
 */
class Comet extends GameObject {
    public static final int LARGE = 3;
    public static final int MEDIUM = 2;
    public static final int SMALL = 1;
    public static final int RADIUS_LARGE = 40;
    public static final int RADIUS_MEDIUM = 25;
    public static final int RADIUS_SMALL = 15;

    // These fields remain final as their properties don't change during the game
    private final double rotationSpeed;
    private final int size;

    public Comet(double x, double y, double dx, double dy, double rotation, double rotationSpeed, int size) {
        super(x, y, dx, dy, rotation);
        this.rotationSpeed = rotationSpeed;
        this.size = size;
    }

    public double getRotationSpeed() {
        return rotationSpeed;
    }

    public int getSize() {
        return size;
    }

    public int getRadius() {
        return switch (size) {
            case LARGE -> RADIUS_LARGE;
            case MEDIUM -> RADIUS_MEDIUM;
            default -> RADIUS_SMALL;
        };
    }

    /**
     * Updates the Comet's rotation and position.
     */
    public void update(int width, int height) {
        // Update rotation
        setRotation(getRotation() + rotationSpeed);
        // Update position and apply wrapping
        move(width, height);
    }

    /**
     * Generates smaller comets when this one is destroyed.
     * @return A list of 2 new smaller comets, or an empty list if this is the smallest.
     */
    public List<Comet> shatter() {
        List<Comet> newComets = new ArrayList<>();
        int nextSize = size - 1;
        if (nextSize < SMALL) {
            return newComets;
        }

        Random rand = new Random();
        double baseSpeed = Math.sqrt(getDx() * getDx() + getDy() * getDy()) * 1.2;

        for (int i = 0; i < 2; i++) {
            double angle = rand.nextDouble() * Math.PI * 2;
            double newDx = baseSpeed * Math.cos(angle);
            double newDy = baseSpeed * Math.sin(angle);

            newComets.add(new Comet(
                    getX(), getY(),
                    newDx, newDy,
                    rand.nextDouble() * 2 * Math.PI,
                    (rand.nextDouble() * 0.05 - 0.025),
                    nextSize
            ));
        }
        return newComets;
    }
}

@SuppressWarnings("serial")
class GamePanel extends JPanel implements Runnable, KeyListener {

    private Ship ship;

    private final List<Comet> comets = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final Random rand = new Random();

    private Thread gameThread;
    private boolean isRunning = false;
    private int score = 0;
    private int lives = 3;
    private final int GAME_LOOP_DELAY = 1000 / 60; // ~60 FPS

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;

    private boolean downPressed = false;
    private boolean firingPressed = false;

    // High score
    private int highScore = 0;
    private final Path HIGH_SCORE_FILE = Paths.get("highscore.txt");

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        setBackground(Color.BLACK);
        initGame();
    }

    private void initGame() {
        ship = new Ship(Comets.GAME_WIDTH / 2.0, Comets.GAME_HEIGHT / 2.0);
        // Synchronize structural modifications
        synchronized (comets) {
            comets.clear();
        }
        synchronized (bullets) {
            bullets.clear();
        }
        score = 0;
        lives = 3;
        // Load high score from file
        loadHighScore();
        spawnInitialComets(5);
    }

    // Extra credit
    private void loadHighScore() {
        try {
            if (Files.exists(HIGH_SCORE_FILE)) {
                List<String> lines = Files.readAllLines(HIGH_SCORE_FILE, StandardCharsets.UTF_8);
                if (!lines.isEmpty()) {
                    try {
                        highScore = Integer.parseInt(lines.get(0).trim());
                    } catch (NumberFormatException nfe) {
                        highScore = 0;
                    }
                }
            } else {
                highScore = 0;
            }
        } catch (IOException e) {
            highScore = 0;
        }
    }

    private void saveHighScoreIfNeeded() {
        if (score > highScore) {
            highScore = score;
            try {
                Files.writeString(HIGH_SCORE_FILE, Integer.toString(highScore), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException ignored) {
            }
        }
    }

    private void spawnInitialComets(int count) {
        for (int i = 0; i < count; i++) {
            spawnRandomComet(Comet.LARGE);
        }
    }

    private void spawnRandomComet(int size) {
        double x, y;
        double minDistance = 150;
        double centerX = Comets.GAME_WIDTH / 2.0;
        double centerY = Comets.GAME_HEIGHT / 2.0;

        do {
            x = rand.nextDouble() * Comets.GAME_WIDTH;
            y = rand.nextDouble() * Comets.GAME_HEIGHT;
        } while (Point2D.distance(x, y, centerX, centerY) < minDistance);

        double angle = Math.atan2(centerY - y, centerX - x) + (rand.nextDouble() * Math.PI / 4 - Math.PI / 8);
        double speed = 1.0 + rand.nextDouble() * 2.0;
        synchronized (comets) {
            comets.add(new Comet(
                    x, y,
                    speed * Math.cos(angle), speed * Math.sin(angle),
                    rand.nextDouble() * 2 * Math.PI,
                    (rand.nextDouble() * 0.05 - 0.025),
                    size
            ));
        }
    }

    public void startGame() {
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double delta = 0;
        double ns = 1000000000.0 / 60.0; // Target 60 updates per second

        while (isRunning) {
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;

            while (delta >= 1) {
                updateGame();
                delta--;
            }

            repaint(); // Request repaint as fast as possible

            try {
                // Sleep to not hog the CPU completely
                long sleepTime = GAME_LOOP_DELAY - (System.nanoTime() - now) / 1000000;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void updateGame() {
        if (lives <= 0) {
            return;
        }

        // Update ship movement state
        ship.update(Comets.GAME_WIDTH, Comets.GAME_HEIGHT, upPressed, leftPressed, rightPressed);

        // Fire bullet if requested
        Bullet newBullet = ship.fire(firingPressed);
        if (newBullet != null) {
            synchronized (bullets) {
                bullets.add(newBullet);
            }
        }

        // Update bullets
        synchronized (bullets) {
            Iterator<Bullet> bulletIterator = bullets.iterator();
            while (bulletIterator.hasNext()) {
                Bullet b = bulletIterator.next();
                b.update(Comets.GAME_WIDTH, Comets.GAME_HEIGHT);
                if (b.isExpired()) {
                    bulletIterator.remove(); // Safe removal using Iterator
                }
            }
        }
        // Update comets
        synchronized (comets) {
            for (Comet comet : comets) {
                comet.update(Comets.GAME_WIDTH, Comets.GAME_HEIGHT);
            }
        }

        // Collision Detection
        handleCollisions();

        // Win condition - spawn new comets if the field is clear
        synchronized (comets) {
            if (comets.isEmpty() && lives > 0) {
                spawnInitialComets(5 + score / 500);
            }
        }
    }

    private void handleCollisions() {
        // Bullet-Comet collisions
        List<Comet> newComets = new ArrayList<>();

        // Synchronize over both lists for atomic collision check/structural change
        synchronized (bullets) {
            synchronized (comets) {
                Iterator<Bullet> bulletIterator = bullets.iterator();
                while (bulletIterator.hasNext()) {
                    Bullet b = bulletIterator.next();
                    boolean bulletRemoved = false;
                    Iterator<Comet> cometIterator = comets.iterator();
                    while (cometIterator.hasNext()) {
                        Comet c = cometIterator.next();
                        double dist = Point2D.distance(b.getX(), b.getY(), c.getX(), c.getY());
                        if (dist <= c.getRadius()) {
                            // Collision detected: remove both safely
                            cometIterator.remove();
                            bulletIterator.remove();
                            bulletRemoved = true;
                            score += 10 * c.getSize();
                            // Shatter into smaller comets
                            List<Comet> shards = c.shatter();
                            if (!shards.isEmpty()) {
                                newComets.addAll(shards);
                            }
                            break; // bullet is gone -> go to next bullet
                        }
                    }
                    if (bulletRemoved) {
                        // bulletIterator already advanced/removed; continue outer loop
                    }
                }
                // Add newly created comets (shards)
                if (!newComets.isEmpty()) {
                    comets.addAll(newComets);
                }
            }
        }

        // Ship-Comet collisions
        if (lives > 0) {
            synchronized (comets) {

                // Implement comet/ship collision
                Iterator<Comet> cometIterator = comets.iterator();
                boolean collisionOccurred = false;
                // Loop through all the bullets
                while (cometIterator.hasNext()) {
                    // Check if the bullet is within the radius of the comet using Point2D.distance
                    Comet c = cometIterator.next();
                    double dist = Point2D.distance(ship.getX(), ship.getY(), c.getX(), c.getY());
                    // buffer of a few pixels so collisions feel fair
                    if (dist <= c.getRadius() + 5) {
                        // collision
                        lives--;
                        collisionOccurred = true;
                        // If it is, remove the bullet, remove the comet, and increment the score by 10*the size of the comet.
                        if (lives > 0) {
                            // Reset ship to center and clear bullets
                            ship = new Ship(Comets.GAME_WIDTH / 2.0, Comets.GAME_HEIGHT / 2.0);
                            synchronized (bullets) {
                                bullets.clear();
                            }
                        } else {
                            // Game over - save high score if needed
                            saveHighScoreIfNeeded();
                        }
                        break; // Only allow one collision this update
                    }
                }
                if (collisionOccurred) {
                    // nothing else here for now
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        if (lives <= 0) {
            drawGameOver(g2d);
            return;
        }
        // Draw Comets
        drawComets(g2d);
        
        // Draw Ship
        drawShip(g2d);

        // Draw Bullets
        drawBullets(g2d);

        // Draw HUD
        drawHUD(g2d);
    }

    private void drawComets(Graphics2D g2d) {
        g2d.setColor(Color.LIGHT_GRAY);

        // Fix: Copy the list under synchronization to prevent CME
        List<Comet> cometsSnapshot;
        synchronized (comets) {
            cometsSnapshot = new ArrayList<>(comets);
        }

        for (Comet comet : cometsSnapshot) {
            AffineTransform originalTransform = g2d.getTransform();

            g2d.translate(comet.getX(), comet.getY());
            g2d.rotate(comet.getRotation());

            int radius = comet.getRadius();
            Path2D.Double cometShape = new Path2D.Double();
            int numPoints = 12;

            for (int i = 0; i < numPoints; i++) {
                double angle = i * 2 * Math.PI / numPoints;
                double r = radius;
                double px = r * Math.cos(angle);
                double py = r * Math.sin(angle);
                if (i == 0) {
                    cometShape.moveTo(px, py);
                } else {
                    cometShape.lineTo(px, py);
                }
            }
            cometShape.closePath();

            g2d.draw(cometShape);

            g2d.setTransform(originalTransform);
        }
    }

    private void drawShip(Graphics2D g2d) {
        AffineTransform originalTransform = g2d.getTransform();

        g2d.translate(ship.getX(), ship.getY());
        g2d.rotate(ship.getRotation());

        Path2D.Double shipShape = new Path2D.Double();
        shipShape.moveTo(15, 0);
        shipShape.lineTo(-10, 10);
        shipShape.lineTo(-5, 0);
        shipShape.lineTo(-10, -10);
        shipShape.closePath();

        g2d.setColor(Color.WHITE);
        g2d.draw(shipShape);

        if (upPressed) {
            g2d.setColor(Color.YELLOW);
            Path2D.Double flame = new Path2D.Double();
            Random rand = new Random();
            flame.moveTo(-10, 0);
            flame.lineTo(-15 - rand.nextInt(5), 5);
            flame.lineTo(-15 - rand.nextInt(5), -5);
            flame.closePath();
            g2d.fill(flame);
        }
        g2d.setTransform(originalTransform);
    }

    private void drawBullets(Graphics2D g2d) {
        g2d.setColor(Color.RED);

        List<Bullet> bulletsSnapshot;
        synchronized (bullets) {
            bulletsSnapshot = new ArrayList<>(bullets);
        }

        for (Bullet bullet : bulletsSnapshot) {
            g2d.fillOval((int) bullet.getX() - Bullet.RADIUS, (int) bullet.getY() - Bullet.RADIUS,
                    Bullet.RADIUS * 2, Bullet.RADIUS * 2);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("Score: " + score, 10, 25);
        g2d.drawString("Lives: " + lives, 10, 50);
        g2d.drawString("High Score: " + highScore, 10, 75);
        // Checking list size should also be synchronized
        synchronized (comets) {
            if (comets.isEmpty()) {
                g2d.setColor(Color.GREEN);
                g2d.drawString("WAVE COMPLETE!", Comets.GAME_WIDTH / 2 - 80,
                        Comets.GAME_HEIGHT / 2 - 50);
            }
        }
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String gameOver = "GAME OVER";
        int strWidth = g2d.getFontMetrics().stringWidth(gameOver);
        g2d.drawString(gameOver, (Comets.GAME_WIDTH - strWidth) / 2, Comets.GAME_HEIGHT / 2 - 30);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 24));
        String finalScore = "Final Score: " + score;
        int scoreWidth = g2d.getFontMetrics().stringWidth(finalScore);
        g2d.drawString(finalScore, (Comets.GAME_WIDTH - scoreWidth) / 2,
                Comets.GAME_HEIGHT / 2 + 20);

        String hs = "High Score: " + highScore;
        int hsWidth = g2d.getFontMetrics().stringWidth(hs);
        g2d.drawString(hs, (Comets.GAME_WIDTH - hsWidth) / 2,
                Comets.GAME_HEIGHT / 2 + 60);

        g2d.setFont(new Font("Arial", Font.PLAIN, 18));
        String restart = "Press R to Restart";
        int rWidth = g2d.getFontMetrics().stringWidth(restart);
        g2d.drawString(restart, (Comets.GAME_WIDTH - rWidth) / 2,
                Comets.GAME_HEIGHT / 2 + 100);
    }

    // --- KeyListener Implementation ---
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
            upPressed = true;
        }
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            leftPressed = true;
        }
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            rightPressed = true;
        }
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
            downPressed = true;
        }
        if (key == KeyEvent.VK_SPACE) {
            firingPressed = true;
        }
        if (key == KeyEvent.VK_R && lives <= 0) {
            initGame(); // Restart the game
        }
        // Extra credit: Hyperspace Jump on 'E' key
        if (key == KeyEvent.VK_E && lives > 0) {
            attemptHyperspaceJump();
        }
    }

    private void attemptHyperspaceJump() {
        // 10% chance to fail
        double chance = rand.nextDouble();
        if (chance < 0.10) {
            // Failure - lose a life, do not move
            lives--;
            if (lives <= 0) {
                saveHighScoreIfNeeded();
            } else {
                // clear bullets as penalty? the spec didn't require clearing on failure.
                synchronized (bullets) {
                    bullets.clear();
                }
            }
        } else {
            // Success - teleport to random location on screen
            double nx = rand.nextDouble() * Comets.GAME_WIDTH;
            double ny = rand.nextDouble() * Comets.GAME_HEIGHT;
            ship.setX(nx);
            ship.setY(ny);
            ship.setDx(0);
            ship.setDy(0);
            synchronized (bullets) {
                bullets.clear();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
            upPressed = false;
        }
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            leftPressed = false;
        }
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            rightPressed = false;
        }
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
            downPressed = false;
        }
        if (key == KeyEvent.VK_SPACE) {
            firingPressed = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
}
