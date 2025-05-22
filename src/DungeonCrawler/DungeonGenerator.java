package DungeonCrawler;

import java.util.Random;

public class DungeonGenerator {
    private Random random;
    private int width;
    private int height;

    public DungeonGenerator(int width, int height, long seed) {
        try {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Map dimensions must be positive");
            }
            this.width = width;
            this.height = height;
            this.random = new Random(seed);
        } catch (IllegalArgumentException e) {
            System.err.println("Error initializing DungeonGenerator: " + e.getMessage());
            this.width = 40;
            this.height = 30;
            this.random = new Random(seed);
        }
    }

    public DungeonMap generate() {
        try {
            TileType[][] map = new TileType[width][height];

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    map[x][y] = TileType.WALL;
                }
            }

            carve(map, width / 2, height / 2);
            addRooms(map, 5 + random.nextInt(5));
            addDoors(map);

            return new DungeonMap(map);
        } catch (Exception e) {
            System.err.println("Error generating dungeon: " + e.getMessage());
            TileType[][] fallbackMap = new TileType[40][30];
            for (int x = 0; x < 40; x++) {
                for (int y = 0; y < 30; y++) {
                    fallbackMap[x][y] = TileType.WALL;
                }
            }
            return new DungeonMap(fallbackMap);
        }
    }

    private void carve(TileType[][] map, int x, int y) {
        try {
            if (x <= 0 || x >= width - 1 || y <= 0 || y >= height - 1) return;
            if (map[x][y] != TileType.WALL) return;

            int adjacent = 0;
            if (map[x + 1][y] == TileType.FLOOR) adjacent++;
            if (map[x - 1][y] == TileType.FLOOR) adjacent++;
            if (map[x][y + 1] == TileType.FLOOR) adjacent++;
            if (map[x][y - 1] == TileType.FLOOR) adjacent++;

            if (adjacent > 1) return;

            map[x][y] = TileType.FLOOR;

            int[] dirs = {0, 1, 2, 3};
            shuffleArray(dirs);

            for (int dir : dirs) {
                switch (dir) {
                    case 0:
                        carve(map, x + 1, y);
                        break;
                    case 1:
                        carve(map, x - 1, y);
                        break;
                    case 2:
                        carve(map, x, y + 1);
                        break;
                    case 3:
                        carve(map, x, y - 1);
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error carving map at (" + x + "," + y + "): " + e.getMessage());
        }
    }

    private void addRooms(TileType[][] map, int roomCount) {
        try {
            for (int i = 0; i < roomCount; i++) {
                int roomWidth = 5 + random.nextInt(3); // Larger rooms for enemies
                int roomHeight = 5 + random.nextInt(3);
                int x = 1 + random.nextInt(width - roomWidth - 2);
                int y = 1 + random.nextInt(height - roomHeight - 2);

                for (int rx = x; rx < x + roomWidth; rx++) {
                    for (int ry = y; ry < y + roomHeight; ry++) {
                        map[rx][ry] = TileType.ROOM;
                    }
                }

                connectRoom(map, x + roomWidth / 2, y + roomHeight / 2);
            }
        } catch (Exception e) {
            System.err.println("Error adding rooms: " + e.getMessage());
        }
    }

    private void addDoors(TileType[][] map) {
        try {
            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                    if (map[x][y] == TileType.FLOOR) {
                        int roomCount = 0;
                        int floorCount = 0;
                        if (map[x + 1][y] == TileType.ROOM) roomCount++;
                        else if (map[x + 1][y] == TileType.FLOOR) floorCount++;
                        if (map[x - 1][y] == TileType.ROOM) roomCount++;
                        else if (map[x - 1][y] == TileType.FLOOR) floorCount++;
                        if (map[x][y + 1] == TileType.ROOM) roomCount++;
                        else if (map[x][y + 1] == TileType.FLOOR) floorCount++;
                        if (map[x][y - 1] == TileType.ROOM) roomCount++;
                        else if (map[x][y - 1] == TileType.FLOOR) floorCount++;
                        if (roomCount > 0 && floorCount > 0) {
                            map[x][y] = TileType.DOOR;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error adding doors: " + e.getMessage());
        }
    }

    private void connectRoom(TileType[][] map, int roomX, int roomY) {
        try {
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
        } catch (Exception e) {
            System.err.println("Error connecting room: " + e.getMessage());
        }
    }

    private void shuffleArray(int[] array) {
        try {
            for (int i = array.length - 1; i > 0; i--) {
                int index = random.nextInt(i + 1);
                int temp = array[index];
                array[index] = array[i];
                array[i] = temp;
            }
        } catch (Exception e) {
            System.err.println("Error shuffling array: " + e.getMessage());
        }
    }
}