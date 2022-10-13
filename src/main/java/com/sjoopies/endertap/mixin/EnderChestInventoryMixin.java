package com.sjoopies.endertap.mixin;

import com.sjoopies.endertap.IEnderChestInventory;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({EnderChestInventory.class})
public abstract class EnderChestInventoryMixin extends SimpleInventory implements IEnderChestInventory {
    public EnderChestInventoryMixin() {
        super(new ItemStack[0]);
    }

    @Accessor("activeBlockEntity")
    public abstract EnderChestBlockEntity getActiveBlockEntity();

    public void markDirty() {
        super.markDirty();
        if (this.getActiveBlockEntity() != null) {
            this.getActiveBlockEntity().getWorld().updateComparators(this.getActiveBlockEntity().getPos(), this.getActiveBlockEntity().getWorld().getBlockState(this.getActiveBlockEntity().getPos()).getBlock());
        }

    }
}

