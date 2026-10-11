package com.kuronami.packstages.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What one stage unlocks. Loaded from {@code data/<namespace>/pack_stages/stages/<path>.json};
 * the stage name is the path with {@code /} turned into {@code .}
 * (so {@code potter/journeyman.json} is the stage {@code potter.journeyman}).
 *
 * <p>Every list takes item/block/menu ids, {@code #tag} or {@code @modid} entries.
 *
 * @param name    stage name used by commands
 * @param display name shown to players; falls back to {@link #name}
 * @param items   items that can be neither made nor used without the stage
 * @param craft   items that can't be taken out of a crafting/output slot without the stage
 * @param use     items that can't be right-clicked or placed without the stage
 * @param blocks  blocks that can't be right-clicked (so their screens don't open) without the stage
 * @param menus   menu types (screens) that close immediately without the stage
 * @param jei     whether locked items are hidden from JEI or stay listed with an unlock hint
 */
public record StageDefinition(String name, Optional<Component> display, List<String> items, List<String> craft,
                              List<String> use, List<String> blocks, List<String> menus, JeiMode jei) {

    public static final MapCodec<StageDefinition> BODY = RecordCodecBuilder.mapCodec(i -> i.group(
            ComponentSerialization.CODEC.optionalFieldOf("display").forGetter(StageDefinition::display),
            Codec.STRING.listOf().optionalFieldOf("items", List.of()).forGetter(StageDefinition::items),
            Codec.STRING.listOf().optionalFieldOf("craft", List.of()).forGetter(StageDefinition::craft),
            Codec.STRING.listOf().optionalFieldOf("use", List.of()).forGetter(StageDefinition::use),
            Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(StageDefinition::blocks),
            Codec.STRING.listOf().optionalFieldOf("menus", List.of()).forGetter(StageDefinition::menus),
            JeiMode.CODEC.optionalFieldOf("jei", JeiMode.SHOW).forGetter(StageDefinition::jei)
    ).apply(i, (display, items, craft, use, blocks, menus, jei) ->
            new StageDefinition("", display, items, craft, use, blocks, menus, jei)));

    /** Wire format: the body plus the stage name. */
    public static final Codec<StageDefinition> NETWORK_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(StageDefinition::name),
            BODY.forGetter(d -> d)
    ).apply(i, (name, body) -> body.withName(name)));

    public StageDefinition withName(String newName) {
        return new StageDefinition(newName, display, items, craft, use, blocks, menus, jei);
    }

    /** Combines two files that define the same stage name (e.g. from two datapacks). */
    public StageDefinition merge(StageDefinition other) {
        return new StageDefinition(name, display.isPresent() ? display : other.display,
                concat(items, other.items), concat(craft, other.craft), concat(use, other.use),
                concat(blocks, other.blocks), concat(menus, other.menus), jei);
    }

    public Component displayName() {
        return display.orElseGet(() -> Component.literal(name));
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(a);
        out.addAll(b);
        return List.copyOf(out);
    }

    /** How JEI shows items locked by a stage. */
    public enum JeiMode {
        HIDE, SHOW;

        public static final Codec<JeiMode> CODEC = Codec.STRING.comapFlatMap(
                s -> switch (s.toLowerCase(java.util.Locale.ROOT)) {
                    case "hide" -> com.mojang.serialization.DataResult.success(HIDE);
                    case "show" -> com.mojang.serialization.DataResult.success(SHOW);
                    default -> com.mojang.serialization.DataResult.error(() -> "\"jei\" must be \"hide\" or \"show\", got \"" + s + "\"");
                },
                m -> m.name().toLowerCase(java.util.Locale.ROOT));
    }
}
