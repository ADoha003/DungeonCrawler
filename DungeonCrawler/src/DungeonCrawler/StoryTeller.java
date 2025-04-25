package DungeonCrawler;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles dynamic storytelling based on player actions and dungeon state
 */
public class StoryTeller {
    private DungeonMap dungeonMap;
    private List<String> storyFragments;
    
    public StoryTeller(DungeonMap dungeonMap) {
        this.dungeonMap = dungeonMap;
        this.storyFragments = new ArrayList<>();
    }
    
    public void update(int playerX, int playerY) {
        storyFragments.clear();
        
        // Check for room-specific stories
        checkRoomStories(playerX, playerY);
        
        // Check for action-based stories
        checkActionStories();
        
        // Check for idle stories
        checkIdleStories();
    }
    
    private void checkRoomStories(int x, int y) {
        long visitTime = dungeonMap.getRoomVisitTime(x, y);
        long timeInRoom = System.currentTimeMillis() - visitTime;
        
        if (timeInRoom > 5000 && dungeonMap.getTileType(x, y) == TileType.ROOM) {
            storyFragments.add("This room feels... familiar. Like you've been here before in another life.");
        }
        
        if (dungeonMap.getTileType(x, y) == TileType.DOOR) {
            storyFragments.add("The door creaks ominously as you approach...");
        }
    }
    
    private void checkActionStories() {
        int kills = dungeonMap.getActionCount("kill");
        if (kills > 10) {
            storyFragments.add("The dungeon seems to recoil at your violence...");
        }
        
        int treasures = dungeonMap.getActionCount("treasure");
        if (treasures > 5) {
            storyFragments.add("You feel the weight of your treasures attracting unwanted attention...");
        }
    }
    
    private void checkIdleStories() {
        long timeSinceLastAction = System.currentTimeMillis() - dungeonMap.getLastActionTime();
        if (timeSinceLastAction > 10000) {
            storyFragments.add("The dungeon whispers to you... 'Why do you hesitate?'");
        }
    }
    
    public List<String> getCurrentStoryFragments() {
        return new ArrayList<>(storyFragments);
    }
}