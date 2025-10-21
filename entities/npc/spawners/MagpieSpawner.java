package builder.entities.npc.spawners;

import builder.GameState;

import engine.EngineState;
import engine.timing.RepeatingTimer;
import engine.timing.TickTimer;

public class MagpieSpawner implements Spawner {

    private int x = 0;
    private int y = 0;
    private TickTimer timer;

    public MagpieSpawner(int x, int y) {
        this.x = x;
        this.y = y;
        this.timer = new RepeatingTimer(360);
    }

    public MagpieSpawner(int x, int y, int duration) {
        this.x = x;
        this.y = y;
        this.timer = new RepeatingTimer(duration);
    }

    @Override
    public TickTimer getTimer() {
        return timer;
    }

    @Override
    public void tick(EngineState state, GameState game) {
        this.timer.tick();
        if (this.getTimer().isFinished()) {
            game.getEnemies().spawnX = this.getX();
            game.getEnemies().spawnY = this.getY();
            game.getEnemies().Birds.add(game.getEnemies().mkM(game.getPlayer()));
        }
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }
}
