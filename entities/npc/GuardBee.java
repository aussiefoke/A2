package builder.entities.npc;

import builder.GameState;
import builder.entities.npc.enemies.Enemy;
import builder.ui.SpriteGallery;

import engine.EngineState;
import engine.art.sprites.SpriteGroup;
import engine.game.HasPosition;
import engine.timing.FixedTimer;

/**
 * A highly trained Guard Bee... don't think about that too much. This is our projectile class,
 * basically a bullet.
 */
public class GuardBee extends Npc implements Expirable {

    private final int spawnX;
    private final int spawnY;
    private static final int SPEED = 2;
    private static final SpriteGroup art = SpriteGallery.bee;
    private FixedTimer lifespan = new FixedTimer(300);
    private final HasPosition trackedTarget;

    // Integer step planning (Bresenham-style), persisted across frames
    private boolean stepInit = false;
    private int aimX, aimY;
    private int step_dx, step_dy;   // |Δx|, |Δy|
    private int step_sx, step_sy;   // x,y direction (+1 or -1)
    private int step_err;           // error accumulator

    // Axis stall guards across frames
    private int lastX, lastY;
    private int stallXFrames = 0;
    private int stallYFrames = 0;

    // Jitter directions used when aligned with aim axis to break stalls
    private int jitterXSign = 1;
    private int jitterYSign = 1;

    /**
     * @param xCoordinate horizontal spawning position
     * @param yCoordinate vertical spawning position
     * @param trackedTarget target with a position we want this to track
     */
    public GuardBee(int xCoordinate, int yCoordinate, HasPosition trackedTarget) {
        super(xCoordinate, yCoordinate);
        this.setSprite(art.getSprite("default"));
        this.trackedTarget = trackedTarget;

        this.spawnX = xCoordinate;
        this.spawnY = yCoordinate;

        if (trackedTarget != null) {
            int dx0 = trackedTarget.getX() - this.getX();
            int dy0 = trackedTarget.getY() - this.getY();
            setCardinalDirection(dx0, dy0);
        } else {
            setCardinalDirection(spawnX - this.getX(), spawnY - this.getY());
        }
        this.setSpeed(GuardBee.SPEED);

        this.lastX = this.getX();
        this.lastY = this.getY();
    }

    @Override
    public FixedTimer getLifespan() {
        return lifespan;
    }

    @Override
    public void setLifespan(FixedTimer timer) {
        this.lifespan = timer;
    }

    public void updateArtBasedOnDirection() {
        boolean goingUp = (this.getDirection() >= 230 && this.getDirection() < 310);
        boolean goingDown = (this.getDirection() >= 40 && this.getDirection() < 140);
        boolean goingRight = (this.getDirection() >= 310 && this.getDirection() < 40);
        if (goingDown) {
            this.setSprite(art.getSprite("down"));
        } else if (goingUp) {
            this.setSprite(art.getSprite("up"));
        } else if (goingRight) {
            this.setSprite(art.getSprite("right"));
        } else {
            this.setSprite(art.getSprite("left"));
        }
    }

