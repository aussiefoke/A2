package builder.entities.resources;

import builder.GameState;
import builder.entities.Usable;
import builder.inventory.items.Jackhammer;
import builder.player.Player;
import builder.tiles.OreVein;
import builder.ui.SpriteGallery;

import engine.EngineState;
import engine.art.sprites.SpriteGroup;
import engine.game.Entity;

/**
 * An entity that is stacked on an {@link OreVein} and yields coins when
 * mined. The ore initially has 10 coins and can be mined by the player using the jackhammer. The
 * ore is initially rendered as 'default' within {@link SpriteGallery#rock}.
 *
 * @stage3
 */
public class Ore extends Entity implements Usable {

    private static final SpriteGroup art = SpriteGallery.rock;

    // total value in coins; visuals depend on the remaining ratio
    private static final int COIN_VALUE = 10;

    // mining cadence: player can extract at most once per N ticks
    private static final int MINE_CADENCE_TICKS = 5;

    private int coins = COIN_VALUE;

    // next tick at/after which mining is allowed again
    private int nextMineTick = 0;

    /**
     * Construct a new ore entity at the given x, y position.
     *
     * <p>Initially the ore is rendered as 'default' within {@link SpriteGallery#rock}.
     *
     * @requires x >= 0, x is less than the window width
     * @requires y >= 0, y is less than the window height
     * @param x The x-axis (horizontal) coordinate.
     * @param y The y-axis (vertical) coordinate.
     */
    public Ore(int x, int y) {
        super(x, y);
        this.setSprite(art.getSprite("default"));
    }

    /**
     * Progress the state of the ore, updating the sprite to render.
     *
     * <p>If the ore has greater than 90% of its original value remaining then it should remain
     * rendered using 'default'. If the ore has less than or equal to 90% of its original value
     * remaining but more than 10%, it should be rendered using 'damaged' in {@link
     * SpriteGallery#rock}. Otherwise, if the ore has less than or equal to 10% remaining, it should
     * be rendered with 'depleted' in {@link SpriteGallery#rock}.
     */
    @Override
    public void tick(EngineState state) {

        final double remainingRatio = (double) coins / COIN_VALUE;
        if (remainingRatio > 0.9) {
            this.setSprite(art.getSprite("default"));
        } else if (remainingRatio > 0.1) {
            this.setSprite(art.getSprite("damaged"));
        } else {
            this.setSprite(art.getSprite("depleted"));
        }
    }

    /**
     * When a jackhammer is used on an ore, it takes damage and the player collects coins from it.
     *
     * <p>If the following conditions are met: i) The player is holding a jackhammer, ii) we are
     * allowed by the cadence timer (every 5 ticks), iii) the remaining value of the ore is greater
     * than zero, then the amount of damage dealt by the player ({@link Player#getDamage()}) is
     * subtracted from the ore's value and added as coins to the player's inventory (capped at the
     * remaining ore value).
     */
    @Override
    public void use(EngineState state, GameState game) {
        if (coins <= 0) return;

        final int now = state.currentTick();
        if (now < nextMineTick) return;

        final Object held = game.getInventory().getHolding();
        if (!(held instanceof Jackhammer)) return;

        final int collection = Math.min(this.coins, game.getPlayer().getDamage());
        if (collection > 0) {
            this.coins -= collection;
            game.getInventory().addCoins(collection);
            nextMineTick = now + MINE_CADENCE_TICKS; // enforce 5-tick cadence
        }
    }
}
