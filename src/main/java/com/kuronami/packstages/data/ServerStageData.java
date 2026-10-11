package com.kuronami.packstages.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.kuronami.packstages.PackStages;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Server-side copy of the pack data. Reloaded with datapacks; resolved lazily so tags are bound. */
public final class ServerStageData {
    private static final Gson GSON = new GsonBuilder().create();

    private static volatile Map<String, StageDefinition> definitions = Map.of();
    private static volatile Locks locks;

    private ServerStageData() {}

    public static Collection<StageDefinition> definitions() {
        return definitions.values();
    }

    public static StageDefinition definition(String name) {
        return definitions.get(name);
    }

    public static Locks locks() {
        Locks l = locks;
        if (l == null) {
            l = new Locks(definitions.values());
            locks = l;
        }
        return l;
    }

    /** Drops resolved caches; called when tags change. */
    public static void invalidate() {
        locks = null;
    }

    /** Resolves now (after tags are bound) so the first click after a reload doesn't pay for it. */
    public static void rebuild() {
        invalidate();
        locks();
    }

    /** Loads {@code pack_stages/stages}. */
    public static final class StagesLoader extends SimpleJsonResourceReloadListener {
        private final HolderLookup.Provider registries;

        public StagesLoader(HolderLookup.Provider registries) {
            super(GSON, "pack_stages/stages");
            this.registries = registries;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
            Map<String, StageDefinition> out = new LinkedHashMap<>();
            files.forEach((id, json) -> {
                String name = id.getPath().replace('/', '.');
                StageDefinition.BODY.codec().parse(ops, json)
                        .resultOrPartial(err -> PackStages.LOGGER.error("[pack_stages] stage file {}: {}", id, err))
                        .ifPresent(def -> out.merge(name, def.withName(name), (a, b) -> {
                            if ((a.display().isPresent() && b.display().isPresent() && !a.display().equals(b.display()))
                                    || a.jei() != b.jei()) {
                                PackStages.LOGGER.warn("[pack_stages] stage {} is defined in several files with different "
                                        + "\"display\" or \"jei\"; which one wins is not fixed (latest: {})", name, id);
                            }
                            return a.merge(b);
                        }));
            });
            definitions = Map.copyOf(out);
            invalidate();
            PackStages.LOGGER.info("[pack_stages] loaded {} stage definition(s)", out.size());
        }
    }
}
