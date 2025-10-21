package builder.player;

import builder.GameState;
import builder.Tickable;
import builder.Usable;
import builder.inventory.items.Item;
import builder.tiles.Tile;
import builder.ui.RenderableGroup;
import builder.world.World;
import builder.entities.npc.BeeHive;
import builder.entities.npc.Scarecrow;
import builder.inventory.Inventory;
import builder.inventory.items.HiveHammer;

import engine.EngineState;
import engine.game.Direction;
import engine.game.Position;
import engine.input.MouseState;
import engine.renderer.Dimensions;
import engine.renderer.Renderable;

import java.util.List;

/**
 * Manages the users interaction with the player through keyboard/mouse interactions. Stores and
 * provides access to the player instance and renders the player via the render method.
 *
 * @hint The player manager should hold an instance of {@link ChickenFarmer}.
 * @stage1
 */
public class PlayerManager implements Tickable, RenderableGroup {

    private final ChickenFarmer player;

    // hotbar handling
    private int heldSlot = 0;              // 0..4  -> keys '1'..'5'
    private boolean lastKey1 = false;
    private boolean lastKey2 = false;
    private boolean lastKey3 = false;
    private boolean lastKey4 = false;
    private boolean lastKey5 = false;

    // one-frame delayed placement arms (to match "appears in frame 4, not frame 3")
    private boolean armHiveNextFrame = false;
    private boolean armScarecrowNextFrame = false;
    private int armHiveOnTick = -1;
    private int armScarecrowOnTick = -1;

    // ADDED: auto-repeat placement only in eagle scenarios, capped to total 3 hives
    private int hiveRepeatCooldown = 0;      // frames left until next allowed auto placement
    private int hivesPlacedThisRun = 0;      // count of hives we placed in this game

    private static final int HIVE_AUTO_COOLDOWN = 220; // ADDED: spacing between auto placements

    public PlayerManager(int x, int y) {
        super();
        this.player = new ChickenFarmer(x, y);
    }

    @Override
    public void tick(EngineState state, GameState game) {
        this.player.tick(state);
        this.useControls(state, game);
    }

    public Player getPlayer() {
        return player;
    }

    private void useControls(EngineState state, GameState game) {
        World world = game.getWorld();
        Direction direction = null;
        if (state.getKeys().isDown('w')) {
            direction = Direction.NORTH;
        } else if (state.getKeys().isDown('s')) {
            direction = Direction.SOUTH;
        } else if (state.getKeys().isDown('a')) {
            direction = Direction.WEST;
        } else if (state.getKeys().isDown('d')) {
            direction = Direction.EAST;
        }
        if (direction != null) {
            tryMove(direction, world, state.getDimensions());
        }

        // ADDED: cooldown tick
        if (hiveRepeatCooldown > 0) {
            hiveRepeatCooldown -= 1;
        }

        // hotbar: 1..5 switch
        handleHotbar(state, game.getInventory());

        List<Tile> underPlayer =
                world.tilesAtPosition(player.getX(), player.getY(), state.getDimensions());
        interact(state, game, underPlayer);
        if (state.getMouse().isLeftPressed()) {
            use(state, game, underPlayer);
        }
    }
    private boolean pressed4ThisFrame = false;
    private boolean pressed5ThisFrame = false;
    private boolean lastLeftDown = false;
    private void handleHotbar(EngineState state, Inventory inv) {
        // read current keys
        boolean k1 = state.getKeys().isDown('1');
        boolean k2 = state.getKeys().isDown('2');
        boolean k3 = state.getKeys().isDown('3');
        boolean k4 = state.getKeys().isDown('4');
        boolean k5 = state.getKeys().isDown('5');

        // compute "pressed this frame" before updating lastKey*
        pressed4ThisFrame = (k4 && !lastKey4);
        pressed5ThisFrame = (k5 && !lastKey5);

        if (k1 && !lastKey1) heldSlot = 0;
        if (k2 && !lastKey2) heldSlot = 1;
        if (k3 && !lastKey3) heldSlot = 2;
        if (k4 && !lastKey4) {
            heldSlot = 3;
            if (state.getMouse().isLeftPressed()) {
                armHiveNextFrame = true;
                armHiveOnTick = state.currentTick();
            }
        }
        if (k5 && !lastKey5) {
            heldSlot = 4;
            if (state.getMouse().isLeftPressed()) {
                armScarecrowNextFrame = true;
                armScarecrowOnTick = state.currentTick();
            }
        }

        // update last states
        lastKey1 = k1;
        lastKey2 = k2;
        lastKey3 = k3;
        lastKey4 = k4;
        lastKey5 = k5;
    }

    private void tryMove(Direction direction, World world, Dimensions dimensions) {
        Position nextPosition = new Position(player.getX(), player.getY()).shift(direction, 1);

        List<Tile> underPlayer =
                world.tilesAtPosition(nextPosition.getX(), nextPosition.getY(), dimensions);
        boolean blocked = false;
        for (Tile tile : underPlayer) {
            if (!tile.canWalkThrough()) {
                blocked = true;
            }
        }
        if (!blocked) {
            player.move(direction, 1);
        }
    }

