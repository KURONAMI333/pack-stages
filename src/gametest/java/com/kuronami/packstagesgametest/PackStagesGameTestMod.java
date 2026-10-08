package com.kuronami.packstagesgametest;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Dev-only companion mod. The GameTests are found through {@code @GameTestHolder}; the {@code /pst_dev}
 * command lets a dev run act for the connected player without sending keyboard or mouse input.
 */
@Mod("pack_stages_gametest")
public final class PackStagesGameTestMod {
    public PackStagesGameTestMod() {
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> DevCommands.register(e.getDispatcher()));
    }
}
