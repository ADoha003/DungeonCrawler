package DungeonCrawler;



import java.util.HashMap;
import java.util.Map;

/**
 * Represents the dungeon map and tracks player exploration
 */
public class DungeonMap {
    private TileType[][] map;
    private Map<String, Integer> playerActions;
    private Map<String, Long> roomVisitTimes;
    private long lastActionTime;
    
    public DungeonMap(TileType[][] map) {
        this.map = map;
        this.playerActions = new HashMap<>();
        this.roomVisitTimes = new HashMap<>();
        this.lastActionTime = System.currentTimeMillis();
    }
    
    public int getWidth() {
        return map.length;
    }
    
    public int getHeight() {
        return map[0].length;
    }
    
    public int getTile(int x, int y) {
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return TileType.WALL.getValue(); // Wall if out of bounds
        }
        return map[x][y].getValue();
    }
    
    public TileType getTileType(int x, int y) {
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return TileType.WALL;
        }
        return map[x][y];
    }
    
    public void recordPlayerAction(String action) {
        playerActions.put(action, playerActions.getOrDefault(action, 0) + 1);
        lastActionTime = System.currentTimeMillis();
    }
    
    public void recordRoomVisit(int x, int y) {
        String key = x + "," + y;
        roomVisitTimes.put(key, System.currentTimeMillis());
    }
    
    public long getLastActionTime() {
        return lastActionTime;
    }
    
    public int getActionCount(String action) {
        return playerActions.getOrDefault(action, 0);
    }
    
    public long getRoomVisitTime(int x, int y) {
        String key = x + "," + y;
        return roomVisitTimes.getOrDefault(key, 0L);
    }
    
    public boolean isExplored(int x, int y) {
        String key = x + "," + y;
        return roomVisitTimes.containsKey(key);
    }
}