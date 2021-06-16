package me.m1dnightninja.hideandseek.spigot.command;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.AbstractLobbySession;
import me.m1dnightninja.hideandseek.api.game.Lobby;
import me.m1dnightninja.hideandseek.api.game.Map;
import me.m1dnightninja.hideandseek.spigot.HideAndSeek;
import me.m1dnightninja.hideandseek.spigot.game.EditingSession;
import me.m1dnightninja.hideandseek.spigot.game.LobbySession;
import me.m1dnightninja.midnightcore.api.module.lang.CustomPlaceholderInline;
import me.m1dnightninja.midnightcore.api.module.lang.ILangProvider;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.spigot.module.lang.LangModule;
import me.m1dnightninja.midnightcore.spigot.player.SpigotPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {

    private static final String[] subcommands = { "join", "leave", "reload", "start", "edit", "customize" };

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        List<String> suggestions = new ArrayList<>();
        if(!(sender instanceof Player)) return suggestions;

        MPlayer player = SpigotPlayer.wrap((Player) sender);

        switch(args.length) {

            case 0:
            case 1:

                for(String s : subcommands) {
                    if(sender.hasPermission("hideandseek.command." + s)) suggestions.add(s);
                }
                break;
            case 2:

                if(!sender.hasPermission("hideandseek.command." + args[0])) break;

                switch(args[0]) {

                    case "join":

                        suggestions.addAll(HideAndSeekAPI.getInstance().getSessionManager().getLobbyNames(HideAndSeekAPI.getInstance().getSessionManager().getOpenLobbies(player)));
                        break;
                    case "start":

                        suggestions.addAll(HideAndSeekAPI.getInstance().getSessionManager().getLobbyMapNames(player));
                        break;
                    case "edit":

                        suggestions.addAll(HideAndSeekAPI.getInstance().getSessionManager().getEditableMapNames(player));
                        break;
                }

                break;
            case 3:

                switch(args[0]) {

                    case "join":

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (HideAndSeekAPI.getInstance().getSessionManager().getSession(SpigotPlayer.wrap(p)) != null) {
                                suggestions.add(p.getName());
                            }
                        }
                        break;
                    case "start":

                        AbstractSession sess = HideAndSeekAPI.getInstance().getSessionManager().getSession(player);
                        if (!(sess instanceof AbstractLobbySession)) break;

                        for (MPlayer pl : sess.getPlayers()) {
                            suggestions.add(pl.getName().allContent());
                        }

                        break;
                }
                break;
        }


        List<String> out = new ArrayList<>();
        for(String sug : suggestions) {
            if(sug.startsWith(args[args.length - 1])) {
                out.add(sug);
            }
        }

        return out;
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {

        if(args.length == 0 || !sender.hasPermission("hideandseek.command." + args[0])) {
            sendUsage(sender);
            return true;
        }

        ILangProvider provider = HideAndSeekAPI.getInstance().getLangProvider();
        MPlayer senderPlayer = sender instanceof Player ? SpigotPlayer.wrap((Player) sender) : null;

        switch(args[0]) {

            case "join":

                if(args.length == 1) {

                    if(senderPlayer == null) {
                        sendUsage(sender);
                        return true;
                    }

                    MPlayer player = SpigotPlayer.wrap((Player) sender);
                    HideAndSeekAPI.getInstance().getRegistry().openLobbyGUI(player, lobby -> lobby.canAccess(player), (lobby, click) -> onCommand(sender, command, label, new String[]{ "join", lobby.getId() }));
                    return true;

                }

                MPlayer player;
                if(args.length == 3) {

                    Player pl = Bukkit.getPlayer(args[2]);
                    if(pl == null) {
                        LangModule.sendMessage(sender, provider, "command.error.not_player");
                        return true;
                    }
                    player = SpigotPlayer.wrap(pl);

                } else {

                    if(senderPlayer == null) {
                        sendUsage(sender);
                        return true;
                    }

                    player = senderPlayer;
                }

                Lobby lobby = HideAndSeekAPI.getInstance().getRegistry().getLobby(args[1]);
                if(lobby == null || (senderPlayer != null && !lobby.canAccess(senderPlayer))) {
                    LangModule.sendMessage(sender, provider, "command.error.invalid_lobby");
                    return true;
                }

                AbstractLobbySession lobbySession = HideAndSeekAPI.getInstance().getSessionManager().getActiveSession(lobby);
                if(lobbySession == null) {

                    lobbySession = new LobbySession(lobby);
                    HideAndSeekAPI.getInstance().getSessionManager().startSession(lobbySession);
                }

                if(!lobbySession.addPlayer(player)) {
                    LangModule.sendMessage(sender, provider,"command.error.cannot_add", player);
                }
                break;

            case "leave":

                if(args.length == 2) {

                    Player pl = Bukkit.getPlayer(args[1]);
                    if(pl == null) {
                        LangModule.sendMessage(sender, provider, "command.error.not_player");
                        return true;
                    }
                    player = SpigotPlayer.wrap(pl);

                } else {
                    if(senderPlayer == null) {
                        sendUsage(sender);
                        return true;
                    }
                    player = senderPlayer;
                }

                AbstractSession session = HideAndSeekAPI.getInstance().getSessionManager().getSession(player);
                if(session == null) {
                    LangModule.sendMessage(sender, provider, "command.error.cannot_remove", player);
                    return true;
                }

                session.removePlayer(player);

                break;
            case "reload":

                long time = HideAndSeekAPI.getInstance().reload();
                LangModule.sendMessage(sender, provider,"hideandseek.command.reload", new CustomPlaceholderInline("time", time + ""));

                break;
            case "start":

                if(senderPlayer == null) {
                    sendUsage(sender);
                    return true;
                }

                session = HideAndSeekAPI.getInstance().getSessionManager().getSession(senderPlayer);
                if(!(session instanceof AbstractLobbySession)) {
                    LangModule.sendMessage(sender, provider, "hideandseek.error.not_in_lobby");
                    return true;
                }

                Map map = null;
                MPlayer seeker = null;
                if(args.length > 1) {
                    map = HideAndSeekAPI.getInstance().getRegistry().getMap(args[1]);
                    if(map == null) {
                        Player pl = Bukkit.getPlayer(args[1]);
                        if(pl != null) seeker = SpigotPlayer.wrap(pl);
                    }
                }
                if(args.length > 2) {
                    Player pl = Bukkit.getPlayer(args[2]);
                    if(pl != null) seeker = SpigotPlayer.wrap(pl);
                }

                lobbySession = (AbstractLobbySession) session;
                lobbySession.startGame(seeker, map);

                break;
            case "edit":

                if(args.length < 2) {
                    LangModule.sendMessage(sender, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.usage", new CustomPlaceholderInline("usage", "/has edit <map> <player>"));
                    return true;
                }

                if(args.length < 3) {
                    if(senderPlayer == null) {
                        sendUsage(sender);
                        return true;
                    }
                    player = senderPlayer;
                } else {

                    if(!sender.hasPermission("hideandseek.command.edit.others")) {
                        LangModule.sendMessage(sender, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.no_permission");
                        return true;
                    }

                    Player pl = Bukkit.getPlayer(args[2]);
                    if(pl == null) {
                        LangModule.sendMessage(sender, provider, "command.error.not_player");
                        return true;
                    }
                    player = SpigotPlayer.wrap(pl);
                }

                Map m = HideAndSeekAPI.getInstance().getRegistry().getMap(args[1]);
                if(m == null) {
                    LangModule.sendMessage(sender, provider, "command.error.invalid_map");
                }

                EditingSession editingSession = null;

                for(AbstractSession sess : HideAndSeekAPI.getInstance().getSessionManager().getSessions()) {
                    if(sess instanceof EditingSession && ((EditingSession) sess).getMap() == m) {
                        editingSession = (EditingSession) sess;
                    }
                }

                if(editingSession == null) editingSession = new EditingSession(m);
                HideAndSeekAPI.getInstance().getSessionManager().startSession(editingSession);

                if(!editingSession.addPlayer(player)) {
                    LangModule.sendMessage(sender, provider,"command.error.cannot_add", player);
                }

                if(editingSession.getPlayerCount() == 0) {
                    editingSession.shutdown();
                }

                break;
            case "customize":

                if(args.length < 2) {
                    if(senderPlayer == null) {
                        sendUsage(sender);
                        return true;
                    }
                    player = senderPlayer;

                } else {

                    if(!sender.hasPermission("hideandseek.command.customize.others")) {
                        LangModule.sendMessage(sender, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.no_permission");
                        return true;
                    }

                    Player pl = Bukkit.getPlayer(args[2]);
                    if(pl == null) {
                        LangModule.sendMessage(sender, provider, "command.error.not_player");
                        return true;
                    }
                    player = SpigotPlayer.wrap(pl);
                }

                HideAndSeekAPI.getInstance().getRegistry().openCustomizeGUI(player);

                break;
        }


        return true;
    }

    private void sendUsage(CommandSender sender) {

        StringBuilder usage = new StringBuilder("/has <");

        int count = 0;
        for (String s : subcommands) {
            if (sender.hasPermission("hideandseek.command." + s)) {
                if (count > 0) usage.append("/");
                usage.append(s);
                count++;
            }
        }
        usage.append(">");

        if(count == 0) {
            LangModule.sendMessage(sender, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.no_permission");
        }

        LangModule.sendMessage(sender, HideAndSeekAPI.getInstance().getLangProvider(), "command.error.usage", new CustomPlaceholderInline("usage", usage.toString()));


    }

}
