package DungeonCrawler;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class DungeonMap {
    private int width;
    private int height;
    private TileType[][] grid;
    private boolean[][] explored;
    private int keyX = -1;
    private int keyY = -1;
    private Map<String, Integer> actionCounts = new HashMap<>();
    private Map<String, Long> actionTimestamps = new HashMap<>();
    private long[][] visitTimes;

    public DungeonMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new TileType[width][height];
        this.explored = new boolean[width][height];
        this.visitTimes = new long[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = TileType.WALL;
                explored[x][y] = false;
                visitTimes[x][y] = 0;
            }
        }
    }

    public DungeonMap(TileType[][] grid) {
        this.width = grid.length;
        this.height = grid[0].length;
        this.grid = grid;
        this.explored = new boolean[width][height];
        this.visitTimes = new long[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                explored[x][y] = false;
                visitTimes[x][y] = 0;
            }
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public TileType getTileType(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return grid[x][y];
        }
        return TileType.WALL;
    }

    public void setTileType(int x, int y, TileType type) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid[x][y] = type;
        }
    }

    public boolean isExplored(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return explored[x][y];
        }
        return false;
    }

    public void recordRoomVisit(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            explored[x][y] = true;
            visitTimes[x][y] = System.currentTimeMillis();
        }
    }

    public long getRoomVisitTime(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return visitTimes[x][y];
        }
        return 0;
    }

    public void recordPlayerAction(String action) {
        actionCounts.put(action, actionCounts.getOrDefault(action, 0) + 1);
        actionTimestamps.put(action, System.currentTimeMillis());
    }

    public int getActionCount(String action) {
        return actionCounts.getOrDefault(action, 0);
    }

    public long getLastActionTime() {
        return actionTimestamps.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(System.currentTimeMillis());
    }

    public void setKeyPosition(int x, int y) {
        this.keyX = x;
        this.keyY = y;
    }

    public int getKeyX() { return keyX; }
    public int getKeyY() { return keyY; }

    public void spawnDoorAtFarthestRoom(int playerX, int playerY) {
        int[] farthestRoom = findFarthestRoom(playerX, playerY);
        if (farthestRoom != null) {
            int roomX = farthestRoom[0];
            int roomY = farthestRoom[1];
            
            boolean doorPlaced = false;
            if (roomX > 0 && getTileType(roomX - 1, roomY) == TileType.WALL) {
                setTileType(roomX - 1, roomY, TileType.LEVEL_UP_DOOR);
                doorPlaced = true;
            } else if (roomX < width - 1 && getTileType(roomX + 1, roomY) == TileType.WALL) {
                setTileType(roomX + 1, roomY, TileType.LEVEL_UP_DOOR);
                doorPlaced = true;
            } else if (roomY > 0 && getTileType(roomX, roomY - 1) == TileType.WALL) {
                setTileType(roomX, roomY - 1, TileType.LEVEL_UP_DOOR);
                doorPlaced = true;
            } else if (roomY < height - 1 && getTileType(roomX, roomY + 1) == TileType.WALL) {
                setTileType(roomX, roomY + 1, TileType.LEVEL_UP_DOOR);
                doorPlaced = true;
            }
            
            if (!doorPlaced) {
                setTileType(roomX, roomY, TileType.LEVEL_UP_DOOR);
            }
            System.out.println("Spawning level-up door at (" + roomX + "," + roomY + ")");
        } else {
            System.err.println("No valid room found to spawn level-up door!");
        }
    }

    private int[] findFarthestRoom(int playerX, int playerY) {
        int[] farthest = null;
        double maxDistance = 0;
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (getTileType(x, y) == TileType.ROOM) {
                    double dist = Math.sqrt(Math.pow(x - playerX, 2) + Math.pow(y - playerY, 2));
                    if (dist > maxDistance) {
                        maxDistance = dist;
                        farthest = new int[]{x, y};
                    }
                }
            }
        }
        return farthest;
    }

    public void save(PrintWriter writer) {
        try {
            writer.println(width);
            writer.println(height);
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    writer.print(grid[x][y].ordinal() + " ");
                }
                writer.println();
            }
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    writer.print(explored[x][y] ? 1 : 0 + " ");
                }
                writer.println();
            }
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    writer.print(visitTimes[x][y] + " ");
                }
                writer.println();
            }
            
            writer.println(keyX + " " + keyY);
            
            writer.println(actionCounts.size());
            for (Map.Entry<String, Integer> entry : actionCounts.entrySet()) {
                writer.println(entry.getKey() + " " + entry.getValue());
            }
            
            writer.println(actionTimestamps.size());
            for (Map.Entry<String, Long> entry : actionTimestamps.entrySet()) {
                writer.println(entry.getKey() + " " + entry.getValue());
            }
        } catch (Exception e) {
            System.err.println("Error saving map: " + e.getMessage());
        }
    }

    public void load(Scanner scanner) {
        try {
            int newWidth = scanner.nextInt();
            int newHeight = scanner.nextInt();
            
            this.width = newWidth;
            this.height = newHeight;
            this.grid = new TileType[width][height];
            this.explored = new boolean[width][height];
            this.visitTimes = new long[width][height];
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    grid[x][y] = TileType.values()[scanner.nextInt()];
                }
            }
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    explored[x][y] = scanner.nextInt() == 1;
                }
            }
            
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    visitTimes[x][y] = scanner.nextLong();
                }
            }
            
            keyX = scanner.nextInt();
            keyY = scanner.nextInt();
            
            actionCounts.clear();
            int countSize = scanner.nextInt();
            for (int i = 0; i < countSize; i++) {
                String key = scanner.next();
                int value = scanner.nextInt();
                actionCounts.put(key, value);
            }
            
            actionTimestamps.clear();
            int timeSize = scanner.nextInt();
            for (int i = 0; i < timeSize; i++) {
                String key = scanner.next();
                long value = scanner.nextLong();
                actionTimestamps.put(key, value);
            }
        } catch (Exception e) {
            System.err.println("Error loading map: " + e.getMessage());
        }
    }
}