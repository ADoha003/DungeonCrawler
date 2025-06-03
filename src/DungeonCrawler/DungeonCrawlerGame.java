package DungeonCrawler;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class DungeonCrawlerGame extends Application {
    public static final int TILE_SIZE = 32;
    private static final int SIDEBAR_WIDTH = 700;
    private static final int MAX_LEVELS = 30;
    private static final int FIREBALL_RANGE = 3;
    private static final long FIREBALL_DISPLAY_DURATION = 600;
    private static final int HEALTH_POTION_HEAL = 50;
    private static final long FRAME_DELAY = 100;

    private List<double[]> fireballTrailPositions = new ArrayList<>();
    private boolean isFireballAnimating = false;
    private long fireballAnimationStartTime = 0;
    private DungeonMap dungeonMap;
    private StoryTeller storyTeller;
    private int playerX, playerY;
    private Canvas mapCanvas;
    private Canvas sidebarCanvas;
    private List<Enemy> enemies;
    private boolean hasKey;
    private boolean keyCollected;
    private int currentLevel;
    private Random random;
    private int playerHealth;
    private boolean gameOver;
    private String errorMessage;
    private char lastDirection;
    private int regularEnemyKills;
    private boolean bossKilled;
    private List<GoldDrop> goldDrops = new ArrayList<>();
    private List<HealthPotion> healthPotionsOnMap = new ArrayList<>();
    private int playerGold = 0;
    private int playerHealthPotions = 0;
    private int enemiesKilled = 0;
    private MediaPlayer footstepSound;
    private MediaPlayer goldSound;
    private MediaPlayer potionSound;
    private MediaPlayer fireballSound;
    private MediaPlayer keySound;
    private MediaPlayer doorSound;
    private Image[] goldCoinFrames = new Image[9];
    private Image fireblastImage;
    private Image potionImage;
    private Image keyImage;
    private Image doorImage;
    private Image levelDoorImage;
    private int currentGoldFrame = 0;
    private long lastFrameTime = 0;
    private boolean resourcesLoaded = false;
    private VBox gameOverButtons;
    private BorderPane root;

    public static void main(String[] args) {
        try {
            Application.launch(args);
        } catch (Exception e) {
            System.err.println("Failed to launch application: " + e.getMessage());
            e.printStackTrace();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Launch Error");
                alert.setHeaderText("Failed to start Dungeon Crawler");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
                Platform.exit();
            });
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
            lastDirection = 'D';
            keyCollected = false;
            initializeLevel();

            mapCanvas = new Canvas();
            sidebarCanvas = new Canvas();
            root = new BorderPane();
            root.setLeft(mapCanvas);
            root.setRight(sidebarCanvas);

            // Initialize game over buttons
            gameOverButtons = new VBox(10);
            gameOverButtons.setVisible(false);
            Button restartButton = new Button("Restart Level");
            Button newGameButton = new Button("New Game");
            Button quitButton = new Button("Quit Game");

            // Style buttons
            restartButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10 5 10;");
            newGameButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10 5 10;");
            quitButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10 5 10;");

            // Add hover effects
            restartButton.setOnMouseEntered(e -> restartButton.setStyle("-fx-background-color: #45a049; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10 5 10;"));
            restartButton.setOnMouseExited(e -> restartButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10 5 10;"));
            newGameButton.setOnMouseEntered(e -> newGameButton.setStyle("-fx-background-color: #1e88e5; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10 5 10;"));
            newGameButton.setOnMouseExited(e -> newGameButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10 5 10;"));
            quitButton.setOnMouseEntered(e -> quitButton.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10 5 10;"));
            quitButton.setOnMouseExited(e -> quitButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 10 5 10;"));

            // Button actions
            restartButton.setOnAction(e -> restartLevel());
            newGameButton.setOnAction(e -> startNewGame());
            quitButton.setOnAction(e -> primaryStage.close());

            gameOverButtons.getChildren().addAll(restartButton, newGameButton, quitButton);
            root.setBottom(gameOverButtons);

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            checkResources();
            initializeGame();

            scene.setOnKeyPressed(event -> {
                if (event == null) return;
                System.out.println("Key pressed: " + event.getCode());
                try {
                    int newX = playerX;
                    int newY = playerY;

                    if (!gameOver) {
                        switch (event.getCode()) {
                            case UP:
                            case W:
                                newY--;
                                lastDirection = 'W';
                                handleMovement(newX, newY);
                                break;
                            case DOWN:
                            case S:
                                newY++;
                                lastDirection = 'S';
                                handleMovement(newX, newY);
                                break;
                            case LEFT:
                            case A:
                                newX--;
                                lastDirection = 'A';
                                handleMovement(newX, newY);
                                break;
                            case RIGHT:
                            case D:
                                newX++;
                                lastDirection = 'D';
                                handleMovement(newX, newY);
                                break;
                            case SPACE:
                                shootFireball();
                                break;
                            case H:
                                useHealthPotion();
                                break;
                            case O:
                                saveGame();
                                break;
                            case L:
                                loadGame();
                                break;
                            default:
                                break;
                        }
                    } else {
                        switch (event.getCode()) {
                            case R:
                                restartLevel();
                                break;
                            case N:
                                startNewGame();
                                break;
                            case Q:
                                primaryStage.close();
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error handling key press: " + e.getMessage());
                    errorMessage = "Error processing input!";
                    updateGame();
                }
            });

            primaryStage.setTitle("Dungeon Crawler - Procedural Adventure");
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint("Press ESC to exit full screen");
            primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            mapCanvas.setWidth(primaryStage.getWidth() - SIDEBAR_WIDTH);
            mapCanvas.setHeight(primaryStage.getHeight());
            sidebarCanvas.setWidth(SIDEBAR_WIDTH);
            sidebarCanvas.setHeight(primaryStage.getHeight());

            primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                double newWidth = newVal.doubleValue() - SIDEBAR_WIDTH;
                mapCanvas.setWidth(newWidth);
                sidebarCanvas.setWidth(SIDEBAR_WIDTH);
                updateGame();
            });

            primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                double newHeight = newVal.doubleValue();
                mapCanvas.setHeight(newHeight);
                sidebarCanvas.setHeight(newHeight);
                updateGame();
            });

            System.out.println("StoryTeller methods: " +
                    Arrays.toString(StoryTeller.class.getDeclaredMethods()));

            primaryStage.show();
            updateGame();
        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            showErrorDialog("Fatal Error", "Failed to start game", e.getMessage());
            Platform.exit();
        }
    }

    private void showErrorDialog(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void checkResources() {
        System.out.println("\n=== Resource Check ===");
        checkResource("/DungeonCrawler/res/sounds/Fire.wav");
        checkResource("/DungeonCrawler/res/sounds/gold_sack.wav");
        checkResource("/DungeonCrawler/res/sounds/potion.wav");
        checkResource("/DungeonCrawler/res/sounds/footstep.wav");
        checkResource("/DungeonCrawler/res/sounds/key.wav");
        checkResource("/DungeonCrawler/res/sounds/Door.wav");
        for (int i = 1; i <= 9; i++) {
            checkResource("/DungeonCrawler/res/images/goldCoin" + i + ".png");
        }
        checkResource("/DungeonCrawler/res/images/fireblast1.png");
        checkResource("/DungeonCrawler/res/images/potion.png");
        checkResource("/DungeonCrawler/res/images/key.png");
        checkResource("/DungeonCrawler/res/images/Door.png");
        checkResource("/DungeonCrawler/res/images/Door_level.png");
        System.out.println("=== End Resource Check ===\n");
    }

    private void checkResource(String path) {
        try {
            URL res = getClass().getResource(path);
            if (res != null) {
                System.out.println("[FOUND] " + path);
                System.out.println("  -> " + res.toString());
            } else {
                System.err.println("[MISSING] " + path);
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Checking " + path + ": " + e.getMessage());
        }
    }

    private Media loadMedia(String path) {
        try {
            URL res = getClass().getResource(path);
            if (res == null) {
                System.err.println("Resource not found: " + path);
                return null;
            }
            String mediaPath = res.toString();
            System.out.println("Loading media: " + mediaPath);
            File file = new File(res.toURI());
            if (!file.exists()) {
                System.err.println("File does not exist: " + file.getAbsolutePath());
                return null;
            }
            Media media = new Media(mediaPath);
            return media;
        } catch (Exception e) {
            System.err.println("Error loading media " + path + ": " + e.getMessage());
            return null;
        }
    }

    private MediaPlayer createSoundPlayer(String path) {
        try {
            Media media = loadMedia(path);
            if (media == null) {
                System.err.println("Sound not found: " + path);
                return null;
            }
            MediaPlayer player = new MediaPlayer(media);
            player.setOnError(() -> System.err.println("Sound error: " + path + " - " + player.getError()));
            return player;
        } catch (Exception e) {
            System.err.println("Error creating sound player: " + e.getMessage());
            return null;
        }
    }

    private Image loadImage(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is != null) {
                Image image = new Image(is);
                System.out.println("Loaded image: " + path);
                return image;
            }
            System.err.println("Image not found: " + path);
        } catch (Exception e) {
            System.err.println("Error loading image " + path + ": " + e.getMessage());
        }
        return null;
    }

    private Image createColoredImage(Color color) {
        int size = 32;
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                pw.setColor(x, y, color);
            }
        }
        return img;
    }

    private Image loadImageWithFallback(String path, Color fallbackColor) {
        try {
            Image img = loadImage(path);
            if (img == null) {
                System.err.println("Creating fallback image for: " + path);
                return createColoredImage(fallbackColor);
            }
            return img;
        } catch (Exception e) {
            System.err.println("Error loading image " + path + ", using fallback: " + e.getMessage());
            return createColoredImage(fallbackColor);
        }
    }

    private void playSound(MediaPlayer soundPlayer, String soundName) {
        if (soundPlayer != null) {
            try {
                soundPlayer.stop();
                soundPlayer.setVolume(0.7);
                soundPlayer.play();
                System.out.println("Played sound: " + soundName);
            } catch (Exception e) {
                System.err.println("Error playing " + soundName + ": " + e.getMessage());
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        } else {
            System.err.println("Sound player is null: " + soundName);
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    private void initializeGame() {
        try {
            loadSounds();
            loadImages();
            resourcesLoaded = true;
        } catch (Exception e) {
            System.err.println("Error loading resources: " + e.getMessage());
            errorMessage = "Failed to load resources! Using fallbacks.";
            createFallbackResources();
        }
    }

    private void loadSounds() {
        try {
            footstepSound = createSoundPlayer("/DungeonCrawler/res/sounds/footstep.wav");
            goldSound = createSoundPlayer("/DungeonCrawler/res/sounds/gold_sack.wav");
            potionSound = createSoundPlayer("/DungeonCrawler/res/sounds/potion.wav");
            fireballSound = createSoundPlayer("/DungeonCrawler/res/sounds/Fire.wav");
            keySound = createSoundPlayer("/DungeonCrawler/res/sounds/key.wav");
            doorSound = createSoundPlayer("/DungeonCrawler/res/sounds/Door.wav");
            if (doorSound == null) {
                System.out.println("Using fallback for door sound");
                doorSound = keySound != null ? keySound : createSoundPlayer("/DungeonCrawler/res/sounds/key.wav");
            }
        } catch (Exception e) {
            System.err.println("Error loading sounds: " + e.getMessage());
            errorMessage = "Sound loading failed, using fallbacks.";
        }
    }

    private void loadImages() {
        try {
            for (int i = 0; i < 9; i++) {
                String imagePath = "/DungeonCrawler/res/images/goldCoin" + (i + 1) + ".png";
                goldCoinFrames[i] = loadImageWithFallback(imagePath, Color.GOLD);
            }
            fireblastImage = loadImageWithFallback("/DungeonCrawler/res/images/fireblast1.png", Color.ORANGERED);
            potionImage = loadImageWithFallback("/DungeonCrawler/res/images/potion.png", Color.RED);
            keyImage = loadImageWithFallback("/DungeonCrawler/res/images/key.png", Color.YELLOW);
            doorImage = loadImageWithFallback("/DungeonCrawler/res/images/Door.png", Color.rgb(139, 69, 19));
            levelDoorImage = loadImageWithFallback("/DungeonCrawler/res/images/Door_level.png", Color.rgb(100, 50, 150));
        } catch (Exception e) {
            System.err.println("Error loading images: " + e.getMessage());
            errorMessage = "Image loading failed, using fallbacks.";
        }
    }

    private void createFallbackResources() {
        try {
            for (int i = 0; i < 9; i++) {
                goldCoinFrames[i] = createColoredImage(Color.GOLD);
            }
            fireblastImage = createColoredImage(Color.ORANGERED);
            potionImage = createColoredImage(Color.RED);
            keyImage = createColoredImage(Color.YELLOW);
            doorImage = createColoredImage(Color.rgb(139, 69, 19));
            levelDoorImage = createColoredImage(Color.rgb(100, 50, 150));
        } catch (Exception e) {
            System.err.println("Error creating fallback resources: " + e.getMessage());
            errorMessage = "Failed to create fallback resources!";
        }
    }

    @Override
    public void stop() {
        try {
            if (footstepSound != null) footstepSound.dispose();
            if (goldSound != null) goldSound.dispose();
            if (potionSound != null) potionSound.dispose();
            if (fireballSound != null) fireballSound.dispose();
            if (keySound != null) keySound.dispose();
            if (doorSound != null) doorSound.dispose();
        } catch (Exception e) {
            System.err.println("Error stopping sound players: " + e.getMessage());
        }
    }

    private void handleMovement(int newX, int newY) {
        if (isValidMove(newX, newY)) {
            playerX = newX;
            playerY = newY;
            playSound(footstepSound, "footstep");
            dungeonMap.recordRoomVisit(playerX, playerY);
            dungeonMap.recordPlayerAction("move");
            moveEnemies();
            checkKeyPickup();
            checkItemCollection();
            checkLevelTransition();
            updateGame();
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
            goldDrops.clear();
            healthPotionsOnMap.clear();
            hasKey = false;
            keyCollected = false;
            regularEnemyKills = 0;
            bossKilled = false;
            findPlayerStartPosition();
            spawnEnemies();
        } catch (Exception e) {
            System.err.println("Error initializing level " + currentLevel + ": " + e.getMessage());
            errorMessage = "Failed to load level " + currentLevel + "!";
            gameOver = true;
            showErrorDialog("Level Error", "Failed to initialize level", e.getMessage());
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
            showErrorDialog("Position Error", "No valid start position", e.getMessage());
        }
    }

    private void spawnEnemies() {
        try {
            if (enemies == null) {
                enemies = new ArrayList<>();
            }

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

            int totalEnemies = 5 + currentLevel;
            int totalBosses = 1 + currentLevel / 5;

            for (int i = 0; i < totalEnemies; i++) {
                int[] roomCenter = roomTiles.get(random.nextInt(roomTiles.size()));
                int roomX = roomCenter[0];
                int roomY = roomCenter[1];

                int attempts = 0;
                int x, y;
                do {
                    x = roomX + random.nextInt(7) - 3;
                    y = roomY + random.nextInt(7) - 3;
                    attempts++;
                    if (attempts > 50) break;
                } while (!isValidSpawn(x, y) || dungeonMap.getTileType(x, y) != TileType.ROOM);

                if (attempts <= 50) {
                    enemies.add(new Enemy(x, y, 50 + currentLevel * 5, false));
                }
            }

            for (int i = 0; i < totalBosses; i++) {
                if (roomTiles.isEmpty()) break;
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
                } while (!isValidSpawn(x, y) || dungeonMap.getTileType(x, y) != TileType.ROOM);

                if (attempts <= 50) {
                    enemies.add(new Enemy(x, y, 100 + currentLevel * 10, true));
                    roomTiles.removeIf(tile -> tile[0] == roomX && tile[1] == roomY);
                }
            }
        } catch (Exception e) {
            System.err.println("Error spawning enemies: " + e.getMessage());
            errorMessage = "Failed to spawn enemies!";
            gameOver = true;
            showErrorDialog("Enemy Spawn Error", "Failed to spawn enemies", e.getMessage());
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
                    !isEnemyAt(x, y) &&
                    !(x == playerX && y == playerY) &&
                    (Math.abs(x - playerX) > 5 || Math.abs(y - playerY) > 5);
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

    private boolean isEnemyAt(int x, int y) {
        try {
            if (enemies == null) return false;
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.isAlive() && enemy.getX() == x && enemy.getY() == y) {
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

            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isAlive()) continue;

                if (Math.abs(enemy.getX() - playerX) > 10 || Math.abs(enemy.getY() - playerY) > 10) {
                    continue;
                }

                int dx = Integer.compare(playerX, enemy.getX());
                int dy = Integer.compare(playerY, enemy.getY());

                int newX = enemy.getX();
                int newY = enemy.getY();

                if (random.nextBoolean() && dx != 0) {
                    newX = enemy.getX() + dx;
                    if (isValidEnemyMove(newX, enemy.getY())) {
                        enemy.setPosition(newX, enemy.getY());
                    }
                } else if (dy != 0) {
                    newY = enemy.getY() + dy;
                    if (isValidEnemyMove(enemy.getX(), newY)) {
                        enemy.setPosition(enemy.getX(), newY);
                    }
                }

                if (Math.abs(enemy.getX() - playerX) + Math.abs(enemy.getY() - playerY) == 1) {
                    int damage = enemy.isBoss() ? 15 : 5;
                    playerHealth -= damage;
                    dungeonMap.recordPlayerAction("damage");
                    storyTeller.addStoryFragment("Hit by " + (enemy.isBoss() ? "boss" : "enemy") + " for " + damage + " damage!");
                    if (playerHealth <= 0) {
                        gameOver = true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error moving enemies: " + e.getMessage());
            errorMessage = "Error moving enemies!";
            showErrorDialog("Enemy Movement Error", "Failed to move enemies", e.getMessage());
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

    private void checkItemCollection() {
        try {
            Iterator<GoldDrop> goldIter = goldDrops.iterator();
            while (goldIter.hasNext()) {
                GoldDrop gold = goldIter.next();
                if (playerX == gold.getX() && playerY == gold.getY()) {
                    playerGold += gold.getAmount();
                    storyTeller.addStoryFragment("Collected " + gold.getAmount() + " gold!");
                    playSound(goldSound, "gold collection");
                    goldIter.remove();
                }
            }

            Iterator<HealthPotion> potionIter = healthPotionsOnMap.iterator();
            while (potionIter.hasNext()) {
                HealthPotion potion = potionIter.next();
                if (playerX == potion.getX() && playerY == potion.getY()) {
                    playerHealthPotions++;
                    storyTeller.addStoryFragment("Picked up a health potion!");
                    playSound(potionSound, "potion collection");
                    potionIter.remove();
                }
            }
            System.out.println("Player at: (" + playerX + "," + playerY + ")");
            goldDrops.forEach(g -> System.out.println("Gold at: (" + g.getX() + "," + g.getY() + ")"));
        } catch (Exception e) {
            System.err.println("Error checking item collection: " + e.getMessage());
            errorMessage = "Error collecting items!";
        }
    }

    private void useHealthPotion() {
        try {
            if (playerHealthPotions > 0) {
                playerHealth = Math.min(100, playerHealth + HEALTH_POTION_HEAL);
                playerHealthPotions--;
                playSound(potionSound, "potion");
                storyTeller.addStoryFragment("Used health potion! +" + HEALTH_POTION_HEAL + " HP");
                updateGame();
            } else {
                errorMessage = "No potions available!";
                updateGame();
            }
        } catch (Exception e) {
            System.err.println("Error using health potion: " + e.getMessage());
            errorMessage = "Error using potion!";
            updateGame();
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

            playSound(fireballSound, "fireball");

            int dx = 0, dy = 0;
            switch (lastDirection) {
                case 'W': dy = -1; break;
                case 'S': dy = 1; break;
                case 'A': dx = -1; break;
                case 'D': dx = 1; break;
                default: dx = 1; break;
            }

            fireballTrailPositions.clear();
            isFireballAnimating = true;
            fireballAnimationStartTime = System.currentTimeMillis();

            boolean hitEnemy = false;

            for (int i = 1; i <= FIREBALL_RANGE; i++) {
                int targetX = playerX + dx * i;
                int targetY = playerY + dy * i;

                if (targetX < 0 || targetX >= dungeonMap.getWidth() ||
                        targetY < 0 || targetY >= dungeonMap.getHeight()) {
                    System.out.println("Fireball stopped at map boundary");
                    break;
                }

                if (dungeonMap.getTileType(targetX, targetY) == TileType.WALL) {
                    System.out.println("Fireball stopped at wall at (" + targetX + "," + targetY + ")");
                    break;
                }

                double screenX = (targetX - playerX) * TILE_SIZE + mapCanvas.getWidth() / 2 - TILE_SIZE / 2;
                double screenY = (targetY - playerY) * TILE_SIZE + mapCanvas.getHeight() / 2 - TILE_SIZE / 2;
                fireballTrailPositions.add(new double[]{screenX + TILE_SIZE / 2.0, screenY + TILE_SIZE / 2.0});

                for (Enemy enemy : enemies) {
                    if (enemy != null && enemy.isAlive() &&
                            enemy.getX() == targetX && enemy.getY() == targetY) {
                        System.out.println("Hit enemy at (" + targetX + "," + targetY + ")");
                        int damage = 25;
                        if (i == 1) damage *= 2;
                        enemy.takeDamage(damage);
                        hitEnemy = true;
                        dungeonMap.recordPlayerAction("fireball");

                        if (!enemy.isAlive()) {
                            int goldAmount = enemy.getGoldValue();
                            goldDrops.add(new GoldDrop(enemy.getX(), enemy.getY(), goldAmount));
                            System.out.println("Gold drop created at (" + enemy.getX() + "," + enemy.getY() + ")");

                            enemiesKilled++;
                            if (enemy.isBoss() || enemiesKilled % 3 == 0) {
                                healthPotionsOnMap.add(new HealthPotion(enemy.getX(), enemy.getY()));
                                System.out.println("Potion drop created at (" + enemy.getX() + "," + enemy.getY() + ")");
                            }

                            if (enemy.isBoss()) {
                                bossKilled = true;
                                storyTeller.addKillStory(true);
                            } else {
                                regularEnemyKills++;
                                storyTeller.addKillStory(false);
                            }
                        }
                        break;
                    }
                }
                if (hitEnemy) break;
            }

            new AnimationTimer() {
                private long lastUpdate = 0;

                @Override
                public void handle(long now) {
                    if (now - lastUpdate >= 100_000_000) {
                        if (System.currentTimeMillis() - fireballAnimationStartTime < FIREBALL_DISPLAY_DURATION) {
                            updateGame();
                        } else {
                            isFireballAnimating = false;
                            fireballTrailPositions.clear();
                            this.stop();
                            updateGame();
                        }
                        lastUpdate = now;
                    }
                }
            }.start();

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
                    List<int[]> roomTiles = new ArrayList<>();
                    for (int x = 0; x < dungeonMap.getWidth(); x++) {
                        for (int y = 0; y < dungeonMap.getHeight(); y++) {
                            if (dungeonMap.getTileType(x, y) == TileType.ROOM) {
                                roomTiles.add(new int[]{x, y});
                            }
                        }
                    }
                    if (!roomTiles.isEmpty()) {
                        int[] keyPos = roomTiles.get(random.nextInt(roomTiles.size()));
                        dungeonMap.setKeyPosition(keyPos[0], keyPos[1]);
                        System.out.println("Key spawned at (" + keyPos[0] + "," + keyPos[1] + ")");
                    } else {
                        System.err.println("No rooms found for key placement!");
                        dungeonMap.setKeyPosition(playerX, playerY);
                    }
                }
            }
            if (hasKey && !keyCollected && dungeonMap != null &&
                    playerX == dungeonMap.getKeyX() && playerY == dungeonMap.getKeyY()) {
                keyCollected = true;
                playSound(keySound, "key pickup");
                if (storyTeller != null) {
                    storyTeller.addStoryFragment("You picked up the golden key!");
                }
                if (dungeonMap != null) {
                    dungeonMap.spawnDoorAtFarthestRoom(playerX, playerY);
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking key pickup: " + e.getMessage());
            errorMessage = "Error with key pickup!";
            showErrorDialog("Key Pickup Error", "Failed to handle key pickup", e.getMessage());
        }
    }

    private void checkLevelTransition() {
        try {
            if (hasKey && keyCollected && dungeonMap != null &&
                    dungeonMap.getTileType(playerX, playerY) == TileType.LEVEL_UP_DOOR) {
                playSound(doorSound, "door transition");
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
            showErrorDialog("Level Transition Error", "Failed to advance level", e.getMessage());
        }
    }

    private void restartLevel() {
        try {
            playerHealth = 100;
            gameOver = false;
            errorMessage = null;
            keyCollected = false;
            hasKey = false;
            regularEnemyKills = 0;
            bossKilled = false;
            goldDrops.clear();
            healthPotionsOnMap.clear();
            enemies.clear();
            findPlayerStartPosition();
            spawnEnemies();
            gameOverButtons.setVisible(false);
            updateGame();
        } catch (Exception e) {
            System.err.println("Error restarting level: " + e.getMessage());
            errorMessage = "Error restarting level!";
            gameOver = true;
            showErrorDialog("Restart Error", "Failed to restart level", e.getMessage());
            updateGame();
        }
    }

    private void startNewGame() {
        try {
            currentLevel = 1;
            playerHealth = 100;
            gameOver = false;
            errorMessage = null;
            keyCollected = false;
            hasKey = false;
            regularEnemyKills = 0;
            bossKilled = false;
            playerGold = 0;
            playerHealthPotions = 0;
            enemiesKilled = 0;
            goldDrops.clear();
            healthPotionsOnMap.clear();
            initializeLevel();
            gameOverButtons.setVisible(false);
            updateGame();
        } catch (Exception e) {
            System.err.println("Error starting new game: " + e.getMessage());
            errorMessage = "Error starting new game!";
            gameOver = true;
            showErrorDialog("New Game Error", "Failed to start new game", e.getMessage());
            updateGame();
        }
    }

    private void saveGame() {
        try (PrintWriter writer = new PrintWriter(new File("savegame.txt"))) {
            writer.println(currentLevel);
            writer.println(playerX);
            writer.println(playerY);
            writer.println(playerHealth);
            writer.println(playerGold);
            writer.println(playerHealthPotions);
            writer.println(enemiesKilled);
            writer.println(regularEnemyKills);
            writer.println(bossKilled);
            writer.println(hasKey);
            writer.println(keyCollected);

            writer.println(enemies.size());
            for (Enemy enemy : enemies) {
                writer.println(enemy.getX() + " " + enemy.getY() + " " +
                        (enemy.isAlive() ? 1 : 0) + " " +
                        enemy.isBoss() + " " + enemy.getHealth());
            }

            writer.println(goldDrops.size());
            for (GoldDrop gold : goldDrops) {
                writer.println(gold.getX() + " " + gold.getY() + " " +
                        gold.getAmount() + " " + gold.getSpawnTime());
            }

            writer.println(healthPotionsOnMap.size());
            for (HealthPotion potion : healthPotionsOnMap) {
                writer.println(potion.getX() + " " + potion.getY() + " " +
                        potion.getSpawnTime());
            }

            dungeonMap.save(writer);

            storyTeller.addStoryFragment("Game saved successfully!");
            updateGame();
        } catch (Exception e) {
            System.err.println("Error saving game: " + e.getMessage());
            errorMessage = "Failed to save game!";
            showErrorDialog("Save Error", "Failed to save game", e.getMessage());
            updateGame();
        }
    }

    private void loadGame() {
        try (Scanner scanner = new Scanner(new File("savegame.txt"))) {
            currentLevel = scanner.nextInt();
            playerX = scanner.nextInt();
            playerY = scanner.nextInt();
            playerHealth = scanner.nextInt();
            playerGold = scanner.nextInt();
            playerHealthPotions = scanner.nextInt();
            enemiesKilled = scanner.nextInt();
            regularEnemyKills = scanner.nextInt();
            bossKilled = scanner.nextBoolean();
            hasKey = scanner.nextBoolean();
            keyCollected = scanner.nextBoolean();

            enemies.clear();
            int enemyCount = scanner.nextInt();
            for (int i = 0; i < enemyCount; i++) {
                int x = scanner.nextInt();
                int y = scanner.nextInt();
                boolean alive = scanner.nextInt() == 1;
                boolean isBoss = scanner.nextBoolean();
                int health = scanner.nextInt();
                Enemy enemy = new Enemy(x, y, health, isBoss);
                enemy.setAlive(alive);
                enemies.add(enemy);
            }

            goldDrops.clear();
            int goldCount = scanner.nextInt();
            for (int i = 0; i < goldCount; i++) {
                int x = scanner.nextInt();
                int y = scanner.nextInt();
                int amount = scanner.nextInt();
                long spawnTime = scanner.nextLong();
                goldDrops.add(new GoldDrop(x, y, amount));
            }

            healthPotionsOnMap.clear();
            int potionCount = scanner.nextInt();
            for (int i = 0; i < potionCount; i++) {
                int x = scanner.nextInt();
                int y = scanner.nextInt();
                long spawnTime = scanner.nextLong();
                healthPotionsOnMap.add(new HealthPotion(x, y));
            }

            dungeonMap = new DungeonMap(40, 30);
            dungeonMap.load(scanner);

            storyTeller = new StoryTeller(dungeonMap);
            storyTeller.addStoryFragment("Game loaded successfully!");
            gameOver = false;
            gameOverButtons.setVisible(false);
            updateGame();
        } catch (Exception e) {
            System.err.println("Error loading game: " + e.getMessage());
            errorMessage = "Failed to load game!";
            showErrorDialog("Load Error", "Failed to load game", e.getMessage());
            startNewGame();
        }
    }

    private void updateGame() {
        try {
            long currentTime = System.currentTimeMillis();
            goldDrops.removeIf(gold -> currentTime - gold.getSpawnTime() > 30000);
            healthPotionsOnMap.removeIf(potion -> currentTime - potion.getSpawnTime() > 30000);
            System.out.println("Current gold drops: " + goldDrops.size());
            System.out.println("Current potions: " + healthPotionsOnMap.size());
            renderMap();
            renderSidebar();
            if (storyTeller != null && dungeonMap != null) {
                storyTeller.update(playerX, playerY);
            }
            gameOverButtons.setVisible(gameOver);
        } catch (Exception e) {
            System.err.println("Error updating game: " + e.getMessage());
            errorMessage = "Error updating game!";
            showErrorDialog("Update Error", "Failed to update game", e.getMessage());
            renderSidebar();
        }
    }

    private void renderMap() {
        try {
            if (!resourcesLoaded) {
                renderLoadingScreen();
                return;
            }

            GraphicsContext gc = mapCanvas.getGraphicsContext2D();
            if (gc == null) {
                throw new IllegalStateException("GraphicsContext is null");
            }

            clearCanvas(gc);
            renderVisibleTiles(gc);
            renderEntities(gc);
            renderFireballAnimation(gc);
        } catch (Exception e) {
            System.err.println("Error rendering map: " + e.getMessage());
            errorMessage = "Error rendering map!";
            showErrorDialog("Render Error", "Failed to render map", e.getMessage());
        }
    }

    private void renderLoadingScreen() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillText("Loading resources...", 50, 50);
    }

    private void clearCanvas(GraphicsContext gc) {
        double canvasWidth = mapCanvas.getWidth();
        double canvasHeight = mapCanvas.getHeight();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvasWidth, canvasHeight);
    }

    private void renderVisibleTiles(GraphicsContext gc) {
        double canvasWidth = mapCanvas.getWidth();
        double canvasHeight = mapCanvas.getHeight();
        int visibleTilesX = (int)(canvasWidth / TILE_SIZE) + 2;
        int visibleTilesY = (int)(canvasHeight / TILE_SIZE) + 2;
        int startX = Math.max(0, playerX - visibleTilesX / 2);
        int startY = Math.max(0, playerY - visibleTilesY / 2);
        int endX = Math.min(dungeonMap.getWidth(), startX + visibleTilesX);
        int endY = Math.min(dungeonMap.getHeight(), startY + visibleTilesY);

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                double screenX = (x - playerX) * TILE_SIZE + canvasWidth / 2 - TILE_SIZE / 2;
                double screenY = (y - playerY) * TILE_SIZE + canvasHeight / 2 - TILE_SIZE / 2;

                switch (dungeonMap.getTileType(x, y)) {
                    case WALL:
                        gc.setFill(Color.rgb(50, 50, 50));
                        gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                        break;
                    case FLOOR:
                        gc.setFill(Color.rgb(100, 100, 100));
                        gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                        break;
                    case ROOM:
                        gc.setFill(Color.rgb(150, 150, 150));
                        gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                        break;
                    case DOOR:
                        if (doorImage != null) {
                            gc.drawImage(doorImage, screenX, screenY, TILE_SIZE, TILE_SIZE);
                        } else {
                            gc.setFill(Color.rgb(139, 69, 19));
                            gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                        }
                        break;
                    case LEVEL_UP_DOOR:
                        if (hasKey && keyCollected && levelDoorImage != null) {
                            gc.drawImage(levelDoorImage, screenX, screenY, TILE_SIZE, TILE_SIZE);
                        } else {
                            gc.setFill(Color.rgb(100, 50, 150));
                            gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                        }
                        break;
                }

                gc.setStroke(Color.rgb(30, 30, 30));
                gc.strokeRect(screenX, screenY, TILE_SIZE, TILE_SIZE);

                if (dungeonMap.isExplored(x, y)) {
                    gc.setFill(Color.rgb(255, 255, 255, 0.1));
                    gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private void renderEntities(GraphicsContext gc) {
        double canvasWidth = mapCanvas.getWidth();
        double canvasHeight = mapCanvas.getHeight();
        int visibleTilesX = (int)(canvasWidth / TILE_SIZE) + 2;
        int visibleTilesY = (int)(canvasHeight / TILE_SIZE) + 2;

        // Render enemies
        if (enemies != null) {
            for (Enemy enemy : enemies) {
                if (enemy != null && enemy.isAlive()) {
                    if (Math.abs(enemy.getX() - playerX) <= visibleTilesX / 2 &&
                            Math.abs(enemy.getY() - playerY) <= visibleTilesY / 2) {
                        double screenX = (enemy.getX() - playerX) * TILE_SIZE + canvasWidth / 2 - TILE_SIZE / 2;
                        double screenY = (enemy.getY() - playerY) * TILE_SIZE + canvasHeight / 2 - TILE_SIZE / 2;
                        enemy.render(gc, screenX, screenY);
                    }
                }
            }
        }

        // Update gold frame
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > FRAME_DELAY) {
            currentGoldFrame = (currentGoldFrame + 1) % goldCoinFrames.length;
            lastFrameTime = currentTime;
        }

        // Render gold drops
        for (GoldDrop gold : goldDrops) {
            double screenX = (gold.getX() - playerX) * TILE_SIZE + canvasWidth / 2 - TILE_SIZE / 2;
            double screenY = (gold.getY() - playerY) * TILE_SIZE + canvasHeight / 2 - TILE_SIZE / 2;
            gold.render(gc, screenX, screenY);
        }

        // Render health potions
        for (HealthPotion potion : healthPotionsOnMap) {
            double screenX = (potion.getX() - playerX) * TILE_SIZE + canvasWidth / 2 - TILE_SIZE / 2;
            double screenY = (potion.getY() - playerY) * TILE_SIZE + canvasHeight / 2 - TILE_SIZE / 2;
            potion.render(gc, screenX, screenY);
        }

        // Render key
        if (hasKey && !keyCollected && dungeonMap.getKeyX() != -1 && dungeonMap.getKeyY() != -1) {
            double screenX = (dungeonMap.getKeyX() - playerX) * TILE_SIZE + canvasWidth / 2 - TILE_SIZE / 2;
            double screenY = (dungeonMap.getKeyY() - playerY) * TILE_SIZE + canvasHeight / 2 - TILE_SIZE / 2;
            if (keyImage != null) {
                gc.drawImage(keyImage, screenX, screenY, TILE_SIZE, TILE_SIZE);
            } else {
                gc.setFill(Color.YELLOW);
                gc.fillRect(screenX + TILE_SIZE / 4, screenY + TILE_SIZE / 4, TILE_SIZE / 2, TILE_SIZE / 2);
            }
        }

        // Render player
        gc.setFill(Color.RED);
        gc.fillOval(canvasWidth / 2 - TILE_SIZE / 2, canvasHeight / 2 - TILE_SIZE / 2, TILE_SIZE, TILE_SIZE);
    }

    private void renderFireballAnimation(GraphicsContext gc) {
        if (isFireballAnimating && !fireballTrailPositions.isEmpty()) {
            long elapsed = System.currentTimeMillis() - fireballAnimationStartTime;
            if (elapsed > FIREBALL_DISPLAY_DURATION) {
                isFireballAnimating = false;
                fireballTrailPositions.clear();
            } else {
                int dotsToDraw = Math.min(fireballTrailPositions.size(), (int)(elapsed / 100) + 1);
                for (int i = 0; i < dotsToDraw; i++) {
                    double[] pos = fireballTrailPositions.get(i);
                    renderFireblast(gc, pos[0], pos[1]);
                }
            }
        }
    }

    private void renderFireblast(GraphicsContext gc, double x, double y) {
        try {
            if (fireblastImage != null) {
                gc.drawImage(fireblastImage, x - 16, y - 16, 32, 32);
                System.out.println("Drawing fireblast image");
            } else {
                gc.setFill(Color.ORANGE);
                gc.fillOval(x - 16, y - 16, 32, 32);
                gc.setFill(Color.YELLOW);
                gc.fillOval(x - 12, y - 12, 24, 24);
                gc.setFill(Color.WHITE);
                gc.fillOval(x - 8, y - 8, 16, 16);
                System.err.println("Fireblast image not loaded - using fallback");
            }
        } catch (Exception e) {
            System.err.println("Error rendering fireblast: " + e.getMessage());
        }
    }

    private void renderSidebar() {
        try {
            GraphicsContext gc = sidebarCanvas.getGraphicsContext2D();
            if (gc == null) {
                throw new IllegalStateException("Sidebar GraphicsContext is null");
            }

            double sidebarWidth = sidebarCanvas.getWidth();
            double canvasHeight = sidebarCanvas.getHeight();

            gc.setFill(Color.rgb(20, 20, 30));
            gc.fillRect(0, 0, sidebarWidth, canvasHeight);

            Font headerFont = Font.font("Courier New", FontWeight.BOLD, 20);
            Font sectionFont = Font.font("Courier New", FontWeight.BOLD, 16);
            Font textFont = Font.font("Courier New", FontWeight.NORMAL, 14);

            gc.setFont(headerFont);
            gc.setFill(Color.GOLD);
            gc.fillText("DUNGEON JOURNAL", 25, 40);
            gc.setStroke(Color.GOLD);
            gc.strokeLine(25, 45, sidebarWidth - 25, 45);

            int yPos = 70;
            gc.setFont(sectionFont);
            gc.setFill(Color.CYAN);
            gc.fillText("PLAYER STATS", 25, yPos);
            yPos += 30;

            gc.setFont(textFont);
            gc.setFill(Color.WHITE);
            gc.fillText("Level: " + currentLevel, 35, yPos);
            yPos += 25;
            gc.fillText("Position: (" + playerX + "," + playerY + ")", 35, yPos);
            yPos += 25;
            gc.fillText("Health: " + playerHealth, 35, yPos);
            yPos += 25;
            gc.setFill(Color.GOLD);
            gc.fillText("Gold: " + playerGold, 35, yPos);
            yPos += 25;
            gc.setFill(Color.RED.brighter());
            gc.fillText("Potions: " + playerHealthPotions, 35, yPos);
            yPos += 40;

            gc.setFont(sectionFont);
            gc.setFill(Color.CYAN);
            gc.fillText("COMBAT LOG", 25, yPos);
            yPos += 30;

            gc.setFont(textFont);
            gc.setFill(Color.WHITE);
            gc.fillText("Enemies Killed: " + regularEnemyKills, 35, yPos);
            yPos += 25;
            gc.fillText("Bosses Killed: " + (bossKilled ? "Yes" : "No"), 35, yPos);
            yPos += 40;

            gc.setFont(sectionFont);
            gc.setFill(Color.CYAN);
            gc.fillText("STORY EVENTS", 25, yPos);
            yPos += 30;

            if (storyTeller != null) {
                List<String> fragments = storyTeller.getCurrentStoryFragments();
                int maxLines = (int)(canvasHeight - yPos - 150) / 20;

                gc.setFont(textFont);
                for (int i = Math.max(0, fragments.size() - maxLines); i < fragments.size(); i++) {
                    String fragment = fragments.get(i);

                    if (i >= fragments.size() - 3) {
                        gc.setFill(Color.LIGHTYELLOW);
                    } else {
                        gc.setFill(Color.LIGHTGRAY);
                    }

                    String[] words = fragment.split(" ");
                    StringBuilder line = new StringBuilder("> ");
                    for (String word : words) {
                        if (gc.getFont().getSize() * (line.length() + word.length()) < sidebarWidth - 50) {
                            line.append(word).append(" ");
                        } else {
                            gc.fillText(line.toString(), 35, yPos);
                            yPos += 20;
                            if (yPos > canvasHeight - 150) break;
                            line = new StringBuilder("  ").append(word).append(" ");
                        }
                    }
                    gc.fillText(line.toString(), 35, yPos);
                    yPos += 20;
                    if (yPos > canvasHeight - 150) break;
                }
            }

            gc.setFont(sectionFont);
            gc.setFill(Color.CYAN);
            gc.fillText("ACTIONS", 25, canvasHeight - 120);

            gc.setFont(textFont);
            gc.setFill(Color.WHITE);
            gc.fillText("Moves: " + dungeonMap.getActionCount("move"), 35, canvasHeight - 95);
            gc.fillText("Fireballs: " + dungeonMap.getActionCount("fireball"), 35, canvasHeight - 70);

            if (gameOver) {
                gc.setFill(Color.RED);
                gc.setFont(Font.font("Courier New", FontWeight.BOLD, 24));
                if (playerHealth <= 0) {
                    gc.fillText("GAME OVER", sidebarWidth/2 - 70, canvasHeight/2);
                } else {
                    gc.fillText("VICTORY!", sidebarWidth/2 - 50, canvasHeight/2);
                }

                gc.setFont(sectionFont);
                gc.setFill(Color.CYAN);
                gc.fillText("OPTIONS", 25, canvasHeight/2 + 30);

                gc.setFont(textFont);
                gc.setFill(Color.WHITE);
                gc.fillText("R: Restart Level", 35, canvasHeight/2 + 55);
                gc.fillText("N: New Game", 35, canvasHeight/2 + 80);
                gc.fillText("Q: Quit Game", 35, canvasHeight/2 + 105);
            }

            // Updated Controls Section
            gc.setFont(sectionFont);
            gc.setFill(Color.LIMEGREEN);
            gc.fillText("CONTROLS", 25, canvasHeight - 45);
            gc.setFont(textFont);
            gc.setFill(Color.WHITE);
            gc.fillText("WASD/Arrow: Move  Space: Fireball  H: Potion  O: Save  L: Load", 35, canvasHeight - 25);

            if (errorMessage != null) {
                gc.setFill(Color.RED);
                gc.setFont(Font.font("Courier New", FontWeight.BOLD, 16));
                gc.fillText(errorMessage, 25, canvasHeight/2 + 135);
            }
        } catch (Exception e) {
            System.err.println("Error rendering sidebar: " + e.getMessage());
            showErrorDialog("Sidebar Render Error", "Failed to render sidebar", e.getMessage());
        }
    }

    class GoldDrop extends Entity {
        private final int amount;
        private final long spawnTime;

        public GoldDrop(int x, int y, int amount) {
            super(x, y);
            this.amount = amount;
            this.spawnTime = System.currentTimeMillis();
        }

        public int getAmount() { return amount; }
        public long getSpawnTime() { return spawnTime; }

        @Override
        public void render(GraphicsContext gc, double screenX, double screenY) {
            try {
                if (goldCoinFrames[0] != null && currentGoldFrame < goldCoinFrames.length) {
                    gc.drawImage(goldCoinFrames[currentGoldFrame], screenX, screenY, TILE_SIZE, TILE_SIZE);
                    System.out.println("Drawing gold frame " + currentGoldFrame);
                } else {
                    gc.setFill(Color.GOLD);
                    gc.fillOval(screenX, screenY, TILE_SIZE, TILE_SIZE);
                    System.err.println("Gold frames not loaded - using fallback");
                }
                gc.setFill(Color.BLACK);
                gc.fillText("" + amount, screenX + TILE_SIZE/2 - 5, screenY + TILE_SIZE/2 + 5);
            } catch (Exception e) {
                System.err.println("Error rendering gold drop at (" + x + "," + y + "): " + e.getMessage());
            }
        }
    }

    class HealthPotion extends Entity {
        private final long spawnTime;

        public HealthPotion(int x, int y) {
            super(x, y);
            this.spawnTime = System.currentTimeMillis();
        }

        public long getSpawnTime() { return spawnTime; }

        @Override
        public void render(GraphicsContext gc, double screenX, double screenY) {
            try {
                if (potionImage != null) {
                    gc.drawImage(potionImage, screenX, screenY, TILE_SIZE, TILE_SIZE);
                } else {
                    gc.setFill(Color.RED);
                    gc.fillRect(screenX, screenY, TILE_SIZE/2, TILE_SIZE);
                    gc.setFill(Color.WHITE);
                    gc.fillRect(screenX + TILE_SIZE/4, screenY + TILE_SIZE/4, TILE_SIZE/4, TILE_SIZE/2);
                }
            } catch (Exception e) {
                System.err.println("Error rendering health potion at (" + x + "," + y + "): " + e.getMessage());
            }
        }
    }
}