package com.kuronami.packstages.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Saved per player: the stages they hold and the milestones already paid out. */
public final class PlayerStages {
    public static final Codec<PlayerStages> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("stages", List.of()).forGetter(p -> List.copyOf(p.stages)),
            Codec.STRING.listOf().optionalFieldOf("milestones", List.of()).forGetter(p -> List.copyOf(p.milestones))
    ).apply(i, PlayerStages::new));

    private final Set<String> stages = new LinkedHashSet<>();
    private final Set<String> milestones = new LinkedHashSet<>();

    public PlayerStages() {}

    private PlayerStages(List<String> stages, List<String> milestones) {
        this.stages.addAll(stages);
        this.milestones.addAll(milestones);
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

    /** Records a milestone key; returns false if it was already recorded. */
    public boolean markMilestone(String key) {
        return milestones.add(key);
    }

    /** Forgets the records of one milestone id, or of all milestones when {@code id} is null. */
    public int resetMilestones(String id) {
        int before = milestones.size();
        if (id == null) milestones.clear();
        else milestones.removeIf(k -> k.equals(id) || k.startsWith(id + "|"));
        return before - milestones.size();
    }
}
