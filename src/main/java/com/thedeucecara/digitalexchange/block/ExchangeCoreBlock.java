package com.thedeucecara.digitalexchange.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ExchangeCoreBlock extends Block implements EntityBlock {

    public ExchangeCoreBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(3.5f, 6.0f)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExchangeCoreBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof ExchangeCoreBlockEntity core) {
                core.openMenu(serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof ExchangeCoreBlockEntity core) {
                // Ensure owner is set if placer is player
                if (placer instanceof Player player) {
                    core.getMainNode().setOwningPlayer(player);
                }
            }
            for (Direction dir : Direction.values()) {
                level.neighborChanged(pos.relative(dir), this, pos);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            for (Direction dir : Direction.values()) {
                level.neighborChanged(pos.relative(dir), this, pos);
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ExchangeCoreBlockEntity core) {
            // Wake up node if level didn't trigger it yet
            if (core.getMainNode().getNode() == null) {
                core.getMainNode().create(level, pos);
            }
        }
    }
}
