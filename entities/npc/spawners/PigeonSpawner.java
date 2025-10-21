package builder.entities.npc.spawners;

import builder.GameState;
import builder.entities.resources.Cabbage;
import builder.tiles.Tile;

import engine.EngineState;
import engine.game.Entity;
import engine.game.HasPosition;
import engine.timing.RepeatingTimer;
import engine.timing.TickTimer;

import java.util.List;

public class PigeonSpawner implements Spawner {

    private int x;
    private int y;
    private final RepeatingTimer timer;

    public PigeonSpawner(int x, int y) {
        this(x, y, 100);
    }

    public PigeonSpawner(int x, int y, int duration) {
        this.x = x;
        this.y = y;
        this.timer = new RepeatingTimer(duration);
    }

    @Override
    public TickTimer getTimer() {
        return this.timer;
    }

    @Override
    public void tick(EngineState state, GameState game) {
        this.timer.tick();

        List<Tile> tilesWithCabbage =
                game.getWorld().tileSelector(tile -> {
                    for (Entity e : tile.getStackedEntities()) {
                        if (e instanceof Cabbage) return true;
                    }
                    return false;
                });

        if (tilesWithCabbage.isEmpty()) return;

        Tile closest = tilesWithCabbage.getFirst();
        int best = distanceFrom(closest);
        for (Tile t : tilesWithCabbage) {
            int d = distanceFrom(t);
            if (d < best) { best = d; closest = t; }
        }

        if (this.timer.isFinished()) {
            game.getEnemies().spawnX = this.getX();
            game.getEnemies().spawnY = this.getY();
            game.getEnemies().Birds.add(game.getEnemies().mkP(closest));
        }
    }

    public int distanceFrom(HasPosition pos) {
        int dx = pos.getX() - this.getX();
        int dy = pos.getY() - this.getY();
        return (int)Math.sqrt(dx * dx + dy * dy);
    }

    @Override public int getX() { return this.x; }
    @Override public void setX(int x) { this.x = x; }
    @Override public int getY() { return this.y; }
    @Override public void setY(int y) { this.y = y; }
}
