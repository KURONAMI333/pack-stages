package com.kuronami.packstages.data;

import com.kuronami.packstages.PackStages;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Expands entries against a registry: {@code namespace:id}, {@code #tag}, {@code @modid}, or an id
 * with {@code *} wildcards such as {@code lilis_pottery:*_vase} (handy for colour variants).
 */
public final class EntryMatcher {
    private EntryMatcher() {}

    public static <T> Set<T> resolve(Registry<T> registry, Collection<String> entries, String where) {
        Set<T> out = new LinkedHashSet<>();
        for (String raw : entries) {
            String entry = raw.trim();
            if (entry.isEmpty()) continue;
            if (entry.startsWith("@")) {
                String namespace = entry.substring(1);
                for (Map.Entry<ResourceKey<T>, T> e : registry.entrySet()) {
                    if (e.getKey().location().getNamespace().equals(namespace)) out.add(e.getValue());
                }
            } else if (entry.startsWith("#")) {
                ResourceLocation id = ResourceLocation.tryParse(entry.substring(1));
                if (id == null) {
                    warn(where, entry);
                    continue;
                }
                registry.getTag(TagKey.create(registry.key(), id))
                        .ifPresent(set -> set.forEach((Holder<T> h) -> out.add(h.value())));
            } else if (entry.indexOf('*') >= 0) {
                Pattern pattern = glob(entry);
                for (Map.Entry<ResourceKey<T>, T> e : registry.entrySet()) {
                    if (pattern.matcher(e.getKey().location().toString()).matches()) out.add(e.getValue());
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id == null) {
                    warn(where, entry);
                    continue;
                }
                // Unknown ids are normal in shared pack data (the mod may simply not be installed).
                registry.getOptional(id).ifPresent(out::add);
            }
        }
        return out;
    }

    private static Pattern glob(String entry) {
        String full = entry.indexOf(':') >= 0 ? entry : "minecraft:" + entry;
        String[] parts = full.split("\\*", -1);
        StringBuilder regex = new StringBuilder(Pattern.quote(parts[0]));
        for (int i = 1; i < parts.length; i++) regex.append(".*").append(Pattern.quote(parts[i]));
        return Pattern.compile(regex.toString());
    }

    private static void warn(String where, String entry) {
        PackStages.LOGGER.warn("[pack_stages] {}: '{}' is not a valid id, #tag, @modid or wildcard id", where, entry);
    }
}
