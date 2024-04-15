package com.github.jarva.arsadditions.registry;

import com.github.jarva.arsadditions.datagen.conditions.ConfigCondition;
import com.hollingsworth.arsnouveau.api.loot.DungeonLootTables;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;

public class AddonSetup {
    public static void registers(IEventBus modEventBus) {
        CraftingHelper.register(ConfigCondition.Serializer.INSTANCE);

        AddonBlockRegistry.BLOCKS.register(modEventBus);
        AddonBlockRegistry.BLOCK_ENTITIES.register(modEventBus);
        AddonItemRegistry.ITEMS.register(modEventBus);
        AddonLootItemFunctionsRegistry.FUNCTION_TYPES.register(modEventBus);
        AddonEffectRegistry.EFFECTS.register(modEventBus);

        DungeonLootTables.BASIC_LOOT.add(() -> new ItemStack(AddonItemRegistry.CODEX_ENTRY.get(), 1));
    }
}
