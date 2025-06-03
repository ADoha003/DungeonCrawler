package DungeonCrawler;

public enum TileType {
    WALL(0, "Wall"),
    FLOOR(1, "Floor"),
    ROOM(2, "Room"),
    DOOR(3, "Door"),
    LEVEL_UP_DOOR(4, "Level Up Door");

    private final int value;
    private final String name;

    TileType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() { return value; }
    public String getName() { return name; }
}