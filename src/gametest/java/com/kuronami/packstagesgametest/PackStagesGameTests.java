package com.kuronami.packstagesgametest;

import com.kuronami.packstages.StageAccess;
import com.kuronami.packstages.player.PlayerStages;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Uses the test datapack under {@code data/pack_stages_gametest/pack_stages/}. */
@GameTestHolder("pack_stages")
@PrefixGameTestTemplate(false)
public final class PackStagesGameTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = Vec3.atCenterOf(helper.absolutePos(POS)).add(0, 1, 0);
        p.moveTo(at.x, at.y, at.z);
        return p;
    }

    private static int run(ServerPlayer player, String command) {
        CommandSourceStack src = player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        try {
            return player.server.getCommands().getDispatcher().execute(command, src);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(command + ": " + e.getMessage(), e);
        }
    }

    private static void rightClick(GameTestHelper helper, ServerPlayer player, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
    }

    /** 未解放で画面が開かない・段階を付けると開く (block lock). */
    @GameTest(template = "empty3x3x3")
    public static void lockedBlockOpensAfterStage(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.FURNACE);
        ServerPlayer p = player(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE, 8));
        rightClick(helper, p, POS);
        helper.assertTrue(p.containerMenu == p.inventoryMenu, "furnace opened without the stage");
        // The held block must not be placed on the locked workstation instead.
        helper.assertBlockNotPresent(Blocks.COBBLESTONE, POS.above());
        helper.assertTrue(p.getMainHandItem().getCount() == 8, "held block was placed on the locked furnace");
        helper.assertTrue(run(p, "pack_stages has @s test.smith") == 0, "has should be 0 before add");
        run(p, "pack_stages add @s test.smith");
        helper.assertTrue(run(p, "pack_stages has @s test.smith") == 1, "has should be 1 after add");
        rightClick(helper, p, POS);
        helper.assertTrue(p.containerMenu instanceof FurnaceMenu, "furnace did not open with the stage");
        helper.succeed();
    }

    /** Menu-type lock closes the screen even when the block itself is not listed. */
    @GameTest(template = "empty3x3x3")
    public static void lockedMenuCloses(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.SMOKER);
        ServerPlayer p = player(helper);
        rightClick(helper, p, POS);
        helper.assertTrue(p.containerMenu == p.inventoryMenu, "smoker menu stayed open without the stage");
        run(p, "pack_stages add @s test.menu");
        rightClick(helper, p, POS);
        helper.assertTrue(p.containerMenu instanceof SmokerMenu, "smoker did not open with the stage");
        helper.succeed();
    }

    /** 未解放のアイテムが作れない: the result can't be taken (click or shift-click) until the stage. */
    @GameTest(template = "empty3x3x3")
    public static void lockedCraftResultCannotBeTaken(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.CRAFTING_TABLE);
        ServerPlayer p = player(helper);
        BlockPos abs = helper.absolutePos(POS);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) ->
                new CraftingMenu(id, inv, ContainerLevelAccess.create(helper.getLevel(), abs)), Component.literal("t")));
        CraftingMenu menu = (CraftingMenu) p.containerMenu;
        menu.getSlot(1).set(new ItemStack(Items.OAK_PLANKS));
        menu.getSlot(4).set(new ItemStack(Items.OAK_PLANKS));
        helper.assertTrue(menu.getSlot(0).getItem().is(Items.STICK), "recipe did not produce sticks");

        menu.clicked(0, 0, ClickType.PICKUP, p);
        helper.assertTrue(menu.getCarried().isEmpty(), "took a locked result by click");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, p);
        helper.assertTrue(!p.getInventory().contains(new ItemStack(Items.STICK)), "took a locked result by shift-click");
        helper.assertTrue(menu.getSlot(1).getItem().is(Items.OAK_PLANKS), "ingredients were consumed");

        run(p, "pack_stages add @s test.smith");
        menu.clicked(0, 0, ClickType.PICKUP, p);
        helper.assertTrue(menu.getCarried().is(Items.STICK), "could not take the result with the stage");
        helper.succeed();
    }

    /** Use lock: right-clicking a locked item does nothing until the stage. */
    @GameTest(template = "empty3x3x3")
    public static void lockedItemCannotBeUsed(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENDER_PEARL, 4));
        p.gameMode.useItem(p, helper.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getMainHandItem().getCount() == 4, "locked pearl was thrown");
        run(p, "pack_stages add @s test.smith");
        p.gameMode.useItem(p, helper.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
        helper.assertTrue(p.getMainHandItem().getCount() == 3, "pearl not thrown with the stage");
        helper.succeed();
    }

    /** Double-click gathering must not pull a locked item out of a furnace output. */
    @GameTest(template = "empty3x3x3")
    public static void pickAllSkipsLockedFurnaceOutput(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.FURNACE);
        ServerPlayer p = player(helper);
        p.openMenu((MenuProvider) helper.getBlockEntity(POS));
        AbstractContainerMenu menu = p.containerMenu;
        helper.assertTrue(menu instanceof FurnaceMenu, "furnace menu did not open");
        menu.getSlot(2).set(new ItemStack(Items.STICK, 5));
        menu.setCarried(new ItemStack(Items.STICK, 1));
        int emptyInventorySlot = 3 + 9; // a main-inventory slot of the furnace menu, empty
        menu.clicked(emptyInventorySlot, 0, ClickType.PICKUP_ALL, p);
        helper.assertTrue(menu.getCarried().getCount() == 1, "gathered " + menu.getCarried().getCount() + " locked sticks");
        helper.assertTrue(menu.getSlot(2).getItem().getCount() == 5, "furnace output was drained");
        run(p, "pack_stages add @s test.smith");
        menu.clicked(emptyInventorySlot, 0, ClickType.PICKUP_ALL, p);
        helper.assertTrue(menu.getCarried().getCount() == 6, "could not gather with the stage");
        helper.succeed();
    }

    /** Anvil results hand back an item the player already had; craft locks don't apply there. */
    @GameTest(template = "empty3x3x3")
    public static void anvilResultNotGated(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.ANVIL);
        ServerPlayer p = player(helper);
        p.setGameMode(GameType.CREATIVE); // no level cost
        BlockPos abs = helper.absolutePos(POS);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) ->
                new AnvilMenu(id, inv, ContainerLevelAccess.create(helper.getLevel(), abs)), Component.literal("t")));
        AnvilMenu menu = (AnvilMenu) p.containerMenu;
        menu.getSlot(0).set(new ItemStack(Items.STICK));
        menu.setItemName("renamed");
        helper.assertTrue(menu.getSlot(2).getItem().is(Items.STICK), "anvil produced no result");
        menu.clicked(2, 0, ClickType.PICKUP, p);
        helper.assertTrue(menu.getCarried().is(Items.STICK), "anvil result of a craft-locked item was refused");
        helper.succeed();
    }

    /** Stages survive save/load. */
    @GameTest(template = "empty3x3x3")
    public static void playerDataRoundTrips(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        run(p, "pack_stages add @s test.smith");
        Tag tag = PlayerStages.CODEC.encodeStart(NbtOps.INSTANCE, StageAccess.data(p)).getOrThrow();
        PlayerStages back = PlayerStages.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        helper.assertTrue(back.stages().contains("test.smith"), "stage lost in round trip");
        helper.succeed();
    }
}
