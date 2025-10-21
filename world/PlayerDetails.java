package builder.world;

import engine.game.ImmutablePosition;

public interface PlayerDetails extends ImmutablePosition {
    int getStartingFood();

    int getStartingCoins();
}
