package com.kuronami.packstages.network;

import com.kuronami.packstages.PackStages;
import com.kuronami.packstages.client.ClientStageState;
import com.kuronami.packstages.data.StageDefinition;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/** Server → client sync of stage definitions and the player's own stages. */
public final class SyncPayloads {
    private SyncPayloads() {}

    public record Definitions(List<StageDefinition> definitions) implements CustomPacketPayload {
        public static final Type<Definitions> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(PackStages.MODID, "definitions"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Definitions> STREAM_CODEC =
                ByteBufCodecs.fromCodecWithRegistries(StageDefinition.NETWORK_CODEC.listOf())
                        .map(Definitions::new, Definitions::definitions);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Stages(List<String> stages) implements CustomPacketPayload {
        public static final Type<Stages> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(PackStages.MODID, "stages"));
        public static final StreamCodec<ByteBuf, Stages> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(Stages::new, Stages::stages);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(Definitions.TYPE, Definitions.STREAM_CODEC,
                (payload, ctx) -> ClientStageState.setDefinitions(payload.definitions()));
        registrar.playToClient(Stages.TYPE, Stages.STREAM_CODEC,
                (payload, ctx) -> ClientStageState.setStages(payload.stages()));
    }
}
