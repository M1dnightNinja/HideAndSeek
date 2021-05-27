package me.m1dnightninja.hideandseek.fabric.util;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.fabric.game.LobbySession;
import me.m1dnightninja.midnightcore.api.module.lang.PlaceholderSupplier;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
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
        mod.registerPlaceholderSupplier("hideandseek_hider_name", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.HIDER).getName()));
        mod.registerPlaceholderSupplier("hideandseek_hider_name_plural", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.HIDER).getPluralName()));
        mod.registerPlaceholderSupplier("hideandseek_hider_name_proper", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.HIDER).getProperName()));
        mod.registerPlaceholderSupplier("hideandseek_seeker_name", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.SEEKER).getName()));
        mod.registerPlaceholderSupplier("hideandseek_seeker_name_plural", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.SEEKER).getPluralName()));
        mod.registerPlaceholderSupplier("hideandseek_seeker_name_proper", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.SEEKER).getProperName()));
        mod.registerPlaceholderSupplier("hideandseek_main_hider_name", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.MAIN_HIDER).getName()));
        mod.registerPlaceholderSupplier("hideandseek_main_hider_name_plural", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.MAIN_HIDER).getPluralName()));
        mod.registerPlaceholderSupplier("hideandseek_main_hider_name_proper", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.MAIN_HIDER).getProperName()));
        mod.registerPlaceholderSupplier("hideandseek_main_seeker_name", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.MAIN_SEEKER).getName()));
        mod.registerPlaceholderSupplier("hideandseek_main_seeker_name_plural", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.MAIN_SEEKER).getPluralName()));
        mod.registerPlaceholderSupplier("hideandseek_main_seeker_name_proper", PlaceholderSupplier.create(AbstractMap.class, map -> map.getData(PositionType.MAIN_SEEKER).getProperName()));

        mod.registerInlinePlaceholderSupplier("hideandseek_position_type", PlaceholderSupplier.create(PositionData.class, obj -> obj.getType().getId()));
        mod.registerInlinePlaceholderSupplier("hideandseek_position_color", PlaceholderSupplier.create(PositionData.class, obj -> obj.getColor().toHex()));

        mod.registerPlaceholderSupplier("hideandseek_position_name", PlaceholderSupplier.create(PositionData.class, PositionData::getName));
        mod.registerPlaceholderSupplier("hideandseek_position_name_plural", PlaceholderSupplier.create(PositionData.class, PositionData::getPluralName));
        mod.registerPlaceholderSupplier("hideandseek_position_name_proper", PlaceholderSupplier.create(PositionData.class, PositionData::getProperName));

        mod.registerInlinePlaceholderSupplier("hideandseek_player_position_type", PlaceholderSupplier.create(MPlayer.class, obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj);
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj)).getType().getId();
        }));

        mod.registerInlinePlaceholderSupplier("hideandseek_player_position_color", PlaceholderSupplier.create(MPlayer.class, obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj);
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj)).getColor().toHex();
        }));

        mod.registerPlaceholderSupplier("hideandseek_player_position_name", PlaceholderSupplier.create(MPlayer.class, obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj);
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj)).getName();
        }));

        mod.registerPlaceholderSupplier("hideandseek_player_position_name_proper", PlaceholderSupplier.create(MPlayer.class, obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj);
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj)).getName();
        }));

        mod.registerPlaceholderSupplier("hideandseek_player_position_name_plural", PlaceholderSupplier.create(MPlayer.class, obj -> {
            LobbySession sess = (LobbySession) HideAndSeekAPI.getInstance().getSessionManager().getSession(obj);
            return sess.getRunningGame().getMap().getData(sess.getRunningGame().getPosition(obj)).getName();
        }));
    }

}
