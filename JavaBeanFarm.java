package builder;

import builder.entities.npc.NpcManager;
import builder.entities.npc.enemies.EnemyManager;
import builder.entities.npc.spawners.EagleSpawner;
import builder.entities.npc.spawners.MagpieSpawner;
import builder.entities.npc.spawners.PigeonSpawner;
import builder.tiles.Dirt;
import builder.tiles.Tile;
import builder.inventory.*;
import builder.inventory.items.Bucket;
import builder.inventory.items.HiveHammer;
import builder.inventory.items.Hoe;
import builder.inventory.items.Jackhammer;
import builder.inventory.items.Pole;
import builder.inventory.ui.InventoryOverlay;
import builder.inventory.ui.ResourceOverlay;
import builder.player.PlayerManager;
import builder.ui.Overlay;
import builder.world.BeanWorld;
import builder.world.CabbageDetails;
import builder.world.OverlayBuilder;
import builder.world.PlayerDetails;
import builder.world.SpawnerDetails;
import builder.world.WorldBuilder;
import builder.world.WorldLoadException;

import engine.EngineState;
import engine.game.Game;
import engine.renderer.Dimensions;
import engine.renderer.Renderable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * JavaBeans, a farming game.
 *
 * <p>In this game, the player collects coins by mining ores. The player, a chicken farmer, may use
 * those coins to plant cabbages on tilled dirt.
 *
 * <p>This class managers the world instance, player manager, and the inventory instance. The
 * inventory and resource overlays should also be managed by this class.
 *
 * <p>In stage 0, this class will store the Brutus character to wander around.
 *
 * @stage1part This class manages the player manager.
 * @stage2part This class manages the world instance.
 */
public class JavaBeanFarm implements Game {

    private final PlayerManager playerManager;

    private final NpcManager npcs;
    private final EnemyManager enemies;

    private final BeanWorld world;

    private final Inventory inventory;
    private final List<Overlay> overlays = new ArrayList<>();

    private String readAllReader(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
         StringJoiner sb = new StringJoiner(System.lineSeparator());
        String line;
        while ((line = br.readLine()) != null) {
            sb.add(line);
        }
        return sb.toString();
    }

    /**
     * Constructs a new JavaBean Farm game using the given dimensions, mapPath and detailPath
     *
     * @param dimensions The dimensions we want for this game.
     * @param mapReader A reader the contains a description of the world map.
     * @param detailReader A reader the contains the overlay details for the game, e.g. spawner locations.
     * @throws IOException If the game is unable to find or open the default world map file.
     * @throws WorldLoadException If the default world map file cannot be parsed successfully.
     */
    public JavaBeanFarm(Dimensions dimensions, Reader mapReader, Reader detailReader)
            throws IOException, WorldLoadException {

        final String detailsContent = readAllReader(detailReader);
        final PlayerDetails playerDetails = OverlayBuilder.getPlayerDetailsFromFile(detailsContent);
        this.playerManager = new PlayerManager(playerDetails.getX(), playerDetails.getY());
        this.npcs = new NpcManager();
        this.enemies = new EnemyManager(dimensions);
        final List<SpawnerDetails> magpieSpawnPoints =
                OverlayBuilder.getMagpieSpawnDetailsFromString(detailsContent);
        for (SpawnerDetails spawnerDetails : magpieSpawnPoints) {
            this.enemies.add(
                    new MagpieSpawner(
                            spawnerDetails.getX(),
                            spawnerDetails.getY(),
                            spawnerDetails.getDuration()));
        }
        final List<SpawnerDetails> eagleSpawnPoints =
                OverlayBuilder.getEagleSpawnDetailsFromString(detailsContent);
        for (SpawnerDetails spawnerDetails : eagleSpawnPoints) {
            this.enemies.add(
                    new EagleSpawner(
                            spawnerDetails.getX(),
                            spawnerDetails.getY(),
                            spawnerDetails.getDuration()));
        }
        final List<SpawnerDetails> pigeonSpawnPoints =
                OverlayBuilder.getPigeonSpawnDetailsFromString(detailsContent);
        for (SpawnerDetails spawnerDetails : pigeonSpawnPoints) {
            this.enemies.add(
                    new PigeonSpawner(
                            spawnerDetails.getX(),
                            spawnerDetails.getY(),
                            spawnerDetails.getDuration()));
        }

        String worldContent = readAllReader(mapReader);
        this.world = WorldBuilder.fromTiles(WorldBuilder.fromString(dimensions, worldContent));

        final List<CabbageDetails> cabbageSpawnPoints =
                OverlayBuilder.getCabbageSpawnDetailsFromString(detailsContent);
        for (CabbageDetails cabbageDetails :
                cabbageSpawnPoints) { // HACK - can I improve this?
            final int positionX = cabbageDetails.getX();
            final int positionY = cabbageDetails.getY();
            final List<Tile> tiles = this.world.tilesAtPosition(positionX, positionY, dimensions);
            for (Tile tile : tiles) {
                if (tile instanceof Dirt) {
                    TinyInventory tempInventory = new TinyInventory(5, 100, 100);
                    ((Dirt) tile).till();
                    ((Dirt) tile).plant(tempInventory);
                }
            }
        }

        int inventorySize = 5;
        this.inventory =
                new TinyInventory(
                        inventorySize,
                        playerDetails.getStartingCoins(),
                        playerDetails.getStartingFood());

        inventory.setItem(0, new Bucket());
        inventory.setItem(1, new Hoe());
        inventory.setItem(2, new Jackhammer());
        inventory.setItem(3, new HiveHammer());
        inventory.setItem(4, new Pole());

        this.overlays.add(new InventoryOverlay(dimensions, inventorySize));
        this.overlays.add(new ResourceOverlay(dimensions));
    }

