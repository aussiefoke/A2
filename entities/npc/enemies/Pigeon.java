package builder.entities.npc.enemies;

import builder.GameState;
import builder.entities.npc.Expirable;
import builder.entities.resources.Cabbage;
import builder.tiles.Tile;
import builder.ui.SpriteGallery;

import engine.EngineState;
import engine.art.sprites.SpriteGroup;
import engine.game.Entity;
import engine.game.HasPosition;
import engine.timing.FixedTimer;

import java.util.List;

public class Pigeon extends Enemy implements Expirable {

    private static final SpriteGroup art = SpriteGallery.pigeon;
    private FixedTimer lifespan = new FixedTimer(3000);
    private HasPosition trackedTarget;
    public Boolean attacking = true;
    private int spawnX = 0;
    private int spawnY = 0;

    public Pigeon(int x, int y) {
        super(x, y);
        this.spawnX = x;
        this.spawnY = y;
        this.setSprite(art.getSprite("down"));
        this.setSpeed(1);
    }

    public Pigeon(int x, int y, HasPosition trackedTarget) {
        super(x, y);
        this.spawnX = x;
        this.spawnY = y;
        this.trackedTarget = trackedTarget;
        this.setSprite(art.getSprite("down"));
        this.setSpeed(1);
    }

    @Override
    public FixedTimer getLifespan() {
        return lifespan;
    }

    @Override
    public void setLifespan(FixedTimer timer) {
        this.lifespan = timer;
    }

    @Override
    public void tick(EngineState engine, GameState game) {
        super.tick(engine, game);
        if (!this.attacking) {
            double deltaX = (this.spawnX - this.getX());
            double deltaY = (this.spawnY - this.getY());
            this.setDirection((int) Math.toDegrees(Math.atan2(deltaY, deltaX)));

            if (this.distanceFrom(this.spawnX, this.spawnY)
                    < engine.getDimensions().tileSize()) { // get close to spawn
                this.markForRemoval();
            }
            if (this.spawnY < this.getY()) {
                this.setSprite(art.getSprite("up"));
            } else {
                this.setSprite(art.getSprite("down"));
            }
        }

        if (this.trackedTarget == null
                && this
                .attacking) { // if the pigeon has no target, it should go to the center of
            // the screen if its hunting
            double deltaX = ((double) engine.getDimensions().windowSize() / 2 - this.getX());
            double deltaY = ((double) engine.getDimensions().windowSize() / 2 - this.getY());
            this.setDirection((int) Math.toDegrees(Math.atan2(deltaY, deltaX)));
            double centerY = (double) engine.getDimensions().windowSize() / 2;
            if (centerY > this.getY()) {
                this.setSprite(art.getSprite("down"));
            } else {
                this.setSprite(art.getSprite("up"));
            }
        } else {
            // do nothing
        }
        if (this.trackedTarget != null && this.attacking) {
            double deltaX = (this.trackedTarget.getX() - this.getX());
            double deltaY = (this.trackedTarget.getY() - this.getY());
            this.setDirection((int) Math.toDegrees(Math.atan2(deltaY, deltaX)));
            if (this.trackedTarget.getY() > this.getY()) {
                this.setSprite(art.getSprite("down"));
            } else {
                this.setSprite(art.getSprite("up"));
            }
        } else {
            // do nothing
        }

        this.move();

        this.lifespan.tick();
        if (this.lifespan.isFinished()) {
            this.markForRemoval();
        } else {
            // do nothing
        }
        if (!attacking) {
            if (this.distanceFrom(spawnX, spawnY) < engine.getDimensions().tileSize()) {
                this.markForRemoval();
            }
            if (this.spawnY < this.getY()) {
                this.setSprite(art.getSprite("up"));
            } else {
                this.setSprite(art.getSprite("down"));
            }
        }

        List<Tile> tiles =
                game.getWorld()
                        .tileSelector(
                                tile -> {
                                    for (Entity entity : tile.getStackedEntities()) {
                                        if (entity instanceof Cabbage) {
                                            return true;
                                        } else {
                                            // do nothing
                                        }
                                    }
                                    return false;
                                });
        if (tiles.size() > 0) {
            int distance = this.distanceFrom(tiles.getFirst());
            Tile closest = tiles.getFirst();
            for (Tile tile : tiles) {
                int d = this.distanceFrom(tile);
                if (d < distance) {
                    closest = tile;
                    distance = d;
                } else {
                    // do nothing
                }
            }
            this.trackedTarget = closest;

            if (this.attacking
                    && this.distanceFrom(this.trackedTarget) < engine.getDimensions().tileSize()) {
                for (Entity entity : closest.getStackedEntities()) {
                    if (entity instanceof Cabbage cabbage) {
                        cabbage.markForRemoval();
                        this.attacking = false;
                    } else {
                        // do nothing
                    }
                }
            }
        } else { // no cabbages to get
            this.attacking = false;
        }
    }
}
