package com.sjoopies.endertap;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public interface IEnderChestBlockEntity {
    void writeNbt(NbtCompound nbtCompound);

    PlayerEntity getOwnerPTR();

    boolean isOwnerLoggedOn();

    boolean isOwned();

    void tryToSetOwnerPTR();

    void setOwner(PlayerEntity playerEntity);

    UUID getOwnerUUID();

    String getOwnerUsername();

    boolean hasOwner();


}