    public JavaBeanFarm(Dimensions dimensions, String mapFile, String detailsFile) throws IOException, WorldLoadException {
        this(dimensions, new FileReader(mapFile), new FileReader(detailsFile));
    }

    /**
     * Ticks the internal game state forward by one frame. a
     *
     * @param state The state of the engine, including the mouse, keyboard information and
     *     dimension. Useful for processing keyboard presses or mouse movement.
     * @stage1part The player manager should be progressed via {@link
     *     PlayerManager#tick(EngineState, GameState)}.
     * @stage2part The world should be progressed via {@link BeanWorld#tick(EngineState,
     *     GameState)}.
     */
    public void tick(EngineState state) {
        GameState game =
                new JavaBeanGameState(
                        world, playerManager.getPlayer(), inventory, this.npcs, this.enemies);
        for (Overlay overlay : overlays) {
            overlay.tick(state, game);
        }
        this.playerManager.tick(state, game);
        this.npcs.tick(state, game);
        this.enemies.tick(state, game);
        this.world.tick(state, game);
        final int px = playerManager.getPlayer().getX();
        final int py = playerManager.getPlayer().getY();
        List<Tile> underPlayer =
                world.tilesAtPosition(px, py, state.getDimensions());

        this.playerManager.interact(state, game, underPlayer);

        // ④ NPC / 敌人交互
        this.npcs.interact(state, game);
        this.enemies.interact(state, game);
        this.npcs.cleanup();
        this.enemies.cleanup();
    }
    /**
     * A collection of items to render, every component of the game to be rendered should be
     * returned.
     *
     * @return The list of renderables required to draw the whole game.
     * @stage2part Any renderables of the world (i.e. {@link BeanWorld#render()}) must be rendered
     *     behind everything else, i.e., first in the returned list.
     * @stage1part Any renderables of the player (i.e. {@link PlayerManager#render()}) must be
     *     rendered after the world but before overlays.
     *     <p>Overlays, i.e., {@link ResourceOverlay} and {@link InventoryOverlay} must be rendered
     *     last in any order.
     */
    @Override
    public List<Renderable> render() {

        List<Renderable> renderables = new ArrayList<>();

        renderables.addAll(this.world.render());

        renderables.addAll(this.npcs.render());
        renderables.addAll(this.enemies.render());

        renderables.addAll(this.playerManager.render());

        for (Overlay overlay : overlays) {
            renderables.addAll(overlay.render());
        }

        return renderables;
    }
}
