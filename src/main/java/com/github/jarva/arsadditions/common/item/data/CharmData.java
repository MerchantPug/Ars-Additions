package com.github.jarva.arsadditions.common.item.data;

import com.github.jarva.arsadditions.setup.registry.AddonDataComponentRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record CharmData(int charges) {
    public static Codec<CharmData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("charges", 0).forGetter(CharmData::charges)
    ).apply(instance, CharmData::new));

    public static StreamCodec<RegistryFriendlyByteBuf, CharmData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, CharmData::charges, CharmData::new
    );

    public static CharmData DEFAULT_DATA = new CharmData(0);

    public CharmData use(int charges) {
        return new CharmData(Math.max(this.charges - charges, 0));
    }

    public static CharmData getOrDefault(ItemStack stack) {
        return stack.getOrDefault(AddonDataComponentRegistry.CHARM_DATA, DEFAULT_DATA);
    }

    public static CharmData getOrDefault(ItemStack stack, int charges) {
        return stack.getOrDefault(AddonDataComponentRegistry.CHARM_DATA, new CharmData(charges));
    }
}
