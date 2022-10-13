package com.sjoopies.endertap.mixin;

import com.sjoopies.endertap.EnderTapSettings;
import com.sjoopies.endertap.IEnderChestBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.LidOpenable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin({EnderChestBlockEntity.class})
public abstract class EnderChestBlockEntityMixin extends BlockEntity implements Inventory, LidOpenable, IEnderChestBlockEntity {
    private boolean owned = false;
    private String ownerUsername = null;
    private UUID ownerUUID = null;
    private PlayerEntity ownerPTR = null;
    private static DefaultedList<ItemStack> EMPTY_INV = DefaultedList.ofSize(0);

    public EnderChestBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.ownerUsername = nbt.getString("ownerUsername");
        this.ownerUUID = nbt.getUuid("ownerUUID");
        this.owned = nbt.getBoolean("owned");
        if (this.hasOwner()) {
            this.tryToSetOwnerPTR();
        }

    }

    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (this.hasOwner()) {
            nbt.putString("ownerUsername", this.ownerUsername);
            nbt.putUuid("ownerUUID", this.ownerUUID);
            nbt.putBoolean("owned", this.owned);
        }

    }

    public boolean isOwned() {
        return owned;
    }

    public PlayerEntity getOwnerPTR() {
        return this.ownerPTR;
    }

    public boolean isOwnerLoggedOn() {
        return this.ownerUUID != null && this.getWorld() != null && this.getWorld().getPlayerByUuid(this.ownerUUID) != null;
    }

    public void tryToSetOwnerPTR() {
        if (this.getWorld() != null) {
            if (!this.getWorld().isClient()) {
                PlayerEntity pe = this.getWorld().getPlayerByUuid(this.ownerUUID);
                if (pe != null && this.ownerUsername.equals(pe.getGameProfile().getName())) {
                    this.ownerPTR = pe;
                } else {
                    this.ownerPTR = null;
                }

            }
        }
    }

    public void setOwner(PlayerEntity player) {
        this.ownerUsername = player.getGameProfile().getName();
        this.ownerUUID = player.getGameProfile().getId();
        if (this.hasOwner()) {
            this.tryToSetOwnerPTR();
        }
        this.owned = true;

    }

    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    public String getOwnerUsername() {
        return this.ownerUsername;
    }

    public boolean hasOwner() {
        return this.ownerUUID != null && this.ownerUsername != null;
    }

    public EnderChestInventory getOwnerInventory() {
        if (!EnderTapSettings.enderTap) {
            return null;
        } else if (!this.hasOwner()) {
            return null;
        } else if (!this.isOwnerLoggedOn()) {
            return null;
        } else if (this.getOwnerPTR() != null) {
            return this.getOwnerPTR().getEnderChestInventory();
        } else {
            return this.getWorld() != null ? this.getWorld().getPlayerByUuid(this.ownerUUID).getEnderChestInventory() : null;
        }
    }

    public boolean hasOwnerInventory() {
        return this.getOwnerInventory() != null;
    }

    public int size() {
        return EnderTapSettings.enderTap && this.hasOwnerInventory() ? this.getOwnerInventory().size() : 0;
    }

    public boolean isEmpty() {
        return EnderTapSettings.enderTap && this.hasOwnerInventory() ? this.getOwnerInventory().isEmpty() : false;
    }

    public ItemStack getStack(int slot) {
        return EnderTapSettings.enderTap && this.hasOwnerInventory() ? this.getOwnerInventory().getStack(slot) : ItemStack.EMPTY;
    }

    public ItemStack removeStack(int slot, int amount) {
        if (EnderTapSettings.enderTap && this.hasOwnerInventory()) {
            ItemStack ret = this.getOwnerInventory().removeStack(slot, amount);
            this.markDirty();
            return ret;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public ItemStack removeStack(int slot) {
        if (EnderTapSettings.enderTap && this.hasOwnerInventory()) {
            ItemStack ret = this.getOwnerInventory().removeStack(slot);
            this.markDirty();
            return ret;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public void setStack(int slot, ItemStack stack) {
        if (EnderTapSettings.enderTap && this.hasOwnerInventory()) {
            this.getOwnerInventory().setStack(slot, stack);
            this.markDirty();
        }

    }

    public void markDirty() {
        if (EnderTapSettings.enderTap && this.getWorld() != null && this.hasOwnerInventory()) {
            this.getOwnerInventory().markDirty();
            this.getWorld().updateComparators(this.getPos(), this.getWorld().getBlockState(this.getPos()).getBlock());
        }

    }
}
