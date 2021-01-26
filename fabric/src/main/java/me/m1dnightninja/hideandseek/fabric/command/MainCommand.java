package me.m1dnightninja.hideandseek.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.fabric.EditingSession;
import me.m1dnightninja.hideandseek.fabric.HideAndSeek;
import me.m1dnightninja.hideandseek.fabric.Lobby;
import me.m1dnightninja.hideandseek.fabric.LobbySession;
import me.m1dnightninja.midnightcore.api.AbstractInventoryGUI;
import me.m1dnightninja.midnightcore.fabric.api.InventoryGUI;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MainCommand {

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("has")
            .then(Commands.literal("join")
                // TODO: perms (.requires("hideandseek.command.join"))
                .executes(context -> joinCommand(context, null, null))
                .then(Commands.argument("lobby", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(HideAndSeekAPI.getInstance().getSessionManager().getLobbyNames(HideAndSeekAPI.getInstance().getSessionManager().getOpenLobbies(context.getSource().getPlayerOrException().getUUID())), builder))
                    .executes(context -> joinCommand(context, context.getArgument("lobby", String.class), null))
                    .then(Commands.argument("players", EntityArgument.players())
                        // .requires("hideandseek.command.join.others")
                        .executes(context -> joinCommand(context, context.getArgument("lobby", String.class), context.getArgument("players", EntitySelector.class).findPlayers(context.getSource())))
                    )
                )
            )
            .then(Commands.literal("leave")
                .executes(context -> leaveCommand(context, null))
                .then(Commands.argument("players", EntityArgument.players())
                    // .requires("hideandseek.command.leave.others")
                    .executes(context -> leaveCommand(context, context.getArgument("players", EntitySelector.class).findPlayers(context.getSource())))
                )
            )
            .then(Commands.literal("start")
                .executes(context -> startCommand(context, null, null))
                .then(Commands.argument("map", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(HideAndSeekAPI.getInstance().getSessionManager().getEditableMapNames(null), builder))
                    .executes(context -> startCommand(context, null, context.getArgument("map", String.class)))
                    .then(Commands.argument("player",  EntityArgument.player())
                        .executes(context -> startCommand(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource()), context.getArgument("map", String.class)))
                    )
                )
                .then(Commands.argument("player",  EntityArgument.player())
                    .executes(context -> startCommand(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource()), null))
                )
            )
            .then(Commands.literal("reload")
                .executes(this::reloadCommand)
            )
            .then(Commands.literal("edit")
                .then(Commands.argument("map", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(HideAndSeekAPI.getInstance().getSessionManager().getEditableMapNames(context.getSource().getPlayerOrException().getUUID()), builder))
                    .then(Commands.argument("players", EntityArgument.players())
                        .executes(context -> editCommand(context, context.getArgument("map", String.class), context.getArgument("players", EntitySelector.class).findPlayers(context.getSource())))
                    )
                )
            )
        );

    }

    private int joinCommand(CommandContext<CommandSourceStack> context, String lobby, List<ServerPlayer> players) {

        ServerPlayer sender = null;
        if(players == null) {
            try {
                sender = context.getSource().getPlayerOrException();
                players = Collections.singletonList(sender);
            } catch(CommandSyntaxException ex) {
                context.getSource().sendFailure(new TextComponent("Only players can use that command!"));
                return 0;
            }
        }

        if(lobby == null) {

            try {
                final ServerPlayer player = sender;
                if (sender == null) return 0;

                List<AbstractLobby> lobbies = HideAndSeekAPI.getInstance().getSessionManager().getOpenLobbies(player.getUUID());

                if (lobbies.size() == 0) {
                    context.getSource().sendFailure(new TextComponent("There are no open lobbies!"));
                    return 0;
                }

                InventoryGUI gui = new InventoryGUI("Join a Lobby");

                int index = 0;
                for (AbstractLobby lby : lobbies) {

                    ItemStack display;
                    if (lby instanceof Lobby) {
                        display = ((Lobby) lby).getDisplayStack();
                    } else {
                        display = Lobby.createDefaultItem(lby);
                    }

                    gui.setItem(display, index, type -> {
                        if (type == AbstractInventoryGUI.ClickType.LEFT) {
                            joinCommand(context, lby.getId(), Collections.singletonList(player));
                        }
                    });

                    index++;
                }

                gui.open(player.getUUID(), 0);

            } catch(Exception ex) {
                ex.printStackTrace();
            }
            return 1;
        }

        AbstractLobby lby = HideAndSeekAPI.getInstance().getRegistry().getLobby(lobby);
        if(lby == null) {
            context.getSource().sendFailure(new TextComponent("That is not a valid lobby!"));
            return 0;
        }

        if(sender != null && !lby.canAccess(sender.getUUID())) {
            context.getSource().sendFailure(new TextComponent("That is not a valid lobby!"));
            return 0;
        }

        AbstractLobbySession sess = HideAndSeekAPI.getInstance().getSessionManager().getActiveSession(lby);
        if(sess == null) {

            sess = new LobbySession(lby);
            HideAndSeekAPI.getInstance().getSessionManager().startSession(sess);
        }

        for(ServerPlayer player : players) {
            if(!sess.addPlayer(player.getUUID())) {
                context.getSource().sendSuccess(new TextComponent("Unable to add ").append(player.getDisplayName()).append(" to the lobby!").setStyle(Style.EMPTY.withColor(ChatFormatting.RED)), false);
            }
        }

        return players.size();
    }

    private int leaveCommand(CommandContext<CommandSourceStack> context, List<ServerPlayer> players) {

        if(players == null) {
            try {
                players = Collections.singletonList(context.getSource().getPlayerOrException());
            } catch(CommandSyntaxException ex) {
                context.getSource().sendFailure(new TextComponent("Only players can use that command!"));
                return 0;
            }
        }

        for(ServerPlayer player : players) {
            AbstractSession sess = HideAndSeekAPI.getInstance().getSessionManager().getSession(player.getUUID());
            if(sess == null) {
                context.getSource().sendFailure(new TextComponent("Unable to remove ").append(player.getDisplayName()).append(" from their session!"));
            } else {
                sess.removePlayer(player.getUUID());
            }
        }

        return players.size();

    }

    private int startCommand(CommandContext<CommandSourceStack> context, ServerPlayer player, String map) {

        try {
            ServerPlayer sender;
            try {
                sender = context.getSource().getPlayerOrException();
            } catch (CommandSyntaxException ex) {
                context.getSource().sendFailure(new TextComponent("Only players can use that command!"));
                return 0;
            }

            AbstractSession sess = HideAndSeekAPI.getInstance().getSessionManager().getSession(sender.getUUID());

            if (!(sess instanceof AbstractLobbySession)) {
                context.getSource().sendFailure(new TextComponent("You are not in a lobby!"));
                return 0;
            }

            UUID seeker = player == null ? null : player.getUUID();
            AbstractMap mp = null;

            if(map != null) {
                mp = HideAndSeekAPI.getInstance().getRegistry().getMap(map);
                if(mp == null) {
                    context.getSource().sendFailure(new TextComponent("That is not a valid map!"));
                    return 0;
                }
            }

            ((AbstractLobbySession) sess).startGame(seeker, mp);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return 1;
    }

    private int reloadCommand(CommandContext<CommandSourceStack> context) {

        long time = System.currentTimeMillis();

        HideAndSeek.getInstance().reload();

        long elapsed = System.currentTimeMillis() - time;
        context.getSource().sendSuccess(new TextComponent(String.format("HideAndSeek reloaded in %sms", elapsed)), false);

        return 1;
    }

    private int editCommand(CommandContext<CommandSourceStack> context, String map, List<ServerPlayer> players) {

        AbstractMap mp = HideAndSeekAPI.getInstance().getRegistry().getMap(map);
        if(mp == null) {
            context.getSource().sendFailure(new TextComponent("That is not a valid map!"));
            return 0;
        }

        EditingSession sess = new EditingSession(mp);
        HideAndSeekAPI.getInstance().getSessionManager().startSession(sess);

        for(ServerPlayer player : players) {
            if(!sess.addPlayer(player.getUUID())) {
                context.getSource().sendFailure(new TextComponent("Unable to add ").append(player.getDisplayName()).append(" to the editing session!"));
            }
        }

        return players.size();
    }

}
