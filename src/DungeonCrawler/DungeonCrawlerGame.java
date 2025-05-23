package DungeonCrawler;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DungeonCrawlerGame extends Application {
    private static final int TILE_SIZE = 32;
    private static final int MAP_WIDTH = 800;
    private static final int MAP_HEIGHT = 600;
    private static final int SIDEBAR_WIDTH = 300;
    private static final int MAX_LEVELS = 30;
    private static final int FIREBALL_RANGE = 3;
    private int playerGold = 0; // Track player's gold
    private int potionCount = 0; // Track number of health potions
    private int killCounter = 0; // Track kills toward potion reward
    private List<double[]> fireballTrailPositions = new ArrayList<>(); // Store fireball trail positions
    private boolean isFireballAnimating = false; // Track if fireball animation is active
    private long fireballAnimationStartTime = 0; // Track animation start time
    private static final long FIREBALL_DISPLAY_DURATION = 600; // 600ms for faster animation
    private DungeonMap dungeonMap;
    private StoryTeller storyTeller;
    private int playerX, playerY;
    private Canvas mapCanvas;
    private Canvas sidebarCanvas;
    private List<Enemy> enemies;
    private boolean hasKey;
    private int currentLevel;
    private Random random;
    private int playerHealth;
    private boolean gameOver;
    private String errorMessage;
    private char lastDirection; // Tracks last movement direction (W, A, S, D)
    private int regularEnemyKills;
    private boolean bossKilled;

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("Failed to launch application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
        	
        	
            random = new Random(System.currentTimeMillis());
            currentLevel = 1;
            playerHealth = 100;
            gameOver = false;
            errorMessage = null;
            lastDirection = 'D'; // Default direction: right
            initializeLevel();

            mapCanvas = new Canvas(MAP_WIDTH, MAP_HEIGHT);
            sidebarCanvas = new Canvas(SIDEBAR_WIDTH, MAP_HEIGHT);

            BorderPane root = new BorderPane();
            root.setLeft(mapCanvas);
            root.setRight(sidebarCanvas);

            Scene scene = new Scene(root, MAP_WIDTH + SIDEBAR_WIDTH, MAP_HEIGHT);

            scene.setOnKeyPressed(event -> {
                if (gameOver) return;
                
                System.out.println("Key pressed: " + event.getCode());
                
                
                
                if (gameOver || event == null) return;
                try {
                    int newX = playerX;
                    int newY = playerY;

                    switch (event.getCode()) {
                        case UP:
                        case W:
                            newY--;
                            lastDirection = 'W';
                            break;
                        case DOWN:
                        case S:
                            newY++;
                            lastDirection = 'S';
                            break;
                        case LEFT:
                        case A:
                            newX--;
                            lastDirection = 'A';
                            break;
                        case RIGHT:
                        case D:
                            newX++;
                            lastDirection = 'D';
                            break;
                        case SPACE:
                            shootFireball();
                            break;
                        default:
                            break;
                    }

                    if (event.getCode() != javafx.scene.input.KeyCode.SPACE && isValidMove(newX, newY)) {
                        playerX = newX;
                        playerY = newY;
                        if (dungeonMap != null) {
                            dungeonMap.recordRoomVisit(playerX, playerY);
                            dungeonMap.recordPlayerAction("move");
                        }
                        moveEnemies();
                        checkKeyPickup();
                        checkLevelTransition();
                        updateGame();
                    }
                    if (event.getCode() == KeyCode.SPACE) {
                        System.out.println("Fireball triggered!");
                        shootFireball();
                        return; // Skip movement processing for fireball
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error handling key press: " + e.getMessage());
                    errorMessage = "Error processing input!";
                    updateGame();
                }
            });
            

            primaryStage.setTitle("Dungeon Crawler - Procedural Adventure");
            primaryStage.setScene(scene);
            primaryStage.show();

            updateGame();
            
        } catch (Exception e) {
            System.err.println("Error starting game: " + e.getMessage());
            errorMessage = "Failed to initialize game!";
            if (sidebarCanvas != null) {
                renderSidebar();
            }
        }
    }

    private void initializeLevel() {
        try {
            DungeonGenerator generator = new DungeonGenerator(40, 30, System.currentTimeMillis() + currentLevel);
            dungeonMap = generator.generate();
            if (dungeonMap == null) {
                throw new IllegalStateException("Dungeon map generation failed");
            }
            storyTeller = new StoryTeller(dungeonMap);
            enemies = new ArrayList<>();
            hasKey = false;
            regularEnemyKills = 0;
            bossKilled = false;

            findPlayerStartPosition();
            spawnEnemies();
        } catch (Exception e) {
            System.err.println("Error initializing level " + currentLevel + ": " + e.getMessage());
            errorMessage = "Failed to load level " + currentLevel + "!";
            gameOver = true;
        }
    }

    private void findPlayerStartPosition() {
        try {
            if (dungeonMap == null) {
                throw new IllegalStateException("Dungeon map is null");
            }
            boolean foundStart = false;
            for (int x = 0; x < dungeonMap.getWidth() && !foundStart; x++) {
                for (int y = 0; y < dungeonMap.getHeight() && !foundStart; y++) {
                    if (dungeonMap.getTileType(x, y) == TileType.ROOM) {
                        playerX = x;
                        playerY = y;
                        foundStart = true;
                    }
                }
            }
            if (!foundStart) {
                throw new IllegalStateException("No valid starting position found");
            }
        } catch (Exception e) {
            System.err.println("Error finding player start position: " + e.getMessage());
            errorMessage = "No valid start position!";
            gameOver = true;
        }
    }

    private void spawnEnemies() {
        try {
            if (enemies == null) {
                enemies = new ArrayList<>();
            }
            
            // Get all room tiles in the dungeon
            List<int[]> roomTiles = new ArrayList<>();
            for (int x = 0; x < dungeonMap.getWidth(); x++) {
                for (int y = 0; y < dungeonMap.getHeight(); y++) {
                    if (dungeonMap.getTileType(x, y) == TileType.ROOM) {
                        roomTiles.add(new int[]{x, y});
                    }
                }
            }
            
            if (roomTiles.isEmpty()) {
                throw new IllegalStateException("No room tiles available for enemy spawning");
            }

            // Spawn enemies more strategically across different rooms
            int totalEnemies = 5 + currentLevel; // Scale with level
            int totalBosses = 1 + currentLevel / 5; // Additional bosses every 5 levels
            
            // Distribute enemies across rooms
            for (int i = 0; i < totalEnemies; i++) {
                // Select a random room
                int[] roomCenter = roomTiles.get(random.nextInt(roomTiles.size()));
                int roomX = roomCenter[0];
                int roomY = roomCenter[1];
                
                // Find valid spawn position within this room
                int attempts = 0;
                int x, y;
                do {
                    x = roomX + random.nextInt(7) - 3; // Spread within room
                    y = roomY + random.nextInt(7) - 3;
                    attempts++;
                    if (attempts > 50) break; // Give up after 50 attempts
                } while (!isValidSpawn(x, y));
                
                if (attempts <= 50) {
                    enemies.add(new Enemy(x, y, 50 + currentLevel * 5, false));
                }
            }
            
            // Spawn bosses in separate rooms
            for (int i = 0; i < totalBosses; i++) {
                if (roomTiles.isEmpty()) break;
                
                // Select a random room far from player start
                int[] roomCenter = getFarthestRoom(roomTiles, playerX, playerY);
                int roomX = roomCenter[0];
                int roomY = roomCenter[1];
                
                int attempts = 0;
                int x, y;
                do {
                    x = roomX + random.nextInt(5) - 2;
                    y = roomY + random.nextInt(5) - 2;
                    attempts++;
                    if (attempts > 50) break;
                } while (!isValidSpawn(x, y));
                
                if (attempts <= 50) {
                    enemies.add(new Enemy(x, y, 100 + currentLevel * 10, true));
                    roomTiles.remove(roomCenter); // Don't put multiple bosses in same room
                }
            }
        } catch (Exception e) {
            System.err.println("Error spawning enemies: " + e.getMessage());
            errorMessage = "Failed to spawn enemies!";
            gameOver = true;
        }
    }

    private int[] getFarthestRoom(List<int[]> rooms, int playerX, int playerY) {
        int[] farthest = null;
        double maxDistance = 0;
        
        for (int[] room : rooms) {
            double dist = Math.sqrt(Math.pow(room[0] - playerX, 2) + Math.pow(room[1] - playerY, 2));
            if (dist > maxDistance) {
                maxDistance = dist;
                farthest = room;
            }
        }
        
        return farthest != null ? farthest : rooms.get(0);
    }

    private boolean isValidSpawn(int x, int y) {
        try {
            return x >= 0 && x < dungeonMap.getWidth() &&
                    y >= 0 && y < dungeonMap.getHeight() &&
                    dungeonMap.getTileType(x, y) == TileType.ROOM &&
                    !isEnemyAt(x, y) && !(x == playerX && y == playerY);
        } catch (Exception e) {
            System.err.println("Error checking spawn position: " + e.getMessage());
            return false;
        }
    }

    private boolean isValidMove(int x, int y) {
        try {
            return x >= 0 && x < dungeonMap.getWidth() &&
                    y >= 0 && y < dungeonMap.getHeight() &&
                    dungeonMap.getTileType(x, y) != TileType.WALL &&
                    !isEnemyAt(x, y);
        } catch (Exception e) {
            System.err.println("Error checking valid move: " + e.getMessage());
            return false;
        }
    }

    private void printEnemyPositions() {
        if (enemies == null) {
            System.out.println("No enemies list");
            return;
        }
        System.out.println("Current enemies (" + enemies.size() + "):");
        for (Enemy enemy : enemies) {
            if (enemy != null) {
                System.out.println("- " + (enemy.isBoss() ? "BOSS" : "Enemy") + 
                    " at (" + enemy.getX() + "," + enemy.getY() + ") " + 
                    (enemy.isAlive() ? "ALIVE" : "DEAD"));
            }
        }
    }
    
    
    private boolean isEnemyAt(int x, int y) {
        try {
            if (enemies == null) return false;
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.getX() == x && enemy.getY() == y && enemy.isAlive()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error checking enemy position: " + e.getMessage());
            return false;
        }
    }

    private void moveEnemies() {
        try {
            if (enemies == null || random == null) {
                System.out.println("Enemies or random is null, skipping enemy movement");
                return;
            }

            final int MAX_HEALTH = 100; // Player's maximum health
            final double REGULAR_ENEMY_DAMAGE_PERCENT = 0.05; // 5% of max health
            final double BOSS_ENEMY_DAMAGE_PERCENT = 0.15; // 15% of max health

            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive()) continue;

                // Random movement
                int[] directions = {0, 1, 2, 3}; // 0: up, 1: down, 2: left, 3: right
                int dir = directions[random.nextInt(4)];
                int newX = enemy.getX();
                int newY = enemy.getY();
                switch (dir) {
                    case 0: newY--; break; // Up
                    case 1: newY++; break; // Down
                    case 2: newX--; break; // Left
                    case 3: newX++; break; // Right
                }
                if (isValidEnemyMove(newX, newY)) {
                    enemy.setPosition(newX, newY);
                    System.out.println("Enemy moved to (" + newX + "," + newY + ")");
                }

                // Damage player if adjacent
                if (Math.abs(enemy.getX() - playerX) + Math.abs(enemy.getY() - playerY) == 1) {
                    int damage = enemy.isBoss() ? 
                        (int)(MAX_HEALTH * BOSS_ENEMY_DAMAGE_PERCENT) : 
                        (int)(MAX_HEALTH * REGULAR_ENEMY_DAMAGE_PERCENT);
                    playerHealth -= damage;
                    dungeonMap.recordPlayerAction("damage");
                    System.out.println("Player took " + damage + " damage from " + 
                        (enemy.isBoss() ? "boss" : "enemy") + " at (" + enemy.getX() + "," + enemy.getY() + ")");
                    if (playerHealth <= 0) {
                        gameOver = true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error moving enemies: " + e.getMessage());
            errorMessage = "Error moving enemies!";
        }
    }

    private boolean isPlayerInSameRoomOrAdjacent(int x, int y) {
        // Check if player is in the same room or an adjacent room
        // Implementation depends on your room identification logic
        // This is a simplified version - you might want to implement proper room detection
        return Math.abs(x - playerX) < 10 && Math.abs(y - playerY) < 10;
    }

    private boolean hasLineOfSight(int x1, int y1, int x2, int y2) {
        // Bresenham's line algorithm to check for walls between two points
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        
        while (x1 != x2 || y1 != y2) {
            if (dungeonMap.getTileType(x1, y1) == TileType.WALL) {
                return false;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
        return true;
    }

    private void pathfindTowardPlayer(Enemy enemy) {
        // Simple pathfinding toward player
        int dx = Integer.compare(playerX, enemy.getX());
        int dy = Integer.compare(playerY, enemy.getY());
        
        // Try horizontal movement first
        if (dx != 0 && isValidEnemyMove(enemy.getX() + dx, enemy.getY())) {
            enemy.setPosition(enemy.getX() + dx, enemy.getY());
        } 
        // Then try vertical movement
        else if (dy != 0 && isValidEnemyMove(enemy.getX(), enemy.getY() + dy)) {
            enemy.setPosition(enemy.getX(), enemy.getY() + dy);
        }
    }

    private boolean isValidEnemyMove(int x, int y) {
        try {
            return x >= 0 && x < dungeonMap.getWidth() &&
                    y >= 0 && y < dungeonMap.getHeight() &&
                    dungeonMap.getTileType(x, y) == TileType.ROOM &&
                    !isEnemyAt(x, y) && !(x == playerX && y == playerY);
        } catch (Exception e) {
            System.err.println("Error checking valid enemy move: " + e.getMessage());
            return false;
        }
    }

    private void shootFireball() {
        System.out.println("Attempting to shoot fireball...");
        try {
            if (enemies == null) {
                System.out.println("No enemies list available!");
                errorMessage = "No enemies to target!";
                updateGame();
                return;
            }

            GraphicsContext gc = mapCanvas.getGraphicsContext2D();
            if (gc == null) {
                System.out.println("GraphicsContext is null!");
                errorMessage = "Rendering error!";
                updateGame();
                return;
            }

            // Find the nearest living enemy within FIREBALL_RANGE
            Enemy nearestEnemy = null;
            int minDistance = Integer.MAX_VALUE;
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.isAlive()) {
                    int distance = Math.abs(enemy.getX() - playerX) + Math.abs(enemy.getY() - playerY);
                    if (distance <= FIREBALL_RANGE && distance < minDistance) {
                        nearestEnemy = enemy;
                        minDistance = distance;
                    }
                }
            }

            if (nearestEnemy == null) {
                System.out.println("No enemies within range!");
                errorMessage = "No enemies in range!";
                updateGame();
                return;
            }

            // Determine direction to nearest enemy
            int dx = 0, dy = 0;
            int deltaX = nearestEnemy.getX() - playerX;
            int deltaY = nearestEnemy.getY() - playerY;
            // Prioritize larger axis to align with grid movement
            if (Math.abs(deltaX) >= Math.abs(deltaY)) {
                dx = deltaX > 0 ? 1 : -1; // Right or left
            } else {
                dy = deltaY > 0 ? 1 : -1; // Down or up
            }
            System.out.println("Targeting enemy at (" + nearestEnemy.getX() + "," + nearestEnemy.getY() + ") with direction dx=" + dx + ", dy=" + dy);

            // Clear previous fireball trail
            fireballTrailPositions.clear();
            isFireballAnimating = true;
            fireballAnimationStartTime = System.currentTimeMillis();

            boolean hitEnemy = false;

            // Calculate fireball path and check for hits
            for (int i = 1; i <= FIREBALL_RANGE; i++) {
                int targetX = playerX + dx * i;
                int targetY = playerY + dy * i;

                // Stop if out of bounds or hitting a wall
                if (targetX < 0 || targetX >= dungeonMap.getWidth() ||
                    targetY < 0 || targetY >= dungeonMap.getHeight() ||
                    dungeonMap.getTileType(targetX, targetY) == TileType.WALL) {
                    System.out.println("Fireball stopped at (" + targetX + "," + targetY + ") - Wall or out of bounds");
                    break;
                }

                // Calculate screen position for rendering
                int screenX = (targetX - playerX) * TILE_SIZE + MAP_WIDTH / 2 - TILE_SIZE / 2;
                int screenY = (targetY - playerY) * TILE_SIZE + MAP_HEIGHT / 2 - TILE_SIZE / 2;
                fireballTrailPositions.add(new double[]{screenX + TILE_SIZE / 2.0, screenY + TILE_SIZE / 2.0});
                System.out.println("Added trail position: (" + (screenX + TILE_SIZE / 2.0) + "," + (screenY + TILE_SIZE / 2.0) + ")");

                // Check for enemy hits
                System.out.println("Fireball at (" + targetX + "," + targetY + ") - Checking for enemies...");
                for (Enemy enemy : enemies) {
                    if (enemy != null && enemy.isAlive() && 
                        enemy.getX() == targetX && enemy.getY() == targetY) {
                        System.out.println("Hit enemy at (" + targetX + "," + targetY + ")");
                        int damage = 25;
                        if (i == FIREBALL_RANGE) {
                            damage *= 2;
                            storyTeller.addStoryFragment("Critical hit at max range!");
                        }
                        enemy.takeDamage(damage);
                        hitEnemy = true;
                        dungeonMap.recordPlayerAction("fireball");
                        if (!enemy.isAlive()) {
                            if (enemy.isBoss()) {
                                bossKilled = true;
                                dungeonMap.recordPlayerAction("boss_kill");
                                storyTeller.addKillStory(true);
                            } else {
                                regularEnemyKills++;
                                dungeonMap.recordPlayerAction("kill");
                                storyTeller.addKillStory(false);
                            }
                        }
                        break; // Stop fireball at first enemy hit
                    }
                }
                if (hitEnemy) break;
            }

            // Start AnimationTimer for fireball trail
            AnimationTimer fireballAnimation = new AnimationTimer() {
                private long lastUpdate = 0;
                private int currentDot = 0;

                @Override
                public void handle(long now) {
                    if (now - lastUpdate >= 100_000_000) { // 100ms per dot
                        if (currentDot < fireballTrailPositions.size()) {
                            updateGame(); // Redraw map with current dot in renderMap
                            currentDot++;
                        } else {
                            stop(); // End animation
                            checkKeyPickup();
                            checkLevelTransition();
                            moveEnemies(); // Move enemies after fireball resolves
                            updateGame();
                        }
                        lastUpdate = now;
                    }
                }
            };
            fireballAnimation.start();

        } catch (Exception e) {
            System.err.println("Error shooting fireball: " + e.getMessage());
            errorMessage = "Fireball failed!";
            updateGame();
        }
    }
    private void checkKeyPickup() {
        try {
            int requiredKills = 5 + currentLevel / 5;
            if (!hasKey && enemies != null && regularEnemyKills >= requiredKills && bossKilled) {
                hasKey = true;
                if (storyTeller != null) {
                    storyTeller.addKeyStory();
                }
                if (dungeonMap != null) {
                    dungeonMap.setKeyPosition(playerX, playerY);
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking key pickup: " + e.getMessage());
            errorMessage = "Error with key pickup!";
        }
    }

    private void checkLevelTransition() {
        try {
            if (hasKey && dungeonMap != null && dungeonMap.getTileType(playerX, playerY) == TileType.DOOR) {
                if (currentLevel < MAX_LEVELS) {
                    currentLevel++;
                    initializeLevel();
                } else {
                    gameOver = true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking level transition: " + e.getMessage());
            errorMessage = "Error advancing level!";
        }
    }

    private void updateGame() {
        try {
            renderMap();
            renderSidebar();
            if (storyTeller != null && dungeonMap != null) {
                storyTeller.update(playerX, playerY);
            }
        } catch (Exception e) {
            System.err.println("Error updating game: " + e.getMessage());
            errorMessage = "Error updating game!";
            renderSidebar();
        }
    }

    private void renderMap() {
        try {
            GraphicsContext gc = mapCanvas.getGraphicsContext2D();
            if (gc == null) {
                throw new IllegalStateException("GraphicsContext is null");
            }
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, MAP_WIDTH, MAP_HEIGHT);

            // Test shapes to verify rendering
            gc.setFill(Color.BLUE);
            gc.fillRect(50, 50, 20, 20); // Blue square at (50,50) for debugging
            System.out.println("Drawing test blue square at (50,50)");
            if (isFireballAnimating) {
                gc.setFill(Color.RED);
                gc.fillRect(70, 50, 20, 20); // Red square when fireball is active
                System.out.println("Drawing test red square at (70,50) during fireball animation");
            }

            int startX = Math.max(0, playerX - MAP_WIDTH / TILE_SIZE / 2);
            int startY = Math.max(0, playerY - MAP_HEIGHT / TILE_SIZE / 2);
            int endX = Math.min(dungeonMap.getWidth(), playerX + MAP_WIDTH / TILE_SIZE / 2);
            int endY = Math.min(dungeonMap.getHeight(), playerY + MAP_HEIGHT / TILE_SIZE / 2);

            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    int screenX = (x - playerX) * TILE_SIZE + MAP_WIDTH / 2 - TILE_SIZE / 2;
                    int screenY = (y - playerY) * TILE_SIZE + MAP_HEIGHT / 2 - TILE_SIZE / 2;

                    switch (dungeonMap.getTileType(x, y)) {
                        case WALL:
                            gc.setFill(Color.rgb(50, 50, 50));
                            break;
                        case FLOOR:
                            gc.setFill(Color.rgb(100, 100, 100));
                            break;
                        case ROOM:
                            gc.setFill(Color.rgb(150, 150, 150));
                            break;
                        case DOOR:
                            gc.setFill(Color.rgb(139, 69, 19));
                            break;
                    }

                    gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);

                    gc.setStroke(Color.rgb(30, 30, 30));
                    gc.strokeRect(screenX, screenY, TILE_SIZE, TILE_SIZE);

                    if (dungeonMap.isExplored(x, y)) {
                        gc.setFill(Color.rgb(255, 255, 255, 0.1));
                        gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                    }
                }
            }

            if (enemies != null) {
                for (Enemy enemy : enemies) {
                    if (enemy != null && enemy.isAlive()) {
                        int screenX = (enemy.getX() - playerX) * TILE_SIZE + MAP_WIDTH / 2 - TILE_SIZE / 2;
                        int screenY = (enemy.getY() - playerY) * TILE_SIZE + MAP_HEIGHT / 2 - TILE_SIZE / 2;
                        gc.setFill(enemy.isBoss() ? Color.PURPLE : Color.GREEN);
                        gc.fillRect(screenX + TILE_SIZE / 4, screenY + TILE_SIZE / 4, TILE_SIZE / 2, TILE_SIZE / 2);
                    }
                }
            }

            if (hasKey && dungeonMap.getKeyX() != -1 && dungeonMap.getKeyY() != -1) {
                int screenX = (dungeonMap.getKeyX() - playerX) * TILE_SIZE + MAP_WIDTH / 2 - TILE_SIZE / 2;
                int screenY = (dungeonMap.getKeyY() - playerY) * TILE_SIZE + MAP_HEIGHT / 2 - TILE_SIZE / 2;
                gc.setFill(Color.YELLOW);
                gc.fillRect(screenX + TILE_SIZE / 4, screenY + TILE_SIZE / 4, TILE_SIZE / 2, TILE_SIZE / 2);
            }

            // Draw player
            gc.setFill(Color.RED);
            gc.fillOval(
                    MAP_WIDTH / 2 - TILE_SIZE / 2,
                    MAP_HEIGHT / 2 - TILE_SIZE / 2,
                    TILE_SIZE,
                    TILE_SIZE
            );

            // Draw fireball trail if animating
            if (isFireballAnimating && !fireballTrailPositions.isEmpty()) {
                long elapsed = System.currentTimeMillis() - fireballAnimationStartTime;
                if (elapsed > FIREBALL_DISPLAY_DURATION) {
                    isFireballAnimating = false;
                    fireballTrailPositions.clear();
                } else {
                    gc.setFill(Color.YELLOW); // Brighter yellow for visibility
                    int dotsToDraw = Math.min(fireballTrailPositions.size(), 
                        (int)(elapsed / 100) + 1); // Draw one dot every 100ms
                    for (int i = 0; i < dotsToDraw; i++) {
                        double[] pos = fireballTrailPositions.get(i);
                        gc.fillOval(pos[0] - 8, pos[1] - 8, 16, 16); // 16x16 dots
                        System.out.println("Rendering fireball dot " + i + " at screen (" + pos[0] + "," + pos[1] + ")");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error rendering map: " + e.getMessage());
            errorMessage = "Error rendering map!";
        }
    }
    

    private void renderSidebar() {
        try {
            GraphicsContext gc = sidebarCanvas.getGraphicsContext2D();
            if (gc == null) {
                throw new IllegalStateException("Sidebar GraphicsContext is null");
            }
            gc.setFill(Color.rgb(30, 30, 40));
            gc.fillRect(0, 0, SIDEBAR_WIDTH, MAP_HEIGHT);

            gc.setFont(Font.font("Courier New", 14));
            gc.setFill(Color.WHITE);

            gc.setFont(Font.font("Courier New", 18));
            gc.fillText("Dungeon Journal", 20, 30);
            gc.setStroke(Color.GOLD);
            gc.strokeLine(20, 35, SIDEBAR_WIDTH - 20, 35);

            gc.setFont(Font.font("Courier New", 14));

            gc.fillText("Level: " + currentLevel, 20, 50);
            gc.fillText(String.format("Position: (%d, %d)", playerX, playerY), 20, 70);
            gc.fillText("Health: " + playerHealth, 20, 90);

            String roomType = "";
            try {
                switch (dungeonMap.getTileType(playerX, playerY)) {
                    case FLOOR:
                        roomType = "Corridor";
                        break;
                    case ROOM:
                        roomType = "Room";
                        break;
                    case DOOR:
                        roomType = "Doorway";
                        break;
                    default:
                        roomType = "Unknown";
                        break;
                }
            } catch (Exception e) {
                roomType = "Unknown";
            }
            gc.fillText("Location: " + roomType, 20, 110);

            gc.fillText("Key: " + (hasKey ? "Obtained" : "Not Found"), 20, 130);
            gc.fillText("Enemies Killed: " + regularEnemyKills + "/" + (5 + currentLevel / 5), 20, 150);
            gc.fillText("Boss Killed: " + (bossKilled ? "Yes" : "No"), 20, 170);

            gc.fillText("Story Events:", 20, 200);
            int yPos = 220;
            if (storyTeller != null) {
                for (String fragment : storyTeller.getCurrentStoryFragments()) {
                    gc.setFill(Color.LIGHTGOLDENRODYELLOW);
                    gc.fillText("> " + fragment, 30, yPos);
                    yPos += 20;
                    if (yPos > MAP_HEIGHT - 100) break;
                }
            }

            gc.setFill(Color.WHITE);
            gc.fillText("Player Actions:", 20, MAP_HEIGHT - 100);
            gc.fillText("Moves: " + (dungeonMap != null ? dungeonMap.getActionCount("move") : 0), 30, MAP_HEIGHT - 80);
            gc.fillText("Fireballs: " + (dungeonMap != null ? dungeonMap.getActionCount("fireball") : 0), 30, MAP_HEIGHT - 60);
            gc.fillText("Kills: " + (dungeonMap != null ? dungeonMap.getActionCount("kill") : 0), 30, MAP_HEIGHT - 40);

            gc.setFill(Color.LIGHTGRAY);
            gc.fillText("Controls:", 20, MAP_HEIGHT - 30);
            gc.fillText("WASD/Arrows to move, Space to shoot fireball", 30, MAP_HEIGHT - 10);

            if (gameOver) {
                gc.setFill(Color.RED);
                gc.setFont(Font.font("Courier New", 20));
                if (playerHealth <= 0) {
                    gc.fillText("Game Over! You died.", 20, MAP_HEIGHT / 2);
                } else if (currentLevel >= MAX_LEVELS) {
                    gc.fillText("Victory! Dungeon Conquered!", 20, MAP_HEIGHT / 2);
                }
            }

            if (errorMessage != null) {
                gc.setFill(Color.RED);
                gc.setFont(Font.font("Courier New", 16));
                gc.fillText(errorMessage, 20, MAP_HEIGHT / 2 + 30);
            }
        } catch (Exception e) {
            System.err.println("Error rendering sidebar: " + e.getMessage());
        }
    }
}