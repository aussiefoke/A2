package builder;

import builder.entities.npc.NpcManager;
import builder.entities.npc.enemies.EnemyManager;
import builder.inventory.Inventory;
import builder.player.Player;
import builder.world.World;

/**
 * An implementation of the game state for the JavaBean game. Stores the world, player, and
 * inventory.
 *
 * @hint As with {@link GameState}, you can create this class incrementally through each stage.
 */
public class JavaBeanGameState implements GameState {
    private final World world;
    private final Player player;
    private final Inventory inventory;
    private final NpcManager npcs;
    private final EnemyManager enemies;

    /**
     * Construct a new instance storing the given world, player, and inventory.
     *
     * @param world The world of the game.
     * @param player The player of the game.
     * @param inventory The inventory of the player.
     */
    public JavaBeanGameState(
            World world,
            Player player,
            Inventory inventory,
            NpcManager npcs,
            EnemyManager enemies) {
        this.world = world;
        this.player = player;
        this.inventory = inventory;
        this.npcs = npcs;
        this.enemies = enemies;
    }

    public NpcManager getNpcs() {
        return this.npcs;
    }

    @Override
    public EnemyManager getEnemies() {
        return this.enemies;
    }

    /**
     * @stage2
     */
    @Override
    public World getWorld() {
        return world;
    }

    /**
     * @stage1
     */
    @Override
    public Player getPlayer() {
        return player;
    }

    /**
     * @stage3
     */
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
