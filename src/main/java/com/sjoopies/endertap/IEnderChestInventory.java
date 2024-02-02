package com.sjoopies.endertap;

import net.minecraft.block.entity.EnderChestBlockEntity;

public interface IEnderChestInventory {
    void setActiveBlockEntity(EnderChestBlockEntity enderChestBlockEntity);
    boolean isActiveBlockEntity(EnderChestBlockEntity enderChestBlockEntity);
}
