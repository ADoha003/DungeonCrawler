package DungeonCrawler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StoryTellerTest {
    @Test
    void testStoryGeneration() {
        TileType[][] testMap = new TileType[10][10];
        DungeonMap map = new DungeonMap(testMap);
        StoryTeller teller = new StoryTeller(map);
        
        // Test initial state
        assertTrue(teller.getCurrentStoryFragments().isEmpty());
        
        // Test room visit story
        map.recordRoomVisit(5, 5);
        try { Thread.sleep(50); } catch (InterruptedException e) {} // Small delay
        teller.update(5, 5);
        assertTrue(teller.getCurrentStoryFragments().isEmpty()); // Not enough time
        
        // Test action story
        map.recordPlayerAction("kill");
        map.recordPlayerAction("kill");
        map.recordPlayerAction("kill");
        teller.update(5, 5);
        assertFalse(teller.getCurrentStoryFragments().contains("The dungeon seems to recoil at your violence..."));
        
        // Add more kills to trigger the story
        for (int i = 0; i < 10; i++) {
            map.recordPlayerAction("kill");
        }
        teller.update(5, 5);
        assertTrue(teller.getCurrentStoryFragments().contains("The dungeon seems to recoil at your violence..."));
    }
}