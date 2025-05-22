package DungeonCrawler;

import java.util.HashMap;
import java.util.Map;

public class DungeonMap {
    private TileType[][] map;
    private Map<String, Integer> playerActions;
    private Map<String, Long> roomVisitTimes;
    private long lastActionTime;
    private int keyX, keyY;

    public DungeonMap(TileType[][] map) {
        try {
            if (map == null || map.length == 0 || map[0].length == 0) {
                throw new IllegalArgumentException("Invalid map dimensions");
            }
            this.map = map;
            this.playerActions = new HashMap<>();
            this.roomVisitTimes = new HashMap<>();
            this.lastActionTime = System.currentTimeMillis();
            this.keyX = -1;
            this.keyY = -1;
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating dungeon map: " + e.getMessage());
            this.map = new TileType[10][10];
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    this.map[x][y] = TileType.WALL;
                }
            }
            this.playerActions = new HashMap<>();
            this.roomVisitTimes = new HashMap<>();
            this.lastActionTime = System.currentTimeMillis();
            this.keyX = -1;
            this.keyY = -1;
        }
    }

    public int getWidth() {
        try {
            return map.length;
        } catch (Exception e) {
            System.err.println("Error getting map width: " + e.getMessage());
            return 10;
        }
    }

    public int getHeight() {
        try {
            return map[0].length;
        } catch (Exception e) {
            System.err.println("Error getting map height: " + e.getMessage());
            return 10;
        }
    }

    public int getTile(int x, int y) {
        try {
            if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
                return TileType.WALL.getValue();
            }
            return map[x][y].getValue();
        } catch (Exception e) {
            System.err.println("Error getting tile at (" + x + "," + y + "): " + e.getMessage());
            return TileType.WALL.getValue();
        }
    }

    public TileType getTileType(int x, int y) {
        try {
            if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
                return TileType.WALL;
            }
            return map[x][y];
        } catch (Exception e) {
            System.err.println("Error getting tile type at (" + x + "," + y + "): " + e.getMessage());
            return TileType.WALL;
        }
    }

    public void recordPlayerAction(String action) {
        try {
            if (action == null) {
                throw new IllegalArgumentException("Action cannot be null");
            }
            playerActions.put(action, playerActions.getOrDefault(action, 0) + 1);
            lastActionTime = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("Error recording player action: " + e.getMessage());
        }
    }

    public void recordRoomVisit(int x, int y) {
        try {
            String key = x + "," + y;
            roomVisitTimes.put(key, System.currentTimeMillis());
        } catch (Exception e) {
            System.err.println("Error recording room visit: " + e.getMessage());
        }
    }

    public long getLastActionTime() {
        return lastActionTime;
    }

    public int getActionCount(String action) {
        try {
            return playerActions.getOrDefault(action, 0);
        } catch (Exception e) {
            System.err.println("Error getting action count: " + e.getMessage());
            return 0;
        }
    }

    public long getRoomVisitTime(int x, int y) {
        try {
            String key = x + "," + y;
            return roomVisitTimes.getOrDefault(key, 0L);
        } catch (Exception e) {
            System.err.println("Error getting room visit time: " + e.getMessage());
            return 0L;
        }
    }

    public boolean isExplored(int x, int y) {
        try {
            String key = x + "," + y;
            return roomVisitTimes.containsKey(key);
        } catch (Exception e) {
            System.err.println("Error checking if explored: " + e.getMessage());
            return false;
        }
    }

    public void setKeyPosition(int x, int y) {
        try {
            if (x < 0 || y < 0) {
                throw new IllegalArgumentException("Invalid key position");
            }
            this.keyX = x;
            this.keyY = y;
        } catch (Exception e) {
            System.err.println("Error setting key position: " + e.getMessage());
            this.keyX = -1;
            this.keyY = -1;
        }
    }

    public int getKeyX() {
        return keyX;
    }

    public int getKeyY() {
        return keyY;
    }
}