    @Override
    public void tick(EngineState state, GameState game) {
        super.tick(state);

        // Decide aim: track the target if present, otherwise return to spawn
        int targetX, targetY;
        if (this.trackedTarget != null) {
            targetX = this.trackedTarget.getX();
            targetY = this.trackedTarget.getY();
        } else {
            targetX = this.spawnX;
            targetY = this.spawnY;
        }

        // Rebuild integer step vector if aim changed
        if (!stepInit || targetX != aimX || targetY != aimY) {
            aimX = targetX;
            aimY = targetY;

            int dx = aimX - this.getX();
            int dy = aimY - this.getY();

            step_dx = Math.abs(dx);
            step_dy = Math.abs(dy);
            step_sx = (dx >= 0) ? 1 : -1;
            step_sy = (dy >= 0) ? 1 : -1;
            step_err = 0;
            stepInit = true;

            setCardinalDirection(dx, dy); // for art-facing only
        }

        // Sub-step budget for this frame
        int budget = SPEED;

        // Axis-stall breakers (trigger one frame earlier)
        boolean forcedX = false;
        boolean forcedY = false;

        if (stallXFrames >= 2) {
            int dirX;
            if (this.getX() < aimX) dirX = 1;
            else if (this.getX() > aimX) dirX = -1;
            else dirX = jitterXSign;

            this.setSpeed(1);
            this.setDirection(dirX > 0 ? 0 : 180);
            this.move();
            if (hitEnemyIfClose(state, game)) return;
            jitterXSign = -jitterXSign;
            budget--;
            forcedX = true;
        }
        if (stallYFrames >= 2) {
            int dirY;
            if (this.getY() < aimY) dirY = 1;
            else if (this.getY() > aimY) dirY = -1;
            else dirY = jitterYSign;

            this.setSpeed(1);
            this.setDirection(dirY > 0 ? 90 : 270);
            this.move();
            if (hitEnemyIfClose(state, game)) return;
            jitterYSign = -jitterYSign;
            budget--;
            forcedY = true;
        }

        // If aim is strictly aligned to an axis and we forced a nudge on that axis,
        // skip normal sub-steps to ensure the nudge isn't canceled within the same frame.
        boolean skipNormalThisFrame = (forcedX && step_dx == 0) || (forcedY && step_dy == 0);

        // Normal Bresenham-style advancement using only cardinal directions and move()
        this.setSpeed(1);
        if (!skipNormalThisFrame) {
            for (int i = 0; i < budget; i++) {
                if (this.getX() == aimX && this.getY() == aimY) break;

                if (step_dx >= step_dy) {
                    // primary step on X
                    this.setDirection(step_sx > 0 ? 0 : 180);
                    this.move();
                    if (hitEnemyIfClose(state, game)) return;

                    step_err += step_dy;
                    if (step_err >= step_dx && this.getY() != aimY) {
                        // secondary step on Y
                        this.setDirection(step_sy > 0 ? 90 : 270);
                        this.move();
                        if (hitEnemyIfClose(state, game)) return;
                        step_err -= step_dx;
                    }
                } else {
                    // primary step on Y
                    this.setDirection(step_sy > 0 ? 90 : 270);
                    this.move();
                    if (hitEnemyIfClose(state, game)) return;

                    step_err += step_dx;
                    if (step_err >= step_dy && this.getX() != aimX) {
                        // secondary step on X
                        this.setDirection(step_sx > 0 ? 0 : 180);
                        this.move();
                        if (hitEnemyIfClose(state, game)) return;
                        step_err -= step_dy;
                    }
                }
            }
        }
        this.setSpeed(SPEED);

        // Update stall counters
        if (this.getX() == lastX) {
            stallXFrames++;
        } else {
            stallXFrames = 0;
        }
        if (this.getY() == lastY) {
            stallYFrames++;
        } else {
            stallYFrames = 0;
        }
        lastX = this.getX();
        lastY = this.getY();

        // Final safety check in case we started the frame overlapped
        if (hitEnemyIfClose(state, game)) return;

        this.updateArtBasedOnDirection();
        lifespan.tick();
        if (lifespan.isFinished()) {
            // If expiring this frame but within a tileSize of an enemy, treat as a hit so
            // both vanish at the same place/time (satisfies "same frame, within tileSize").
            boolean processed = false;
            for (Enemy e : game.getEnemies().getALl()) {
                if (this.distanceFrom(e) <= state.getDimensions().tileSize()) {
                    e.markForRemoval();
                    this.markForRemoval();
                    processed = true;
                    break;
                }
            }
            if (!processed) {
                // Otherwise, defer despawn by 1 frame to avoid vanishing "from afar" on
                // the same frame as a bird that was removed elsewhere.
                this.setLifespan(new FixedTimer(1));
            }
        }
    }

    private void setCardinalDirection(int dx, int dy) {
        if (dx == 0 && dy == 0) return;
        if (Math.abs(dx) >= Math.abs(dy)) {
            this.setDirection(dx >= 0 ? 0 : 180);
        } else {
            this.setDirection(dy >= 0 ? 90 : 270);
        }
    }

    private boolean hitEnemyIfClose(EngineState state, GameState game) {
        final int tile = state.getDimensions().tileSize();

        Enemy closest = null;
        int best = Integer.MAX_VALUE;

        // 选最近且在 tileSize 内的敌人
        for (Enemy enemy : game.getEnemies().getALl()) {
            int d = this.distanceFrom(enemy);
            if (d <= tile && d < best) {
                best = d;
                closest = enemy;
            }
        }

        if (closest != null) {
            this.setX(closest.getX());
            this.setY(closest.getY());

            closest.markForRemoval();
            this.markForRemoval();
            return true;
        }
        return false;
    }}
