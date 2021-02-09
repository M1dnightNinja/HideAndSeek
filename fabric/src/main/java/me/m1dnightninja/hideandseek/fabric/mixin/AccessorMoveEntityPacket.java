package me.m1dnightninja.hideandseek.fabric.mixin;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundMoveEntityPacket.class)
public interface AccessorMoveEntityPacket {
    @Accessor
    int getEntityId();
}
