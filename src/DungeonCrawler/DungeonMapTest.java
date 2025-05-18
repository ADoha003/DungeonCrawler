package DungeonCrawler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DungeonMapTest {
    @Test
    void testPlayerActions() {
        TileType[][] testMap = new TileType[10][10];
        DungeonMap map = new DungeonMap(testMap);
        
        assertEquals(0, map.getActionCount("kill"));
        map.recordPlayerAction("kill");
        assertEquals(1, map.getActionCount("kill"));
    }
    
    @Test
    void testRoomVisits() {
        TileType[][] testMap = new TileType[10][10];
        DungeonMap map = new DungeonMap(testMap);
        
        assertFalse(map.isExplored(5, 5));
        map.recordRoomVisit(5, 5);
        assertTrue(map.isExplored(5, 5));
    }
}