package DungeonCrawler;

import java.util.ArrayList;
import java.util.List;

public class StoryTeller {
    private DungeonMap dungeonMap;
    private List<String> storyFragments;

    public StoryTeller(DungeonMap dungeonMap) {
        try {
            if (dungeonMap == null) {
                throw new IllegalArgumentException("DungeonMap cannot be null");
            }
            this.dungeonMap = dungeonMap;
            this.storyFragments = new ArrayList<>();
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating StoryTeller: " + e.getMessage());
            this.dungeonMap = null;
            this.storyFragments = new ArrayList<>();
        }
    }

    public void update(int playerX, int playerY) {
        try {
            if (storyFragments == null) {
                storyFragments = new ArrayList<>();
            }
            storyFragments.clear();

            if (dungeonMap == null) {
                storyFragments.add("Error: Dungeon map not loaded!");
                return;
            }

            checkRoomStories(playerX, playerY);
            checkActionStories();
            checkIdleStories();
        } catch (Exception e) {
            System.err.println("Error updating story: " + e.getMessage());
            storyFragments.add("Error generating story!");
        }
    }

    private void checkRoomStories(int x, int y) {
        try {
            long visitTime = dungeonMap.getRoomVisitTime(x, y);
            long timeInRoom = System.currentTimeMillis() - visitTime;

            if (timeInRoom > 5000 && dungeonMap.getTileType(x, y) == TileType.ROOM) {
                storyFragments.add("This room feels... familiar. Like you've been here before in another life.");
            }

            if (dungeonMap.getTileType(x, y) == TileType.DOOR) {
                storyFragments.add("The door creaks ominously as you approach...");
            }
        } catch (Exception e) {
            System.err.println("Error checking room stories: " + e.getMessage());
        }
    }

    private void checkActionStories() {
        try {
            int kills = dungeonMap.getActionCount("kill");
            if (kills > 10) {
                storyFragments.add("The dungeon seems to recoil at your violence...");
            }

            int damages = dungeonMap.getActionCount("damage");
            if (damages > 5) {
                storyFragments.add("Your wounds ache as the dungeon's malice grows...");
            }
        } catch (Exception e) {
            System.err.println("Error checking action stories: " + e.getMessage());
        }
    }

    private void checkIdleStories() {
        try {
            long timeSinceLastAction = System.currentTimeMillis() - dungeonMap.getLastActionTime();
            if (timeSinceLastAction > 10000) {
                storyFragments.add("The dungeon whispers to you... 'Why do you hesitate?'");
            }
        } catch (Exception e) {
            System.err.println("Error checking idle stories: " + e.getMessage());
        }
    }

    public void addKillStory() {
        try {
            if (storyFragments == null) {
                storyFragments = new ArrayList<>();
            }
            storyFragments.add("An enemy falls before you, its life extinguished.");
        } catch (Exception e) {
            System.err.println("Error adding kill story: " + e.getMessage());
        }
    }

    public void addKeyStory() {
        try {
            if (storyFragments == null) {
                storyFragments = new ArrayList<>();
            }
            storyFragments.add("With the last enemy defeated, a golden key appears!");
        } catch (Exception e) {
            System.err.println("Error adding key story: " + e.getMessage());
        }
    }

    public List<String> getCurrentStoryFragments() {
        try {
            if (storyFragments == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(storyFragments);
        } catch (Exception e) {
            System.err.println("Error getting story fragments: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}