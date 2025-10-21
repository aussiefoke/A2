package builder.entities.npc.spawners;

import builder.GameState;
import builder.entities.npc.Scarecrow;

import engine.EngineState;
import engine.timing.RepeatingTimer;
import engine.timing.TickTimer;

public class ScarecrowSpawner implements Spawner {

    private int x = 0;
    private int y = 0;
    private RepeatingTimer timer = new RepeatingTimer(300);

    public ScarecrowSpawner(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public TickTimer getTimer() {
        return this.timer;
    }

    @Override
    public void tick(EngineState state, GameState game) {
        this.timer.tick();
        // look at use code to spawn
        if (game.getInventory().getCoins() >= 2 && state.getKeys().isDown('c')) {
            game.getInventory().addCoins(-2);
            game.getNpcs().addNpc(new Scarecrow(game.getPlayer().getX(), game.getPlayer().getY()));
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
