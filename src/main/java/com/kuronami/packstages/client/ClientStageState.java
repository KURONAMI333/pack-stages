package com.kuronami.packstages.client;

import com.kuronami.packstages.data.Locks;
import com.kuronami.packstages.data.StageDefinition;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What the client knows: the definitions sent by the server and the local player's stages.
 * Plain Java only, so common code may read it without touching client classes.
 */
public final class ClientStageState {
    private static volatile List<StageDefinition> definitions = List.of();
    private static volatile Set<String> stages = Set.of();
    private static volatile Locks locks;
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private ClientStageState() {}

    public static Set<String> stages() {
        return stages;
    }

    public static Locks locks() {
        Locks l = locks;
        if (l == null) {
            l = new Locks(definitions);
            locks = l;
        }
        return l;
    }

    public static void setDefinitions(List<StageDefinition> defs) {
        definitions = List.copyOf(defs);
        locks = null;
        fire();
    }

    public static void setStages(List<String> newStages) {
        stages = Set.copyOf(newStages);
        fire();
    }

    /** Tags changed on the client: re-resolve. */
    public static void invalidate() {
        locks = null;
        fire();
    }

    /** Leaving a world. */
    public static void reset() {
        definitions = List.of();
        stages = Set.of();
        locks = null;
        fire();
    }

    /** Called (on the client thread) whenever definitions, stages or tags change. */
    public static void addListener(Runnable listener) {
        LISTENERS.add(listener);
    }

    private static void fire() {
        LISTENERS.forEach(Runnable::run);
    }
}
