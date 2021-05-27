package me.m1dnightninja.hideandseek.fabric.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.fabric.game.EditingSession;
import me.m1dnightninja.hideandseek.fabric.HideAndSeek;
import me.m1dnightninja.hideandseek.fabric.game.LobbySession;
import me.m1dnightninja.hideandseek.fabric.game.Map;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.inventory.AbstractInventoryGUI;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.module.skin.Skin;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.api.text.MStyle;
import me.m1dnightninja.midnightcore.fabric.api.PermissionHelper;
import me.m1dnightninja.midnightcore.fabric.inventory.InventoryGUI;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.TextComponent;
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
                .executes(context -> leaveCommand(context, null))
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
                .executes(context -> customizeCommand(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(context -> hasPermission(context, "hideandseek.command.customize.others"))
                    .executes(context -> customizeCommand(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
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
                context.getSource().sendFailure(new TextComponent("Only players can use that command!"));
                return 0;
            }
        }

        if(lobby == null) {

            try {
                final ServerPlayer player = sender;
                if (sender == null) return 0;

                MPlayer pl = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(player.getUUID());

                List<Lobby> lobbies = HideAndSeekAPI.getInstance().getSessionManager().getOpenLobbies(pl);

                if (lobbies.size() == 0) {
                    context.getSource().sendFailure(new TextComponent("There are no open lobbies!"));
                    return 0;
                }

                InventoryGUI gui = new InventoryGUI(MComponent.createTextComponent("Join a Lobby"));

                int index = 0;
                for (Lobby lby : lobbies) {

                    gui.setItem(lby.getDisplayStack(), index, (type, u) -> {
                        if (type == AbstractInventoryGUI.ClickType.LEFT) {
                            joinCommand(context, lby.getId(), Collections.singletonList(player));
                            player.closeContainer();
                        }
                    });

                    index++;
                }

                gui.open(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(player.getUUID()), 0);

            } catch(Exception ex) {
                ex.printStackTrace();
            }
            return 1;
        }

        Lobby lby = HideAndSeekAPI.getInstance().getRegistry().getLobby(lobby);
        if(lby == null) {
            context.getSource().sendFailure(new TextComponent("That is not a valid lobby!"));
            return 0;
        }

        if(sender != null && !lby.canAccess(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(sender.getUUID()))) {
            context.getSource().sendFailure(new TextComponent("That is not a valid lobby!"));
            return 0;
        }

        AbstractLobbySession sess = HideAndSeekAPI.getInstance().getSessionManager().getActiveSession(lby);
        if(sess == null) {

            sess = new LobbySession(lby);
            HideAndSeekAPI.getInstance().getSessionManager().startSession(sess);
        }

        for(ServerPlayer player : players) {
            if(!sess.addPlayer(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(player.getUUID()))) {
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

            MPlayer pl = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(player.getUUID());

            AbstractSession sess = HideAndSeekAPI.getInstance().getSessionManager().getSession(pl);
            if(sess == null) {
                context.getSource().sendFailure(new TextComponent("Unable to remove ").append(player.getDisplayName()).append(" from their session!"));
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
                context.getSource().sendFailure(new TextComponent("Only players can use that command!"));
                return 0;
            }

            MPlayer pl = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(sender.getUUID());
            AbstractSession sess = HideAndSeekAPI.getInstance().getSessionManager().getSession(pl);

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

            ((AbstractLobbySession) sess).startGame(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(seeker), mp);
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

        Entity ent = context.getSource().getEntity();

        AbstractMap mp = HideAndSeekAPI.getInstance().getRegistry().getMap(map);
        if(mp == null || (ent != null && !mp.canEdit(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(ent.getUUID())))) {

            context.getSource().sendFailure(new TextComponent("That is not a valid map!"));
            return 0;
        }

        EditingSession sess = new EditingSession(mp);
        HideAndSeekAPI.getInstance().getSessionManager().startSession(sess);

        for(ServerPlayer player : players) {
            if(!sess.addPlayer(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(player.getUUID()))) {
                context.getSource().sendFailure(new TextComponent("Unable to add ").append(player.getDisplayName()).append(" to the editing session!"));
            }
        }

        return players.size();
    }

    private int customizeCommand(CommandContext<CommandSourceStack> context, ServerPlayer target) {

        try {
            InventoryGUI gui = new InventoryGUI(MComponent.createTextComponent("Customize"));
            InventoryGUI mapsGui = new InventoryGUI(MComponent.createTextComponent("Choose a Map"));

            int current = 0;
            for (AbstractMap m : HideAndSeekAPI.getInstance().getRegistry().getMaps()) {
                for (PositionType tp : PositionType.values()) {
                    if (m.getData(tp).getPlayerClasses(FabricPlayer.wrap(target)).size() > 1) {

                        mapsGui.setItem(Map.getDisplayStack(m), current, (type, user) -> {

                            InventoryGUI pos = new InventoryGUI(MComponent.createTextComponent("Select a Role"));

                            int i = 0;
                            for (PositionType pt : PositionType.values()) {
                                if (m.getData(pt).getClasses().size() > 1) {

                                    MItemStack is = MItemStack.Builder.woolWithColor(m.getData(pt).getColor()).withName(MComponent.createTextComponent("").withStyle(MStyle.ITEM_BASE).addChild(m.getData(pt).getName())).build();
                                    pos.setItem(is, i, (type1, user1) -> openMapClassGui(user1, m, pt));

                                    i++;
                                }
                            }

                            pos.open(user, 0);

                        });

                        current++;
                        break;
                    }
                }
            }

            if (current == 0) {
                context.getSource().sendFailure(new TextComponent("There is nothing to customize!"));
            }

            MItemStack is = MItemStack.Builder.of(MIdentifier.create("minecraft", "diamond")).withName(MComponent.createTextComponent("Change Preferred Classes").withStyle(MStyle.ITEM_BASE.withColor(Color.fromRGBI(11)))).build();
            gui.setItem(is, 4, (type, user) -> {
                if (type == AbstractInventoryGUI.ClickType.LEFT) {
                    mapsGui.open(user, 0);
                }
            });

            gui.open(MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(target.getUUID()), 0);

        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return 0;

    }

    private void openMapClassGui(MPlayer user, AbstractMap map, PositionType type) {

        InventoryGUI gui = new InventoryGUI(MComponent.createTextComponent("Select a Class"));

        int i = 0;
        for(AbstractClass clazz : map.getData(type).getPlayerClasses(user)) {

            Skin s = null;
            if(clazz.getSkins().size() > 0) {
                s = HideAndSeekAPI.getInstance().getRegistry().getSkin(clazz.getSkins().get(0)).getSkin();
            }

            MItemStack is = MItemStack.Builder.headWithSkin(s).withName(MComponent.createTextComponent("").withStyle(MStyle.ITEM_BASE).addChild(clazz.getName())).build();
            gui.setItem(is, i, (ctype, user1) -> HideAndSeekAPI.getInstance().getRegistry().setPreferredClass(user1.getUUID(), map, type, clazz));
            i++;

        }

        gui.open(user, 0);

    }

}
