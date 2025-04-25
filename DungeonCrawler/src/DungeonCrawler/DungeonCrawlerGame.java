package DungeonCrawler;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class DungeonCrawlerGame extends Application {
    private static final int TILE_SIZE = 32;
    private static final int MAP_WIDTH = 800;
    private static final int MAP_HEIGHT = 600;
    private static final int SIDEBAR_WIDTH = 300;
    
    private DungeonMap dungeonMap;
    private StoryTeller storyTeller;
    private int playerX, playerY;
    private Canvas mapCanvas;
    private Canvas sidebarCanvas;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize game
        DungeonGenerator generator = new DungeonGenerator(40, 30, System.currentTimeMillis());
        dungeonMap = generator.generate();
        storyTeller = new StoryTeller(dungeonMap);
        
        // Find starting position
        findPlayerStartPosition();

        // Create JavaFX UI
        mapCanvas = new Canvas(MAP_WIDTH, MAP_HEIGHT);
        sidebarCanvas = new Canvas(SIDEBAR_WIDTH, MAP_HEIGHT);
        
        BorderPane root = new BorderPane();
        root.setLeft(mapCanvas);
        root.setRight(sidebarCanvas);
        
        Scene scene = new Scene(root, MAP_WIDTH + SIDEBAR_WIDTH, MAP_HEIGHT);
        
        // Handle keyboard input
        scene.setOnKeyPressed(event -> {
            int newX = playerX;
            int newY = playerY;
            
            switch (event.getCode()) {
                case UP: case W: newY--; break;
                case DOWN: case S: newY++; break;
                case LEFT: case A: newX--; break;
                case RIGHT: case D: newX++; break;
                default: break;
            }
            
            if (isValidMove(newX, newY)) {
                playerX = newX;
                playerY = newY;
                dungeonMap.recordRoomVisit(playerX, playerY);
                dungeonMap.recordPlayerAction("move");
                updateGame();
            }
        });

        primaryStage.setTitle("Dungeon Crawler - Procedural Adventure");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        updateGame();
    }

    private void findPlayerStartPosition() {
        boolean foundStart = false;
        for (int x = 0; x < dungeonMap.getWidth() && !foundStart; x++) {
            for (int y = 0; y < dungeonMap.getHeight() && !foundStart; y++) {
                if (dungeonMap.getTileType(x, y) == TileType.FLOOR) {
                    playerX = x;
                    playerY = y;
                    foundStart = true;
                }
            }
        }
    }

    private boolean isValidMove(int x, int y) {
        return x >= 0 && x < dungeonMap.getWidth() && 
               y >= 0 && y < dungeonMap.getHeight() && 
               dungeonMap.getTileType(x, y) != TileType.WALL;
    }

    private void updateGame() {
        renderMap();
        renderSidebar();
        storyTeller.update(playerX, playerY);
    }

    private void renderMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, MAP_WIDTH, MAP_HEIGHT);
        
        // Calculate visible area around player
        int startX = Math.max(0, playerX - MAP_WIDTH/TILE_SIZE/2);
        int startY = Math.max(0, playerY - MAP_HEIGHT/TILE_SIZE/2);
        int endX = Math.min(dungeonMap.getWidth(), playerX + MAP_WIDTH/TILE_SIZE/2);
        int endY = Math.min(dungeonMap.getHeight(), playerY + MAP_HEIGHT/TILE_SIZE/2);

        // Draw visible dungeon area
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                int screenX = (x - playerX) * TILE_SIZE + MAP_WIDTH/2 - TILE_SIZE/2;
                int screenY = (y - playerY) * TILE_SIZE + MAP_HEIGHT/2 - TILE_SIZE/2;
                
                // Set color based on tile type
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
                        gc.setFill(Color.rgb(139, 69, 19)); // Brown
                        break;
                }
                
                // Draw tile
                gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                
                // Draw grid
                gc.setStroke(Color.rgb(30, 30, 30));
                gc.strokeRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                
                // Highlight explored rooms
                if (dungeonMap.isExplored(x, y)) {
                    gc.setFill(Color.rgb(255, 255, 255, 0.1));
                    gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                }
            }
        }
        
        // Draw player
        gc.setFill(Color.RED);
        gc.fillOval(
            MAP_WIDTH/2 - TILE_SIZE/2, 
            MAP_HEIGHT/2 - TILE_SIZE/2, 
            TILE_SIZE, 
            TILE_SIZE
        );
    }

    private void renderSidebar() {
        GraphicsContext gc = sidebarCanvas.getGraphicsContext2D();
        gc.setFill(Color.rgb(30, 30, 40));
        gc.fillRect(0, 0, SIDEBAR_WIDTH, MAP_HEIGHT);
        
        // Set font for text
        gc.setFont(Font.font("Courier New", 14));
        gc.setFill(Color.WHITE);
        
        // Draw title
        gc.setFont(Font.font("Courier New", 18));
        gc.fillText("Dungeon Journal", 20, 30);
        gc.setStroke(Color.GOLD);
        gc.strokeLine(20, 35, SIDEBAR_WIDTH - 20, 35);
        
        // Reset font
        gc.setFont(Font.font("Courier New", 14));
        
        // Draw current position
        gc.fillText(String.format("Position: (%d, %d)", playerX, playerY), 20, 60);
        
        // Draw current room type
        String roomType = "";
        switch (dungeonMap.getTileType(playerX, playerY)) {
            case FLOOR: roomType = "Corridor"; break;
            case ROOM: roomType = "Room"; break;
            case DOOR: roomType = "Doorway"; break;
            default: roomType = "Unknown"; break;
        }
        gc.fillText("Location: " + roomType, 20, 80);
        
        // Draw story fragments
        gc.fillText("Story Events:", 20, 120);
        int yPos = 140;
        for (String fragment : storyTeller.getCurrentStoryFragments()) {
            gc.setFill(Color.LIGHTGOLDENRODYELLOW);
            gc.fillText("> " + fragment, 30, yPos);
            yPos += 20;
            if (yPos > MAP_HEIGHT - 40) break;
        }
        
        // Draw player stats
        gc.setFill(Color.WHITE);
        gc.fillText("Player Actions:", 20, MAP_HEIGHT - 80);
        gc.fillText("Moves: " + dungeonMap.getActionCount("move"), 30, MAP_HEIGHT - 60);
        
        // Draw controls
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("Controls:", 20, MAP_HEIGHT - 30);
        gc.fillText("WASD or Arrows to move", 30, MAP_HEIGHT - 10);
    }
}