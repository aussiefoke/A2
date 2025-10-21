package builder.world;

import engine.game.HasPosition;

public interface SpawnerDetails extends HasPosition {
    public int getX();

    public int getY();

    public void setX(int x);

    public void setY(int y);

    public int getDuration();
}
