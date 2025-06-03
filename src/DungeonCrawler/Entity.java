package DungeonCrawler;

import javafx.scene.canvas.GraphicsContext;

public abstract class Entity {
    protected int x, y;
    protected boolean alive = true;

    public Entity(int x, int y) {
        try {
            if (x < 0 || y < 0) {
                throw new IllegalArgumentException("Entity coordinates cannot be negative");
            }
            this.x = x;
            this.y = y;
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating entity: " + e.getMessage());
            this.x = 0;
            this.y = 0;
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isAlive() { return alive; }

    public void setPosition(int x, int y) {
        try {
            if (x < 0 || y < 0) {
                throw new IllegalArgumentException("Invalid position coordinates");
            }
            this.x = x;
            this.y = y;
        } catch (IllegalArgumentException e) {
            System.err.println("Error setting position: " + e.getMessage());
        }
    }

    public abstract void render(GraphicsContext gc, double screenX, double screenY);
}