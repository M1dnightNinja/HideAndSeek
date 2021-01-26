package me.m1dnightninja.hideandseek.fabric.mixin;

import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(ClientboundAddPlayerPacket.class)
public interface AccessorPlayerSpawnPacket {
    @Accessor
    UUID getPlayerId();
}