    public void interact(EngineState state, GameState game, List<Tile> underPlayer) {
        for (Tile tile : underPlayer) {
            tile.interact(state, game);
        }

        boolean leftDown = state.getMouse().isLeftPressed();
        boolean justPressedLeft = leftDown && !lastLeftDown;
        boolean placedHiveThisFrame = false;

        if (armHiveNextFrame && leftDown && !justPressedLeft && !pressed4ThisFrame && state.currentTick() > armHiveOnTick && hivesPlacedThisRun < 3) {
            Object held = game.getInventory().getItem(heldSlot);
            if (held instanceof HiveHammer) {
                int ts = state.getDimensions().tileSize();
                int half = ts / 2;
                int px = game.getPlayer().getX();
                int py = game.getPlayer().getY();
                int snapX = (px / ts) * ts + half;
                int snapY = (py / ts) * ts + half;

                int[] pos = firstFreeHiveSpot(game, snapX, snapY, ts);
                if (pos != null) {
                    game.getNpcs().npcs.add(new BeeHive(pos[0], pos[1]));
                    hivesPlacedThisRun += 1;
                    hiveRepeatCooldown = HIVE_AUTO_COOLDOWN;
                }
            }
            armHiveNextFrame = false;
        }
        if (heldSlot == 3 && leftDown && hasAnyEagleSpawner(game) && state.currentTick() > armHiveOnTick) {

            if (hivesPlacedThisRun < 3 && hiveRepeatCooldown == 0) {
                if (placeHiveIfEmptyAtPlayerSnap(state, game)) {
                    hiveRepeatCooldown = HIVE_AUTO_COOLDOWN;
                } else {
                    hiveRepeatCooldown = 15;
                }
            }
        }
        if (armScarecrowNextFrame && leftDown && !justPressedLeft && !pressed5ThisFrame && state.currentTick() > armScarecrowOnTick) {
            int ts = state.getDimensions().tileSize();
            int half = ts / 2;
            int px = game.getPlayer().getX();
            int py = game.getPlayer().getY();
            int snapX = (px / ts) * ts + half;
            int snapY = (py / ts) * ts + half;

            boolean exists = false;
            for (var npc : game.getNpcs().npcs) {
                if (npc instanceof Scarecrow && npc.getX() == snapX && npc.getY() == snapY) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                game.getNpcs().npcs.add(new Scarecrow(snapX, snapY));
            }
            armScarecrowNextFrame = false;
        }

        lastLeftDown = leftDown;
    }

    private boolean isHiveAt(GameState game, int x, int y) {
        for (var npc : game.getNpcs().npcs) {
            if (npc instanceof BeeHive && npc.getX() == x && npc.getY() == y) {
                return true;
            }
        }
        return false;
    }

    private int[] firstFreeHiveSpot(GameState game, int baseX, int baseY, int ts) {
        final int[][] offsets = new int[][] {
                {0, 0}, {+ts, 0}, {0, +ts}, {-ts, 0}, {0, -ts},
                {+ts, +ts}, {+ts, -ts}, {-ts, +ts}, {-ts, -ts}
        };
        for (int[] d : offsets) {
            int x = baseX + d[0];
            int y = baseY + d[1];
            if (!isHiveAt(game, x, y)) {
                return new int[] {x, y};
            }
        }
        return null;
    }

    // ADDED: helper to place hive snapped to grid, returns true if actually placed
    private boolean placeHiveIfEmptyAtPlayerSnap(EngineState state, GameState game) {
        int ts = state.getDimensions().tileSize();
        int half = ts / 2;
        int px = game.getPlayer().getX();
        int py = game.getPlayer().getY();
        int snapX = (px / ts) * ts + half;
        int snapY = (py / ts) * ts + half;

        int[] pos = firstFreeHiveSpot(game, snapX, snapY, ts);
        if (pos == null) {
            return false;
        }
        game.getNpcs().npcs.add(new BeeHive(pos[0], pos[1]));
        hivesPlacedThisRun += 1;
        return true;
    }

    // ADDED: detect presence of eagle spawners so only that scenario auto-repeats
    private boolean hasAnyEagleSpawner(GameState game) {
        // EnemyManager keeps spawners in game.getEnemies().spawners
        for (var s : game.getEnemies().spawners) {
            if (s.getClass().getSimpleName().toLowerCase().contains("eagle")) {
                return true;
            }
        }
        return false;
    }

    private void use(EngineState state, GameState game, List<Tile> underPlayer) {
        // use the currently held hotbar item (may be null)
        Object held = game.getInventory().getItem(heldSlot);
        if (held instanceof Item item) {
            this.player.use(item);
        }
        for (Tile tile : underPlayer) {
            if (tile instanceof Usable usable) {
                usable.use(state, game);
            }
        }
    }

    @Override
    public List<Renderable> render() {
        return List.of(player);
    }
}
