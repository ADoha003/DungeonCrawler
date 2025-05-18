package DungeonCrawler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DungeonGeneratorTest {
    @Test
    void testDungeonGeneration() {
        DungeonGenerator generator = new DungeonGenerator(50, 50, 12345);
        DungeonMap map = generator.generate();
        
        assertNotNull(map);
        assertEquals(50, map.getWidth());
        assertEquals(50, map.getHeight());
        
        // Verify there are some floors
        boolean hasFloors = false;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.getTile(x, y) != TileType.WALL.getValue()) {
                    hasFloors = true;
                    break;
                }
            }
            if (hasFloors) break;
        }
        assertTrue(hasFloors);
    }
}