package builder.entities.npc;

import builder.GameState;
import builder.Tickable;
import builder.Interactable;
import builder.ui.RenderableGroup;

import engine.EngineState;
import engine.renderer.Renderable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NpcManager implements Interactable, Tickable, RenderableGroup {

    // Active NPCs on the field
    public final ArrayList<Npc> npcs = new ArrayList<>();

    // Newly requested NPCs to add at a safe point
    private final ArrayList<Npc> pendingAdd = new ArrayList<>();

    public NpcManager() {}

    /** Queue an NPC to be managed (safe to call during any phase). */
    public void spawn(Npc npc) {
        pendingAdd.add(npc);
    }

    /** Backward compatible alias if other code calls addNpc. */
    public void addNpc(Npc npc) {
        pendingAdd.add(npc);
    }

    private void flushPendingAdds() {
        if (!pendingAdd.isEmpty()) {
            npcs.addAll(pendingAdd);
            pendingAdd.clear();
        }
    }

    /** Remove any NPCs that were marked for removal. */
    public void cleanup() {
        Iterator<Npc> it = npcs.iterator();
        while (it.hasNext()) {
            if (it.next().isMarkedForRemoval()) {
                it.remove();
            }
        }
    }

    @Override
    public void tick(EngineState state, GameState game) {
        this.cleanup();

        for (Npc npc : npcs) {
            if (npc.isMarkedForRemoval()) continue;

            npc.tick(state, game);
        }
    }

    @Override
    public void interact(EngineState state, GameState game) {
        // Accept newly spawned NPCs before interacting
        flushPendingAdds();

        // Build interactables from a snapshot for stability
        ArrayList<Interactable> interactables = getInteractablesSnapshot();
        for (Interactable interactable : interactables) {
            interactable.interact(state, game);
        }
    }

    private ArrayList<Interactable> getInteractablesSnapshot() {
        ArrayList<Interactable> list = new ArrayList<>();
        for (Npc npc : new ArrayList<>(npcs)) {
            if (npc instanceof Interactable) {
                list.add((Interactable) npc);
            }
        }
        return list;
    }

    @Override
    public List<Renderable> render() {
        return new ArrayList<>(this.npcs);
    }
}

