package com.sjoopies.endertap.mixin;

import com.sjoopies.endertap.EnderTap;
import com.sjoopies.endertap.EnderTapSettings;
import com.sjoopies.endertap.IEnderChestBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Mixin({EnderChestBlock.class})
public abstract class EnderChestBlockMixin extends AbstractChestBlock<EnderChestBlockEntity> implements Waterloggable {

    private static final BooleanProperty OWNED = BooleanProperty.of("owned");
    private static final Text CONTAINER_NAME = Text.translatable("container.enderchest");

    protected EnderChestBlockMixin(Settings settings, Supplier<BlockEntityType<? extends EnderChestBlockEntity>> blockEntityTypeSupplier) {
        super(settings, blockEntityTypeSupplier);
    }

    @Inject(
            at = {@At("TAIL")},
            method = {"<init>(Lnet/minecraft/block/AbstractBlock$Settings;)V"}
    )
    public void init(Settings settings, CallbackInfo cb) {
        this.setDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(EnderChestBlock.FACING, Direction.NORTH)).with(EnderChestBlock.WATERLOGGED, false)).with(OWNED, false));
    }

    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        ItemStack itemStack = super.getPickStack(world, pos, state);
        if (world.getBlockEntity(pos) != null && ((IEnderChestBlockEntity)world.getBlockEntity(pos)).hasOwner()) {
            ((IEnderChestBlockEntity)world.getBlockEntity(pos)).writeNbt(itemStack.getOrCreateNbt());
        }

        return itemStack;
    }

    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        super.appendTooltip(stack, world, tooltip, options);
        NbtCompound nbtCompound = stack.getNbt();
        if (nbtCompound != null && nbtCompound.contains("ownerUsername")) {
            tooltip.add(Text.literal(nbtCompound.getString("ownerUsername")));
        }

    }

    @Overwrite
    public void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{EnderChestBlock.FACING, EnderChestBlock.WATERLOGGED, OWNED});
    }

    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }

    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (EnderTapSettings.enderTap && placer instanceof PlayerEntity && world.getBlockEntity(pos) != null) {
            PlayerEntity player = (PlayerEntity)placer;
            if (player.isSneaking()) {
                world.setBlockState(pos, (BlockState)state.with(OWNED, true));
                ((IEnderChestBlockEntity)world.getBlockEntity(pos)).setOwner(player);
            }

            NbtCompound nbtCompound = stack.getNbt();
            if (nbtCompound != null && nbtCompound.contains("ownerUsername") && nbtCompound.contains("ownerUUID")) {
                world.getBlockEntity(pos).readNbt(nbtCompound);
                world.setBlockState(pos, (BlockState)state.with(OWNED, true));
            }
        }

    }

    @Overwrite
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (EnderTapSettings.enderTap && (Boolean)state.get(OWNED)) {
            if (world.getBlockEntity(pos) == null) {
                return ActionResult.success(world.isClient);
            } else if (((IEnderChestBlockEntity)world.getBlockEntity(pos)).hasOwner()) {
                UUID playerUUID = ((IEnderChestBlockEntity)world.getBlockEntity(pos)).getOwnerUUID();
                String playerUsername = ((IEnderChestBlockEntity)world.getBlockEntity(pos)).getOwnerUsername();
                PlayerEntity playerEntity = world.getPlayerByUuid(playerUUID);
                if (!world.isClient()) {
                    player.sendMessage(Text.of(playerUsername), true);
                }

                if (playerEntity != null) {
                    EnderChestInventory enderChestInventory = playerEntity.getEnderChestInventory();
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (enderChestInventory != null && blockEntity instanceof EnderChestBlockEntity) {
                        BlockPos blockPos = pos.up();
                        if (world.getBlockState(blockPos).isSolidBlock(world, blockPos)) {
                            return ActionResult.success(world.isClient);
                        } else if (world.isClient) {
                            return ActionResult.SUCCESS;
                        } else {
                            EnderChestBlockEntity enderChestBlockEntity = (EnderChestBlockEntity)blockEntity;
                            enderChestInventory.setActiveBlockEntity(enderChestBlockEntity);
                            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inventory, playerx) -> {
                                return GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, enderChestInventory);
                            }, Text.of(playerUsername)));
                            player.incrementStat(Stats.OPEN_ENDERCHEST);
                            PiglinBrain.onGuardedBlockInteracted(player, true);
                            return ActionResult.CONSUME;
                        }
                    } else {
                        return ActionResult.success(world.isClient);
                    }
                } else {
                    return ActionResult.CONSUME;
                }
            } else {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (!world.isClient())
                    EnderTap.LOGGER.error("EnderChest is owned, but does not have a player (owner) assigned. USERNAME: " + ((IEnderChestBlockEntity)blockEntity).getOwnerUsername());
                return ActionResult.CONSUME;
            }
        } else {
            EnderChestInventory enderChestInventory = player.getEnderChestInventory();
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (enderChestInventory != null && blockEntity instanceof EnderChestBlockEntity) {
                BlockPos blockPos = pos.up();
                if (world.getBlockState(blockPos).isSolidBlock(world, blockPos)) {
                    return ActionResult.success(world.isClient);
                } else if (world.isClient) {
                    return ActionResult.SUCCESS;
                } else {
                    EnderChestBlockEntity enderChestBlockEntity = (EnderChestBlockEntity)blockEntity;
                    enderChestInventory.setActiveBlockEntity(enderChestBlockEntity);
                    player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inventory, playerx) -> {
                        return GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, enderChestInventory);
                    }, CONTAINER_NAME));
                    player.incrementStat(Stats.OPEN_ENDERCHEST);
                    PiglinBrain.onGuardedBlockInteracted(player, true);
                    return ActionResult.CONSUME;
                }
            } else {
                return ActionResult.success(world.isClient);
            }
        }
    }
}
