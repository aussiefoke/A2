package builder;

import builder.entities.npc.NpcManager;
import builder.entities.npc.enemies.EnemyManager;
import builder.tiles.Tile;
import builder.inventory.Inventory;
import builder.player.Player;
import builder.world.World;

/**
 * An interface to the game state information, including world, player, and inventory data.
 *
 * @stage1
 * @hint The state will eventually need to include the World and Player. In stage 1, you will only
 *     need it to include the Player and in stage 2, you only need World.
 */
public interface GameState {
    /**
     * Returns the current state of the game world.
     *
     * <p>The returned world is mutable, that is, calling mutator methods such as {@link
     * World#place(Tile)} will modify the world.
     *
     * @return The game world.
     * @stage2
     */
    World getWorld();

    NpcManager getNpcs();

    EnemyManager getEnemies();

    /**
     * Returns the current state of the player. Useful for retrieving the player's location.
     *
     * @return The player of the game.
     * @stage1
     */
    Player getPlayer();

    /**
     * Returns the current state of the inventory.
     *
     * <p>The returned inventory is mutable, that is calling mutator methods such as {@link
     * Inventory#addCoins(int)} will modify the inventory.
     *
     * @return The inventory of the player.
     */
    Inventory getInventory();
}
