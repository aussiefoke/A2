package builder.entities.resources;

import builder.GameState;
import builder.entities.Interactable;
import builder.tiles.Dirt;
import builder.ui.SpriteGallery;

import engine.EngineState;
import engine.art.sprites.SpriteGroup;
import engine.game.Entity;

/**
 * An entity planted (stacked on) {@link Dirt} that grows and can be
 * collected by the player once grown. A cabbage is initially rendered as 'default' within {@link
 * SpriteGallery#cabbage}.
 *
 * @stage3
 */
public class Cabbage extends Entity implements Interactable {

    private static final SpriteGroup art = SpriteGallery.cabbage;

    // 0..4: default -> budding -> growing -> grown -> collectable
    private int growthState = 0;

    // advance one stage every 100 ticks
    private static final int GROWTH_INTERVAL_TICKS = 100;
    private int ticksSinceStage = 0;

    /** The cost of planting a cabbage, 2 coins. */
    public static final int COST = 2;

    /**
     * Construct a new cabbage entity at the given x, y position.
     *
     * <p>Initially the cabbage is rendered as 'default' within {@link SpriteGallery#cabbage}.
     *
     * @requires x >= 0, x is less than the window width
     * @requires y >= 0, y is less than the window height
     * @param x The x-axis (horizontal) coordinate.
     * @param y The y-axis (vertical) coordinate.
     */
    public Cabbage(int x, int y) {
        super(x, y);
        this.setSprite(art.getSprite("default"));
    }

    /**
     * Progress the state of the cabbage, updating how it is rendered as required.
     *
     * <p>The cabbage should progress through the following sprites in {@link
     * SpriteGallery#cabbage}: 'default', 'budding', 'growing', 'grown', and finally 'collectable'.
     *
     * <p>The cabbage should transition into its next state every 100 ticks.
     *
     * @hint To track cabbage state transitions, you may find {@link
     * GROWTH_INTERVAL_TICKS} helpful.
     */
    @Override
    public void tick(EngineState state) {

        if (growthState < 4) {
            ticksSinceStage += 1;
            if (ticksSinceStage >= GROWTH_INTERVAL_TICKS) {
                growthState += 1;
                ticksSinceStage = 0;
                updateArt();
            }
        }
    }

    /** Updates the displayed art of this entity based on the given progress value. */
    private void updateArt() {
        this.setSprite(
                art.getSprite(
                        switch (this.growthState) {
                            case 0 -> "default";
                            case 1 -> "budding";
                            case 2 -> "growing";
                            case 3 -> "grown";
                            default -> "collectable";
                        }));
    }

    /**
     * Handle collecting a fully grown cabbage. When the player interacts with a fully grown cabbage
     * the cost of the cabbage should be added to the player's food, 3 coins should be added to the
     * player's inventory, and the cabbage should be removed from the game.
     *
     * @param state The state of the engine, including the mouse, keyboard information and
     *     dimension. Useful for processing keyboard presses or mouse movement. Note that for
     *     left-click behaviour, {@link builder.entities.Usable} should be used instead.
     * @param game The state of the game, including the player and world. Can be used to query or
     *     update the game state.
     */
    @Override
    public void interact(EngineState state, GameState game) {
        if (this.growthState >= 4) {
            game.getInventory().addFood(COST);
            game.getInventory().addCoins(3);
            this.markForRemoval();
        }
    }
}
