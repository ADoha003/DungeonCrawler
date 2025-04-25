package DungeonCrawler;



import java.util.Random;

/**
 * Generates a procedural 2D dungeon map using recursive backtracking
 */
public class DungeonGenerator {
    private Random random;
    private int width;
    private int height;
    
    public DungeonGenerator(int width, int height, long seed) {
        this.width = width;
        this.height = height;
        this.random = new Random(seed);
    }
    
    public DungeonMap generate() {
        TileType[][] map = new TileType[width][height];
        
        // Initialize all as walls
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map[x][y] = TileType.WALL;
            }
        }
        
        // Start carving from the center
        carve(map, width/2, height/2);
        
        // Add some rooms
        addRooms(map, 5 + random.nextInt(5));
        
        return new DungeonMap(map);
    }
    
    private void carve(TileType[][] map, int x, int y) {
        if (x <= 0 || x >= width-1 || y <= 0 || y >= height-1) return;
        if (map[x][y] != TileType.WALL) return;
        
        // Count adjacent floors
        int adjacent = 0;
        if (map[x+1][y] == TileType.FLOOR) adjacent++;
        if (map[x-1][y] == TileType.FLOOR) adjacent++;
        if (map[x][y+1] == TileType.FLOOR) adjacent++;
        if (map[x][y-1] == TileType.FLOOR) adjacent++;
        
        if (adjacent > 1) return;
        
        map[x][y] = TileType.FLOOR;
        
        // Randomize direction
        int[] dirs = {0, 1, 2, 3};
        shuffleArray(dirs);
        
        for (int dir : dirs) {
            switch (dir) {
                case 0: carve(map, x+1, y); break;
                case 1: carve(map, x-1, y); break;
                case 2: carve(map, x, y+1); break;
                case 3: carve(map, x, y-1); break;
            }
        }
    }
    
    private void addRooms(TileType[][] map, int roomCount) {
        for (int i = 0; i < roomCount; i++) {
            int roomWidth = 3 + random.nextInt(5);
            int roomHeight = 3 + random.nextInt(5);
            int x = 1 + random.nextInt(width - roomWidth - 2);
            int y = 1 + random.nextInt(height - roomHeight - 2);
            
            // Create room
            for (int rx = x; rx < x + roomWidth; rx++) {
                for (int ry = y; ry < y + roomHeight; ry++) {
                    map[rx][ry] = TileType.ROOM;
                }
            }
            
            // Connect to nearest corridor
            connectRoom(map, x + roomWidth/2, y + roomHeight/2);
        }
    }
    
    private void connectRoom(TileType[][] map, int roomX, int roomY) {
        // Find nearest floor
        int nearestX = roomX;
        int nearestY = roomY;
        double nearestDist = Double.MAX_VALUE;
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (map[x][y] == TileType.FLOOR) {
                    double dist = Math.sqrt(Math.pow(x - roomX, 2) + Math.pow(y - roomY, 2));
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestX = x;
                        nearestY = y;
                    }
                }
            }
        }
        
        // Create corridor
        int x = roomX;
        int y = roomY;
        while (x != nearestX || y != nearestY) {
            if (x < nearestX) x++;
            else if (x > nearestX) x--;
            
            if (y < nearestY) y++;
            else if (y > nearestY) y--;
            
            if (map[x][y] == TileType.WALL) {
                map[x][y] = TileType.FLOOR;
            }
        }
    }
    
    private void shuffleArray(int[] array) {
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
}