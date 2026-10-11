package com.kuronami.packstages.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Saved per player: the stages they hold. */
public final class PlayerStages {
    public static final Codec<PlayerStages> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("stages", List.of()).forGetter(p -> List.copyOf(p.stages))
    ).apply(i, PlayerStages::new));

    private final Set<String> stages = new LinkedHashSet<>();

    public PlayerStages() {}

    private PlayerStages(List<String> stages) {
        this.stages.addAll(stages);
    }

    public Set<String> stages() {
        return Collections.unmodifiableSet(stages);
    }

    public boolean add(String stage) {
        return stages.add(stage);
    }

    public boolean remove(String stage) {
        return stages.remove(stage);
    }

    public int clear() {
        int n = stages.size();
        stages.clear();
        return n;
    }
}
