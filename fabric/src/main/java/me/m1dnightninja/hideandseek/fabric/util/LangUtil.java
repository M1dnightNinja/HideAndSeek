package me.m1dnightninja.hideandseek.fabric.util;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.Lobby;
import me.m1dnightninja.hideandseek.api.game.AbstractLobbySession;
import me.m1dnightninja.hideandseek.api.game.AbstractMap;
import me.m1dnightninja.hideandseek.api.game.PositionData;
import me.m1dnightninja.hideandseek.fabric.game.LobbySession;
import me.m1dnightninja.midnightcore.api.module.lang.PlaceholderSupplier;
import me.m1dnightninja.midnightcore.fabric.module.lang.LangModule;

public class LangUtil {

    public static void registerPlaceholders(LangModule mod) {

        mod.registerInlinePlaceholderSupplier("hideandseek_lobby_players", PlaceholderSupplier.create(AbstractLobbySession.class, sess -> sess.getPlayerCount() + ""));

        mod.registerInlinePlaceholderSupplier("hideandseek_lobby_id", PlaceholderSupplier.create(Lobby.class, Lobby::getId));
        mod.registerInlinePlaceholderSupplier("hideandseek_lobby_color", PlaceholderSupplier.create(Lobby.class, lby -> lby.getColor().toHex()));
        mod.registerInlinePlaceholderSupplier("hideandseek_lobby_color_legacy", PlaceholderSupplier.create(Lobby.class, lby -> "§" + Integer.toHexString(lby.getColor().toRGBI())));
        mod.registerInlinePlaceholderSupplier("hideandseek_lobby_min_players", PlaceholderSupplier.create(Lobby.class, lby -> lby.getMinPlayers() + ""));
        mod.registerInlinePlaceholderSupplier("hideandseek_lobby_max_players", PlaceholderSupplier.create(Lobby.class, lby -> lby.getMaxPlayers() + ""));
        mod.registerInlinePlaceholderSupplier("hideandseek_lobby_game_mode", PlaceholderSupplier.create(Lobby.class, lby -> lby.getGameType().getId()));

        mod.registerPlaceholderSupplier("hideandseek_lobby_name", PlaceholderSupplier.create(Lobby.class, Lobby::getName));
        mod.registerPlaceholderSupplier("hideandseek_lobby_game_mode_name", PlaceholderSupplier.create(Lobby.class, lby -> lby.getGameType().getName()));

        mod.registerInlinePlaceholderSupplier("hideandseek_map_id", PlaceholderSupplier.create(AbstractMap.class, AbstractMap::getId));
        mod.registerInlinePlaceholderSupplier("hideandseek_map_hide_time", PlaceholderSupplier.create(AbstractMap.class, obj -> obj.getHideTime() + ""));
        mod.registerInlinePlaceholderSupplier("hideandseek_map_seek_time", PlaceholderSupplier.create(AbstractMap.class, obj -> obj.getHideTime() + ""));

        mod.registerPlaceholderSupplier("hideandseek_map_name", PlaceholderSupplier.create(AbstractMap.class, AbstractMap::getName));

        mod.registerInlinePlaceholderSupplier("hideandseek_position_type", PlaceholderSupplier.create(PositionData.class, obj -> obj.getType().getId()));
        mod.registerInlinePlaceholderSupplier("hideandseek_position_color", PlaceholderSupplier.create(PositionData.class, obj -> obj.getColor().toHex()));

        mod.registerPlaceholderSupplier("hideandseek_position_name", PlaceholderSupplier.create(PositionData.class, PositionData::getName));
        mod.registerPlaceholderSupplier("hideandseek_position_name_plural", PlaceholderSupplier.create(PositionData.class, PositionData::getPluralName));
        mod.registerPlaceholderSupplier("hideandseek_position_name_proper", PlaceholderSupplier.create(PositionData.class, PositionData::getProperName));

        mod.registerInlinePlaceholderSupplier("hideandseek_player_position_type", LangModule.playerOrUUID(obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj.getUUID());
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj.getUUID())).getType().getId();
        }));

        mod.registerInlinePlaceholderSupplier("hideandseek_player_position_color", LangModule.playerOrUUID(obj ->  {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj.getUUID());
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj.getUUID())).getColor().toHex();
        }));

        mod.registerPlaceholderSupplier("hideandseek_player_position_name", LangModule.playerOrUUID(obj ->  {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj.getUUID());
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj.getUUID())).getName();
        }));

        mod.registerPlaceholderSupplier("hideandseek_player_position_name_proper", LangModule.playerOrUUID(obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj.getUUID());
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj.getUUID())).getProperName();
        }));

        mod.registerPlaceholderSupplier("hideandseek_player_position_name_plural", LangModule.playerOrUUID(obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj.getUUID());
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj.getUUID())).getPluralName();
        }));

    }

}
