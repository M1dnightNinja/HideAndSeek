package me.m1dnightninja.hideandseek.fabric.mixin;

import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TextColor.class)
public interface AccessorTextColor {

    @Invoker
    String callFormatValue();
}
