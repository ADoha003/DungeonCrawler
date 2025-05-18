package DungeonCrawler;

public class Enemy {
    private int x, y;
    private int health;
    private boolean alive;

    public Enemy(int x, int y, int health) {
        try {
            if (health <= 0) {
                throw new IllegalArgumentException("Enemy health must be positive");
            }
            this.x = x;
            this.y = y;
            this.health = health;
            this.alive = true;
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating enemy: " + e.getMessage());
            this.health = 50; // Default health
            this.x = x;
            this.y = y;
            this.alive = true;
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        try {
            this.x = x;
            this.y = y;
        } catch (Exception e) {
            System.err.println("Error setting enemy position: " + e.getMessage());
        }
    }

    public boolean isAlive() {
        return alive;
    }

    public void takeDamage(int damage) {
        try {
            if (damage < 0) {
                throw new IllegalArgumentException("Damage cannot be negative");
            }
            health -= damage;
            if (health <= 0) {
                alive = false;
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error applying damage: " + e.getMessage());
        }
    }
}