package com.kuronami.packstages.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Commands that run the first time a player holds one of the listed items.
 * Loaded from {@code data/<namespace>/pack_stages/milestones/<path>.json}.
 *
 * @param id       file id, used as the key of the "already done" record
 * @param items    item ids, {@code #tag}, {@code @modid} or ids with {@code *} wildcards
 * @param each     {@code true}: once per distinct item; {@code false}: once for the whole set
 * @param commands run as the player ({@code @s}) with function permission; {@code {item}} becomes the item id
 */
public record Milestone(ResourceLocation id, List<String> items, boolean each, List<String> commands) {

    private static final Codec<String> MODE = Codec.STRING.validate(s -> "each".equals(s) || "once".equals(s)
            ? DataResult.success(s)
            : DataResult.error(() -> "\"mode\" must be \"each\" or \"once\", got \"" + s + "\""));

    public static final Codec<Milestone> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().fieldOf("items").forGetter(Milestone::items),
            MODE.optionalFieldOf("mode", "each").forGetter(m -> m.each ? "each" : "once"),
            Codec.STRING.listOf().fieldOf("commands").forGetter(Milestone::commands)
    ).apply(i, (items, mode, commands) ->
            new Milestone(ResourceLocation.withDefaultNamespace("unset"), items, !"once".equals(mode), commands)));

    public Milestone withId(ResourceLocation newId) {
        return new Milestone(newId, items, each, commands);
    }
}
