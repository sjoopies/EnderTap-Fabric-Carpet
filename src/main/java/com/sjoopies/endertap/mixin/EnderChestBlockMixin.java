package com.sjoopies.endertap.mixin;

import com.sjoopies.endertap.EnderTap;
import com.sjoopies.endertap.EnderTapSettings;
import com.sjoopies.endertap.IEnderChestBlockEntity;
import net.minecraft.block.AbstractChestBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.Waterloggable;
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
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Mixin({EnderChestBlock.class})
public abstract class EnderChestBlockMixin extends AbstractChestBlock<EnderChestBlockEntity> implements Waterloggable {
    @Unique
    private static final Text CONTAINER_NAME = Text.translatable("container.enderchest");

    protected EnderChestBlockMixin(Settings settings,
                                   Supplier<BlockEntityType<? extends EnderChestBlockEntity>> blockEntityTypeSupplier) {
        super(settings, blockEntityTypeSupplier);
    }


    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        ItemStack itemStack = super.getPickStack(world, pos, state);
        if (world.getBlockEntity(pos) == null
                || !(world.getBlockEntity(pos) instanceof IEnderChestBlockEntity enderChestBlockEntity))
            return itemStack;
        if (enderChestBlockEntity.isOwned() && enderChestBlockEntity.hasOwner())
            enderChestBlockEntity.writeNbt(itemStack.getOrCreateNbt());
        return itemStack;
    }

    public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
        super.appendTooltip(stack, world, tooltip, options);
        NbtCompound nbtCompound = stack.getNbt();
        if (nbtCompound == null || !nbtCompound.contains("ownerUsername"))
            return;
        tooltip.add(Text.literal(nbtCompound.getString("ownerUsername")));
    }

    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) == null)
            return 0;
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }

    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!EnderTapSettings.enderTap
                || !(placer instanceof PlayerEntity player)
                || world.getBlockEntity(pos) == null)
            return;
        if (player.isSneaking()) {
            ((IEnderChestBlockEntity) world.getBlockEntity(pos)).setOwner(player);
        }
        NbtCompound nbtCompound = stack.getNbt();
        if (nbtCompound != null
                && nbtCompound.contains("ownerUsername")
                && nbtCompound.contains("ownerUUID")) {
            world.getBlockEntity(pos).readNbt(nbtCompound);
        }
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) == null)
            return ActionResult.success(world.isClient);
        if (!(world.getBlockEntity(pos) instanceof IEnderChestBlockEntity enderChestBlockEntity))
            return ActionResult.FAIL;
        if (!EnderTapSettings.enderTap || !enderChestBlockEntity.isOwned()) {
            // default
            EnderChestInventory enderChestInventory = player.getEnderChestInventory();
            if (enderChestInventory != null) {
                BlockPos blockPos = pos.up();
                if (world.getBlockState(blockPos).isSolidBlock(world, blockPos)) {
                    return ActionResult.success(world.isClient);
                } else if (world.isClient) {
                    return ActionResult.SUCCESS;
                } else {
                    enderChestInventory.setActiveBlockEntity((EnderChestBlockEntity) world.getBlockEntity(pos));
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
        if (!enderChestBlockEntity.hasOwner()) {
            if (!world.isClient())
                EnderTap.LOGGER.error("EnderChest is owned, but does not have a player (owner) assigned. USERNAME: " +
                        enderChestBlockEntity.getOwnerUsername());
            return ActionResult.CONSUME;
        }

        UUID playerUUID = enderChestBlockEntity.getOwnerUUID();
        String playerUsername = enderChestBlockEntity.getOwnerUsername();
        PlayerEntity playerEntity = world.getPlayerByUuid(playerUUID);
        if (!world.isClient())
            player.sendMessage(Text.of(playerUsername), true);
        if (playerEntity == null)
            return ActionResult.CONSUME;
        EnderChestInventory enderChestInventory = playerEntity.getEnderChestInventory();
        if (enderChestInventory == null || !(enderChestBlockEntity instanceof EnderChestBlockEntity ecbe))
            return ActionResult.FAIL;

        BlockPos blockPos = pos.up();
        if (world.getBlockState(blockPos).isSolidBlock(world, blockPos)) {
            return ActionResult.success(world.isClient);
        } else if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        enderChestInventory.setActiveBlockEntity(ecbe);
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inventory, playerx) -> {
            return GenericContainerScreenHandler.createGeneric9x3(syncId, inventory, enderChestInventory);
        }, Text.of(playerUsername)));
        player.incrementStat(Stats.OPEN_ENDERCHEST);
        PiglinBrain.onGuardedBlockInteracted(player, true);
        return ActionResult.CONSUME;


    }
}
