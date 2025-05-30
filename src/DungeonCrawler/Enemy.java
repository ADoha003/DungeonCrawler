package DungeonCrawler;

public class Enemy {
    private int x, y;
    private int health;
    private boolean alive;
    private boolean isBoss;
    private int goldValue;
    public Enemy(int x, int y, int health, boolean isBoss) {
        try {
            if (health <= 0) {
                throw new IllegalArgumentException("Enemy health must be positive");
            }
            this.x = x;
            this.y = y;
            this.health = health;
            this.alive = true;
            this.isBoss = isBoss;
        } catch (IllegalArgumentException e) {
            System.err.println("Error creating enemy: " + e.getMessage());
            this.health = isBoss ? 100 : 50;
            this.x = x;
            this.y = y;
            this.alive = true;
            this.isBoss = isBoss;
            this.goldValue = isBoss ? 50 : 10;
        }
    }
    public int getGoldValue() {
        return goldValue;
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

    public boolean isBoss() {
        return isBoss;
    }

    public void takeDamage(int damage) {
        try {
            if (damage < 0) {
                throw new IllegalArgumentException("Damage cannot be negative");
            }
            health -= damage;
            if (health <= 0) {
                alive = false;
                System.out.println("Enemy defeated at (" + x + "," + y + ")");
            } else {
                System.out.println("Enemy at (" + x + "," + y + ") took " + damage + " damage. Remaining health: " + health);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error applying damage: " + e.getMessage());
        }
    }
}