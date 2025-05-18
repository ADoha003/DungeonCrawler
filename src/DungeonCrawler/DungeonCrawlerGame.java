package DungeonCrawler;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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
            // Initialize game
            random = new Random(System.currentTimeMillis());
            currentLevel = 1;
            playerHealth = 100;
            gameOver = false;
            errorMessage = null;
            initializeLevel();

            // Create JavaFX UI
            mapCanvas = new Canvas(MAP_WIDTH, MAP_HEIGHT);
            sidebarCanvas = new Canvas(SIDEBAR_WIDTH, MAP_HEIGHT);

            BorderPane root = new BorderPane();
            root.setLeft(mapCanvas);
            root.setRight(sidebarCanvas);

            Scene scene = new Scene(root, MAP_WIDTH + SIDEBAR_WIDTH, MAP_HEIGHT);

            // Handle keyboard input
            scene.setOnKeyPressed(event -> {
                if (gameOver || event == null) return;
                try {
                    int newX = playerX;
                    int newY = playerY;

                    switch (event.getCode()) {
                        case UP:
                        case W:
                            newY--;
                            break;
                        case DOWN:
                        case S:
                            newY++;
                            break;
                        case LEFT:
                        case A:
                            newX--;
                            break;
                        case RIGHT:
                        case D:
                            newX++;
                            break;
                        case SPACE:
                            attack();
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
            // Initialize dungeon map
            DungeonGenerator generator = new DungeonGenerator(40, 30, System.currentTimeMillis() + currentLevel);
            dungeonMap = generator.generate();
            if (dungeonMap == null) {
                throw new IllegalStateException("Dungeon map generation failed");
            }
            storyTeller = new StoryTeller(dungeonMap);
            enemies = new ArrayList<>();
            hasKey = false;

            // Find starting position
            findPlayerStartPosition();

            // Spawn enemies
            spawnEnemies();
        } catch (Exception e) {
            System.err.println("Error initializing level " + currentLevel + ": " + e.getMessage());
            errorMessage = "Failed to load level " + currentLevel + "!";
            gameOver = true;
        }
    }

    private void findPlayerStartPosition() {
        try {
            boolean foundStart = false;
            if (dungeonMap == null) {
                throw new IllegalStateException("Dungeon map is null");
            }
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
            int numEnemies = 3 + currentLevel / 5;
            for (int i = 0; i < numEnemies; i++) {
                int x, y;
                int attempts = 0;
                do {
                    x = random.nextInt(dungeonMap.getWidth());
                    y = random.nextInt(dungeonMap.getHeight());
                    attempts++;
                    if (attempts > 100) {
                        throw new IllegalStateException("Could not find valid enemy spawn position");
                    }
                } while (dungeonMap.getTileType(x, y) != TileType.ROOM || (x == playerX && y == playerY));
                enemies.add(new Enemy(x, y, 50 + currentLevel * 5));
            }
        } catch (Exception e) {
            System.err.println("Error spawning enemies: " + e.getMessage());
            errorMessage = "Failed to spawn enemies!";
            gameOver = true;
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
            if (enemies == null || random == null) return;
            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive()) continue;
                if (dungeonMap.getTileType(enemy.getX(), enemy.getY()) == TileType.ROOM) {
                    int[] directions = {0, 1, 2, 3};
                    int dir = directions[random.nextInt(4)];
                    int newX = enemy.getX();
                    int newY = enemy.getY();
                    switch (dir) {
                        case 0: newY--; break;
                        case 1: newY++; break;
                        case 2: newX--; break;
                        case 3: newX++; break;
                    }
                    if (isValidEnemyMove(newX, newY)) {
                        enemy.setPosition(newX, newY);
                    }
                }
                // Enemy attacks player if adjacent
                if (Math.abs(enemy.getX() - playerX) + Math.abs(enemy.getY() - playerY) == 1) {
                    playerHealth -= 10;
                    if (dungeonMap != null) {
                        dungeonMap.recordPlayerAction("damage");
                    }
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

    private void attack() {
        try {
            if (enemies == null) return;
            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive()) continue;
                if (Math.abs(enemy.getX() - playerX) + Math.abs(enemy.getY() - playerY) == 1) {
                    enemy.takeDamage(20);
                    if (dungeonMap != null) {
                        dungeonMap.recordPlayerAction("attack");
                    }
                    if (!enemy.isAlive() && storyTeller != null) {
                        dungeonMap.recordPlayerAction("kill");
                        storyTeller.addKillStory();
                    }
                }
            }
            moveEnemies();
            checkKeyPickup();
            checkLevelTransition();
            updateGame();
        } catch (Exception e) {
            System.err.println("Error during attack: " + e.getMessage());
            errorMessage = "Error during combat!";
        }
    }

    private void checkKeyPickup() {
        try {
            if (!hasKey && enemies != null && enemies.stream().noneMatch(Enemy::isAlive)) {
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
                            gc.setFill(Color.rgb(150, 150, 100));
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
                        gc.setFill(Color.GREEN);
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

            gc.setFill(Color.RED);
            gc.fillOval(
                    MAP_WIDTH / 2 - TILE_SIZE / 2,
                    MAP_HEIGHT / 2 - TILE_SIZE / 2,
                    TILE_SIZE,
                    TILE_SIZE
            );
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

            gc.fillText("Story Events:", 20, 160);
            int yPos = 180;
            if (storyTeller != null) {
                for (String fragment : storyTeller.getCurrentStoryFragments()) {
                    gc.setFill(Color.LIGHTGOLDENRODYELLOW);
                    gc.fillText("> " + fragment, 30, yPos);
                    yPos += 20;
                    if (yPos > MAP_HEIGHT - 100) break;
                }
            }

            gc.setFill(Color.WHITE);
            gc.fillText("Player Actions:", 20, MAP_HEIGHT - 80);
            gc.fillText("Moves: " + (dungeonMap != null ? dungeonMap.getActionCount("move") : 0), 30, MAP_HEIGHT - 60);
            gc.fillText("Kills: " + (dungeonMap != null ? dungeonMap.getActionCount("kill") : 0), 30, MAP_HEIGHT - 40);

            gc.setFill(Color.LIGHTGRAY);
            gc.fillText("Controls:", 20, MAP_HEIGHT - 30);
            gc.fillText("WASD or Arrows to move, Space to attack", 30, MAP_HEIGHT - 10);

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