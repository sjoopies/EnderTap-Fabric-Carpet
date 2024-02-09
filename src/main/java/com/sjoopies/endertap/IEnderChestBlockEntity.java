package com.sjoopies.endertap;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public interface IEnderChestBlockEntity {
    void writeNbt(NbtCompound nbtCompound);

    PlayerEntity getOwner();

    void setOwner(PlayerEntity playerEntity);

    boolean isOwnerLoggedOn();

    boolean isOwned();
    
    UUID getOwnerUUID();

    String getOwnerUsername();

    boolean hasOwner();

}
