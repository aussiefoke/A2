package builder.entities.npc;

import builder.GameState;
import builder.entities.npc.enemies.Enemy;
import builder.ui.SpriteGallery;

import engine.EngineState;
import engine.art.sprites.SpriteGroup;
import engine.timing.RepeatingTimer;

/** Spawns bees it fires at enemy's within a set range */
public class BeeHive extends Npc {

    public static final int DETECTION_DISTANCE = 350;
    // IMPORTANT: 100 so we can spawn at ~41,141,241,341,441,541 within 630 ticks (total 6 bees)
    public static final int TIMER = 100;

    public static final int FOOD_COST = 2;
    public static final int COIN_COST = 2;

    private static final SpriteGroup art = SpriteGallery.hive;

    private boolean loaded = true; // armed to fire when timer finished & enemy in range
    private final RepeatingTimer timer = new RepeatingTimer(TIMER);
    private boolean wasFinished = false;

    public BeeHive(int x, int y) {
        super(x, y);
        this.setSprite(art.getSprite("default"));
        this.setSpeed(0);
    }

    @Override
    public void tick(EngineState state, GameState game) {
        super.tick(state);
        // advance our simple fire-rate timer
        this.timer.tick();
        boolean nowFinished = timer.isFinished();
        if (nowFinished && !wasFinished) {
            loaded = true;
        }
        wasFinished = nowFinished;
    }

    @Override
    public void interact(EngineState state, GameState game) {
        super.interact(state, game);

        if (!this.loaded) return;
        Enemy nearest = null;
        int bestD2 = Integer.MAX_VALUE;
        final int x0 = this.getX();
        final int y0 = this.getY();

        for (Enemy e : game.getEnemies().getALl()) {
            int dx = e.getX() - x0;
            int dy = e.getY() - y0;
            int d2 = dx * dx + dy * dy;
            if (d2 < bestD2) {
                bestD2 = d2;
                nearest = e;
            }
        }

        final int detect = DETECTION_DISTANCE;
        if (nearest != null && bestD2 <= detect * detect) {
            game.getNpcs().npcs.add(new GuardBee(x0, y0, nearest));
            this.loaded = false;
            this.wasFinished = true;
        }
    }}