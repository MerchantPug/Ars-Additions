package com.github.jarva.arsadditions.common.block;

import com.github.jarva.arsadditions.common.block.tile.WarpNexusTile;
import com.github.jarva.arsadditions.common.capability.CapabilityRegistry;
import com.github.jarva.arsadditions.common.menu.WarpNexusMenu;
import com.github.jarva.arsadditions.setup.networking.OpenNexusPacket;
import com.hollingsworth.arsnouveau.common.block.ITickableBlock;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class WarpNexus extends Block implements EntityBlock, ITickableBlock {
    public static final BooleanProperty REQUIRES_SOURCE = BooleanProperty.create("requires_source");
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public WarpNexus() {
        super(Properties.of().sound(SoundType.STONE).strength(3.0f, 6.0f).lightLevel((b) -> 8).noOcclusion().pushReaction(PushReaction.BLOCK).mapColor(MapColor.STONE));
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(REQUIRES_SOURCE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, REQUIRES_SOURCE);
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        WarpNexusTile be = WarpNexusTile.getWarpNexus(level, pos).orElse(null);

        if (be == null) return InteractionResult.FAIL;

        if (!be.getStack().isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                ItemStack item = Capabilities.ItemHandler.BLOCK.getCapability(level, pos, state, be, null).extractItem(0, Item.ABSOLUTE_MAX_STACK_SIZE, false);
                serverPlayer.getInventory().add(item);
            }
            return InteractionResult.SUCCESS;
        }

        if (player.isSecondaryUseActive()) {
            if (player.getMainHandItem().getItem() instanceof SpellBook) {
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(state.getMenuProvider(level, be.getBlockPos()));
            }
        } else {
            OpenNexusPacket.openNexus(player, be.getBlockPos());
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((i, inventory, player) -> {
            ItemStackHandler itemStackHandler = player.getCapability(CapabilityRegistry.PLAYER_NEXUS);
            return new WarpNexusMenu(i, inventory, ContainerLevelAccess.create(level, pos), itemStackHandler);
        }, Component.translatable("block.ars_additions.warp_nexus"));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarpNexusTile(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf doubleBlockHalf = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && doubleBlockHalf == DoubleBlockHalf.LOWER == (direction == Direction.UP)) {
            return neighborState.is(this) && neighborState.getValue(HALF) != doubleBlockHalf ? state : Blocks.AIR.defaultBlockState();
        } else {
            return doubleBlockHalf == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }
}
