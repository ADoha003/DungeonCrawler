package DungeonCrawler;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Enemy extends Entity {
    private int health;
    private boolean isBoss;
    private int goldValue;

    public Enemy(int x, int y, int health, boolean isBoss) {
        super(x, y);
        try {
            if (health <= 0) {
                throw new IllegalArgumentException("Enemy health must be positive");
            }
            this.health = health;
            this.isBoss = isBoss;
            this.goldValue = isBoss ? 50 : 10;
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating enemy: " + e.getMessage());
            this.health = isBoss ? 100 : 50;
            this.goldValue = isBoss ? 50 : 10;
        }
    }

    @Override
    public void render(GraphicsContext gc, double screenX, double screenY) {
        try {
            gc.setFill(isBoss ? Color.PURPLE : Color.GREEN);
            gc.fillRect(screenX, screenY, DungeonCrawlerGame.TILE_SIZE, DungeonCrawlerGame.TILE_SIZE);
        } catch (Exception e) {
            System.err.println("Error rendering enemy at (" + x + "," + y + "): " + e.getMessage());
        }
    }

    public int getHealth() { return health; }
    public void setHealth(int health) {
        this.health = health;
        if (health <= 0) {
            alive = false;
        }
    }
    public boolean isBoss() { return isBoss; }
    public int getGoldValue() { return goldValue; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public void takeDamage(int damage) {
        try {
            if (damage < 0) {
                throw new IllegalArgumentException("Damage cannot be negative");
            }
            health -= damage;
            if (health <= 0) {
                alive = false;
                System.out.println("Enemy defeated at (" + x + "," + y + ")");
            }
        } catch (Exception e) {
            System.err.println("Error applying damage: " + e.getMessage());
        }
    }
}