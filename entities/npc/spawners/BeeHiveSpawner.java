package builder.entities.npc.spawners;

import builder.GameState;
import builder.Interactable;
import builder.entities.npc.BeeHive;
import builder.entities.npc.Npc;
import engine.EngineState;
import engine.timing.RepeatingTimer;

/**
 * Spawns up to 3 BeeHives over time. Timer advances in tick(); actual spawn happens in interact().
 */
public class BeeHiveSpawner extends Npc implements Interactable {

    private final RepeatingTimer timer;
    private int spawned = 0;
    private static final int MAX_SPAWNS = 3;

    public BeeHiveSpawner(int x, int y, int duration) {
        super(x, y);
        this.timer = new RepeatingTimer(duration);
        this.setSpeed(0);
    }

    @Override
    public void tick(EngineState state, GameState game) {
        super.tick(state);
        // Advance timer only; don't spawn here to avoid double-spawns in the same frame
        this.timer.tick();
    }

    @Override
    public void interact(EngineState state, GameState game) {
        super.interact(state, game);
        if (spawned >= MAX_SPAWNS) return;
        if (!this.timer.isFinished()) return;

        // snap to tile center (consistent with tests)
        int ts = state.getDimensions().tileSize();
        int half = ts / 2;
        int sx = (this.getX() / ts) * ts + half;
        int sy = (this.getY() / ts) * ts + half;

        // avoid duplicate hive on the same snapped cell
        for (var npc : game.getNpcs().npcs) {
            if (npc instanceof BeeHive && npc.getX() == sx && npc.getY() == sy) {
                return;
            }
        }

        game.getNpcs().npcs.add(new BeeHive(sx, sy));
        spawned++;
    }
}