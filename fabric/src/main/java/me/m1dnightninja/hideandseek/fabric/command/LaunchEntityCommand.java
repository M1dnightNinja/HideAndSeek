package me.m1dnightninja.hideandseek.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class LaunchEntityCommand {

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("launchentity")
            .then(Commands.argument("entities", EntityArgument.entities())
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                            .executes(context -> execute(context, context.getArgument("entities", EntitySelector.class).findEntities(context.getSource()), context.getArgument("x", double.class), context.getArgument("y", double.class), context.getArgument("z", double.class)))
                        )
                    )
                )
            )
        );
    }

    private int execute(CommandContext<CommandSourceStack> context, List<? extends Entity> entities, double x, double y, double z) {

        for(Entity ent : entities) {
            ent.setDeltaMovement(x, y, z);
            if(ent instanceof ServerPlayer) {
                ((ServerPlayer) ent).connection.send(new ClientboundSetEntityMotionPacket(ent));
            }
        }

        context.getSource().sendSuccess(new TextComponent("Launched " + entities.size() + (entities.size() == 1 ? " entity" : " entities")), true);

        return entities.size();
    }

}
