package me.m1dnightninja.hideandseek.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.core.HideAndSeekRegistry;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.fabric.game.EditingSession;
import me.m1dnightninja.hideandseek.fabric.HideAndSeek;
import me.m1dnightninja.hideandseek.fabric.game.LobbySession;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.inventory.AbstractInventoryGUI;
import me.m1dnightninja.midnightcore.api.module.lang.CustomPlaceholderInline;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.fabric.api.PermissionHelper;
import me.m1dnightninja.midnightcore.fabric.module.lang.LangModule;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MainCommand {

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("has")
            .requires(context -> hasPermission(context, "hideandseek.command"))
            .then(Commands.literal("join")
                .requires(context -> hasPermission(context, "hideandseek.command.join"))
                .executes(context -> joinCommand(context, null, null))
                .then(Commands.argument("lobby", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(HideAndSeekAPI.getInstance().getSessionManager().getLobbyNames(HideAndSeekAPI.getInstance().getSessionManager().getOpenLobbies(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(context.getSource().getPlayerOrException().getUUID()))), builder))
                    .executes(context -> joinCommand(context, context.getArgument("lobby", String.class), null))
                    .then(Commands.argument("players", EntityArgument.players())
                        .requires(context -> hasPermission(context, "hideandseek.command.join.others"))
                        .executes(context -> joinCommand(context, context.getArgument("lobby", String.class), context.getArgument("players", EntitySelector.class).findPlayers(context.getSource())))
                    )
                )
            )
            .then(Commands.literal("leave")
                .requires(context -> hasPermission(context, "hideandseek.command.leave"))
                .executes(context -> leaveCommand(context, Collections.singletonList(context.getSource().getPlayerOrException())))
                .then(Commands.argument("players", EntityArgument.players())
                    .requires(context -> hasPermission(context, "hideandseek.command.leave.others"))
                    .executes(context -> leaveCommand(context, context.getArgument("players", EntitySelector.class).findPlayers(context.getSource())))
                )
            )
            .then(Commands.literal("start")
                .requires(context -> hasPermission(context, "hideandseek.command.start"))
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
                .requires(context -> hasPermission(context, "hideandseek.command.reload"))
                .executes(this::reloadCommand)
            )
            .then(Commands.literal("edit")
                .requires(context -> hasPermission(context, "hideandseek.command.edit"))
                .then(Commands.argument("map", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(HideAndSeekAPI.getInstance().getSessionManager().getEditableMapNames(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(context.getSource().getPlayerOrException().getUUID())), builder))
                    .executes(context -> editCommand(context, context.getArgument("map", String.class), Collections.singletonList(context.getSource().getPlayerOrException())))
                    .then(Commands.argument("players", EntityArgument.players())
                        .requires(context -> hasPermission(context, "hideandseek.command.edit.others"))
                        .executes(context -> editCommand(context, context.getArgument("map", String.class), context.getArgument("players", EntitySelector.class).findPlayers(context.getSource())))
                    )
                )
            )
            .then(Commands.literal("customize")
                .requires(context -> hasPermission(context, "hideandseek.command.customize"))
                .executes(context -> customizeCommand(context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(context -> hasPermission(context, "hideandseek.command.customize.others"))
                    .executes(context -> customizeCommand(context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                )
            )
        );

    }

    private boolean hasPermission(CommandSourceStack st, String perm) {

        return PermissionHelper.checkOrOp(st, perm, 2);
    }

    private int joinCommand(CommandContext<CommandSourceStack> context, String lobby, List<ServerPlayer> players) {

        ServerPlayer sender = null;
        if(players == null) {
            try {
                sender = context.getSource().getPlayerOrException();
                players = Collections.singletonList(sender);
            } catch(CommandSyntaxException ex) {
                LangModule.sendCommandFailure(context, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.no_console");
                return 0;
            }
        }

        if(lobby == null) {

            try {
                final ServerPlayer player = sender;
                if (sender == null) return 0;

                MPlayer pl = FabricPlayer.wrap(player);

                List<Lobby> lobbies = HideAndSeekAPI.getInstance().getSessionManager().getOpenLobbies(pl);

                if (lobbies.size() == 0) {
                    LangModule.sendCommandFailure(context, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.no_lobbies");
                    return 0;
                }

                HideAndSeekAPI.getInstance().getRegistry().openLobbyGUI(pl, lby -> lby.canAccess(pl), (lby, click) -> {
                    if(click == AbstractInventoryGUI.ClickType.LEFT) {
                        joinCommand(context, lby.getId(), Collections.singletonList(player));
                        player.closeContainer();
                    }
                });

            } catch(Exception ex) {
                ex.printStackTrace();
            }
            return 1;
        }

        Lobby lby = HideAndSeekAPI.getInstance().getRegistry().getLobby(lobby);
        if(lby == null) {
            LangModule.sendCommandFailure(context, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.invalid_lobby");
            return 0;
        }

        if(sender != null && !lby.canAccess(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(sender.getUUID()))) {
            LangModule.sendCommandFailure(context, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.invalid_lobby");
            return 0;
        }

        AbstractLobbySession sess = HideAndSeekAPI.getInstance().getSessionManager().getActiveSession(lby);
        if(sess == null) {

            sess = new LobbySession(lby);
            HideAndSeekAPI.getInstance().getSessionManager().startSession(sess);
        }

        for(ServerPlayer player : players) {
            if(!sess.addPlayer(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(player.getUUID()))) {
                LangModule.sendCommandSuccess(context, HideAndSeekAPI.getInstance().getLangProvider(), false,"command.error.cannot_add", player);
            }
        }

        return players.size();
    }

    private int leaveCommand(CommandContext<CommandSourceStack> context, List<ServerPlayer> players) {

        for(ServerPlayer player : players) {

            MPlayer pl = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(player.getUUID());

            AbstractSession sess = HideAndSeekAPI.getInstance().getSessionManager().getSession(pl);
            if(sess == null) {
                LangModule.sendCommandSuccess(context, HideAndSeekAPI.getInstance().getLangProvider(), false,"command.error.cannot_remove", player);
            } else {
                sess.removePlayer(pl);
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
                LangModule.sendCommandFailure(context, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.no_console");
                return 0;
            }

            MPlayer pl = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(sender.getUUID());
            AbstractSession sess = HideAndSeekAPI.getInstance().getSessionManager().getSession(pl);

            if (!(sess instanceof AbstractLobbySession)) {
                LangModule.sendCommandFailure(context, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.not_in_lobby");
                return 0;
            }

            UUID seeker = player == null ? null : player.getUUID();
            Map mp = null;

            if(map != null) {
                mp = HideAndSeekAPI.getInstance().getRegistry().getMap(map);
                if(mp == null) {
                    LangModule.sendCommandFailure(context, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.invalid_map");
                    return 0;
                }
            }

            ((AbstractLobbySession) sess).startGame(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(seeker), mp);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return 1;
    }

    private int reloadCommand(CommandContext<CommandSourceStack> context) {

        long time = HideAndSeek.getInstance().reload();
        LangModule.sendCommandSuccess(context, HideAndSeekAPI.getInstance().getLangProvider(), false, "command.reload.result", new CustomPlaceholderInline("time", time+""));

        return 1;
    }

    private int editCommand(CommandContext<CommandSourceStack> context, String map, List<ServerPlayer> players) {

        Entity ent = context.getSource().getEntity();

        Map mp = HideAndSeekAPI.getInstance().getRegistry().getMap(map);
        if(mp == null || (ent != null && !mp.canEdit(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(ent.getUUID())))) {

            LangModule.sendCommandFailure(context, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.invalid_map");
            return 0;
        }

        EditingSession session = null;

        for(AbstractSession sess : HideAndSeekAPI.getInstance().getSessionManager().getSessions()) {
            if(sess instanceof EditingSession && ((EditingSession) sess).getMap() == mp) {
                session = (EditingSession) sess;
            }
        }

        if(session == null) session = new EditingSession(mp);

        HideAndSeekAPI.getInstance().getSessionManager().startSession(session);

        for(ServerPlayer player : players) {
            if(!session.addPlayer(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(player.getUUID()))) {
                LangModule.sendCommandFailure(context, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.cannot_add", player);
            }
        }

        if(session.getPlayerCount() == 0) {
            session.shutdown();
        }

        return players.size();
    }

    private int customizeCommand(ServerPlayer target) {

        HideAndSeekRegistry reg = HideAndSeekAPI.getInstance().getRegistry();
        reg.openCustomizeGUI(FabricPlayer.wrap(target));

        return 1;

    }

}
