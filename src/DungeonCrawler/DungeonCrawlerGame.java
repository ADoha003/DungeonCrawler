package DungeonCrawler;
///////    https://github.com/ADoha003/DungeonCrawler/commits?author=ADoha003
/*
* week 7 make the Dungeon Game to be more flexbil to be full size and improve the treasure to be be only in room last some time and vanish
* and to make render Sidebar more active with the chat and more colorful
*/
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class DungeonCrawlerGame extends Application {
    private static final int TILE_SIZE = 32;
    private static final int BASE_MAP_WIDTH = 800;
    private static final int BASE_MAP_HEIGHT = 600;
    private static final int SIDEBAR_WIDTH = 550;
    private static final int MAX_LEVELS = 30;
    private static final int FIREBALL_RANGE = 3;
    private List<double[]> fireballTrailPositions = new ArrayList<>();
    private boolean isFireballAnimating = false;
    private long fireballAnimationStartTime = 0;
    private static final long FIREBALL_DISPLAY_DURATION = 600;
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
    private char lastDirection;
    private int regularEnemyKills;
    private boolean bossKilled;
    private List<GoldDrop> goldDrops = new ArrayList<>();
    private List<HealthPotion> healthPotionsOnMap = new ArrayList<>();
    private int playerGold = 0;
    private int playerHealthPotions = 0;
    private int enemiesKilled = 0;
    private static final int HEALTH_POTION_HEAL = 50;

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
            lastDirection = 'D';
            initializeLevel();

            mapCanvas = new Canvas();
            sidebarCanvas = new Canvas();

            BorderPane root = new BorderPane();
            root.setLeft(mapCanvas);
            root.setRight(sidebarCanvas);

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);

            scene.setOnKeyPressed(event -> {
                if (gameOver || event == null) return;
                System.out.println("Key pressed: " + event.getCode());
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
                        case H:
                            useHealthPotion();
                            break;
                        default:
                            break;
                    }

                    if (event.getCode() != KeyCode.SPACE && event.getCode() != KeyCode.H && isValidMove(newX, newY)) {
                        playerX = newX;
                        playerY = newY;
                        if (dungeonMap != null) {
                            dungeonMap.recordRoomVisit(playerX, playerY);
                            dungeonMap.recordPlayerAction("move");
                        }
                        moveEnemies();
                        checkKeyPickup();
                        checkItemCollection();
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
            goldDrops.clear();
            healthPotionsOnMap.clear();
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
        Iterator<GoldDrop> goldIter = goldDrops.iterator();
        while (goldIter.hasNext()) {
            GoldDrop gold = goldIter.next();
            if (playerX == gold.getX() && playerY == gold.getY()) {
                playerGold += gold.getAmount();
                storyTeller.addStoryFragment("Collected " + gold.getAmount() + " gold!");
                goldIter.remove();
            }
        }

        Iterator<HealthPotion> potionIter = healthPotionsOnMap.iterator();
        while (potionIter.hasNext()) {
            HealthPotion potion = potionIter.next();
            if (playerX == potion.getX() && playerY == potion.getY()) {
                playerHealthPotions++;
                storyTeller.addStoryFragment("Picked up a health potion!");
                potionIter.remove();
            }
        }
        System.out.println("Player at: " + playerX + "," + playerY);
        goldDrops.forEach(g -> System.out.println("Gold at: " + g.getX() + "," + g.getY()));
    }

    private void useHealthPotion() {
        if (playerHealthPotions > 0) {
            playerHealth = Math.min(100, playerHealth + HEALTH_POTION_HEAL);
            playerHealthPotions--;
            storyTeller.addStoryFragment("Used health potion! +" + HEALTH_POTION_HEAL + " HP");
            updateGame();
        } else {
            errorMessage = "No potions available!";
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

            GraphicsContext gc = mapCanvas.getGraphicsContext2D();
            if (gc == null) {
                System.out.println("GraphicsContext is null!");
                errorMessage = "Rendering error!";
                updateGame();
                return;
            }

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
                        if (i == FIREBALL_RANGE) damage *= 2;
                        enemy.takeDamage(damage);
                        hitEnemy = true;
                        dungeonMap.recordPlayerAction("fireball");
                        
                        if (!enemy.isAlive()) {
                            int goldAmount = enemy.isBoss() ? 50 : 10;
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

            double canvasWidth = mapCanvas.getWidth();
            double canvasHeight = mapCanvas.getHeight();

            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, canvasWidth, canvasHeight);

            int visibleTilesX = (int)(canvasWidth / TILE_SIZE) + 2;
            int visibleTilesY = (int)(canvasHeight / TILE_SIZE) + 2;

            int startX = Math.max(0, playerX - visibleTilesX / 2);
            int startY = Math.max(0, playerY - visibleTilesY / 2);
            int endX = Math.min(dungeonMap.getWidth(), playerX + visibleTilesX / 2);
            int endY = Math.min(dungeonMap.getHeight(), playerY + visibleTilesY / 2);

            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    double screenX = (x - playerX) * TILE_SIZE + canvasWidth / 2 - TILE_SIZE / 2;
                    double screenY = (y - playerY) * TILE_SIZE + canvasHeight / 2 - TILE_SIZE / 2;

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
                        if (Math.abs(enemy.getX() - playerX) <= visibleTilesX/2 && 
                            Math.abs(enemy.getY() - playerY) <= visibleTilesY/2) {
                            double screenX = (enemy.getX() - playerX) * TILE_SIZE + canvasWidth / 2 - TILE_SIZE / 2;
                            double screenY = (enemy.getY() - playerY) * TILE_SIZE + canvasHeight / 2 - TILE_SIZE / 2;
                            gc.setFill(enemy.isBoss() ? Color.PURPLE : Color.GREEN);
                            gc.fillRect(screenX + TILE_SIZE / 4, screenY + TILE_SIZE / 4, 
                                      TILE_SIZE / 2, TILE_SIZE / 2);
                        }
                    }
                }
            }

            gc.setFill(Color.GOLD.brighter().brighter());
            for (GoldDrop gold : goldDrops) {
                double screenX = (gold.getX() - playerX) * TILE_SIZE + mapCanvas.getWidth() / 2;
                double screenY = (gold.getY() - playerY) * TILE_SIZE + mapCanvas.getHeight() / 2;
                
                double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 200.0);
                gc.setFill(Color.rgb(255, (int)(215 * pulse), 0));
                gc.fillOval(screenX - TILE_SIZE/2, screenY - TILE_SIZE/2, TILE_SIZE, TILE_SIZE);
                
                gc.setFill(Color.BLACK);
                gc.fillText("" + gold.getAmount(), screenX - 5, screenY + 5);
            }

            gc.setFill(Color.RED.brighter());
            for (HealthPotion potion : healthPotionsOnMap) {
                double screenX = (potion.getX() - playerX) * TILE_SIZE + mapCanvas.getWidth() / 2;
                double screenY = (potion.getY() - playerY) * TILE_SIZE + mapCanvas.getHeight() / 2;
                
                gc.fillRect(screenX - TILE_SIZE/3, screenY - TILE_SIZE/2, TILE_SIZE/1.5, TILE_SIZE);
                gc.setFill(Color.WHITE);
                gc.fillText("H", screenX - 3, screenY + 5);
            }

            if (hasKey && dungeonMap.getKeyX() != -1 && dungeonMap.getKeyY() != -1) {
                double screenX = (dungeonMap.getKeyX() - playerX) * TILE_SIZE + canvasWidth / 2 - TILE_SIZE / 2;
                double screenY = (dungeonMap.getKeyY() - playerY) * TILE_SIZE + canvasHeight / 2 - TILE_SIZE / 2;
                gc.setFill(Color.YELLOW);
                gc.fillRect(screenX + TILE_SIZE / 4, screenY + TILE_SIZE / 4, TILE_SIZE / 2, TILE_SIZE / 2);
            }

            gc.setFill(Color.RED);
            gc.fillOval(
                canvasWidth / 2 - TILE_SIZE / 2,
                canvasHeight / 2 - TILE_SIZE / 2,
                TILE_SIZE,
                TILE_SIZE
            );

            if (isFireballAnimating && !fireballTrailPositions.isEmpty()) {
                long elapsed = System.currentTimeMillis() - fireballAnimationStartTime;
                if (elapsed > FIREBALL_DISPLAY_DURATION) {
                    isFireballAnimating = false;
                    fireballTrailPositions.clear();
                } else {
                    gc.setFill(Color.YELLOW);
                    int dotsToDraw = Math.min(fireballTrailPositions.size(), 
                        (int)(elapsed / 100) + 1);
                    for (int i = 0; i < dotsToDraw; i++) {
                        double[] pos = fireballTrailPositions.get(i);
                        gc.fillOval(pos[0] - 8, pos[1] - 8, 16, 16);
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

            double sidebarWidth = sidebarCanvas.getWidth();
            double canvasHeight = sidebarCanvas.getHeight();

            // Clear sidebar with darker background
            gc.setFill(Color.rgb(20, 20, 30));
            gc.fillRect(0, 0, sidebarWidth, canvasHeight);

            // Set default font
            Font headerFont = Font.font("Courier New", FontWeight.BOLD, 20);
            Font sectionFont = Font.font("Courier New", FontWeight.BOLD, 16);
            Font textFont = Font.font("Courier New", 14);

            // Header
            gc.setFont(headerFont);
            gc.setFill(Color.GOLD);
            gc.fillText("DUNGEON JOURNAL", 25, 40);
            gc.setStroke(Color.GOLD);
            gc.strokeLine(25, 45, sidebarWidth - 25, 45);

            // Player Stats Section
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

            // Combat Log Section
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

            // Story Events Section
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
                    
                    // Highlight recent events
                    if (i >= fragments.size() - 3) {
                        gc.setFill(Color.LIGHTYELLOW);
                    } else {
                        gc.setFill(Color.LIGHTGRAY);
                    }
                    
                    // Word wrap long messages
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

            // Player Actions Section (bottom)
            gc.setFont(sectionFont);
            gc.setFill(Color.CYAN);
            gc.fillText("ACTIONS", 25, canvasHeight - 120);
            
            gc.setFont(textFont);
            gc.setFill(Color.WHITE);
            gc.fillText("Moves: " + dungeonMap.getActionCount("move"), 35, canvasHeight - 95);
            gc.fillText("Fireballs: " + dungeonMap.getActionCount("fireball"), 35, canvasHeight - 70);
            // Removed the "Kills" line here

            // Controls Section (very bottom)
            gc.setFont(sectionFont);
            gc.setFill(Color.LIMEGREEN);
            gc.fillText("CONTROLS", 25, canvasHeight - 25);
            
            gc.setFont(textFont);
            gc.setFill(Color.LIGHTGRAY);
            gc.fillText("WASD/Arrows: Move", 35, canvasHeight - 5);
            gc.fillText("Space: Fireball", sidebarWidth/2, canvasHeight - 5);
            gc.fillText("H: Use Potion", sidebarWidth - 120, canvasHeight - 5);

            // Game over/error messages
            if (gameOver) {
                gc.setFill(Color.RED);
                gc.setFont(Font.font("Courier New", FontWeight.BOLD, 24));
                if (playerHealth <= 0) {
                    gc.fillText("GAME OVER", sidebarWidth/2 - 70, canvasHeight/2);
                } else {
                    gc.fillText("VICTORY!", sidebarWidth/2 - 50, canvasHeight/2);
                }
            }

            if (errorMessage != null) {
                gc.setFill(Color.RED);
                gc.setFont(Font.font("Courier New", FontWeight.BOLD, 16));
                gc.fillText(errorMessage, 25, canvasHeight/2 + 30);
            }
        } catch (Exception e) {
            System.err.println("Error rendering sidebar: " + e.getMessage());
        }
    }

    class GoldDrop {
        private int x, y;
        private int amount;
        private long spawnTime;

        public GoldDrop(int x, int y, int amount) {
            this.x = x;
            this.y = y;
            this.amount = amount;
            this.spawnTime = System.currentTimeMillis();
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getAmount() { return amount; }
        public long getSpawnTime() { return spawnTime; }
    }

    class HealthPotion {
        private int x, y;
        private long spawnTime;

        public HealthPotion(int x, int y) {
            this.x = x;
            this.y = y;
            this.spawnTime = System.currentTimeMillis();
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public long getSpawnTime() { return spawnTime; }
    }
}