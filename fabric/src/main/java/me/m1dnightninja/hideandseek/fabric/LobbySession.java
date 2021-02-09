package me.m1dnightninja.hideandseek.fabric;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ILangModule;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.CustomScoreboard;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import me.m1dnightninja.midnightcore.fabric.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.UUID;

public class LobbySession extends AbstractLobbySession {

    public static void registerPlaceholders(ILangModule<Component> mod) {

        mod.registerStringPlaceholder("hideandseek_lobby_players", mod.createSupplier(AbstractLobbySession.class, sess -> sess.getPlayerCount() + ""));
    }

    private final Location tpLocation;
    private final CustomScoreboard board;

    public LobbySession(AbstractLobby lobby) {
        super(lobby);

        if(lobby.getWorld() == null) {
            shutdown();
        }

        ResourceLocation world = new ResourceLocation(lobby.getWorld());

        ServerLevel dim = MidnightCore.getServer().getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, world));
        if(dim == null) {
            shutdown();
        }

        tpLocation = new Location(world, lobby.getLocation().getX(), lobby.getLocation().getY(), lobby.getLocation().getZ(), lobby.getRotation(), 0);

        board = new CustomScoreboard(RandomStringUtils.random(15,true,true), new TextComponent("HideAndSeek").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true)));

        board.setLine(5, new TextComponent("                         "));
        board.setLine(4, new TextComponent("Lobby: ").append(TextUtil.parse(lobby.getName())));
        board.setLine(3, new TextComponent("Game Mode: ").append(TextUtil.parse(lobby.getGameType().getName())));
        board.setLine(2, new TextComponent("                         "));
        board.setLine(1, new TextComponent("Players: ").append(new TextComponent(getPlayerCount() + " / " + lobby.getMaxPlayers()).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))));

    }

    @Override
    protected boolean shouldAddPlayer(UUID u) {
        return true;
    }

    @Override
    protected void onPlayerAdded(UUID u) {
        ServerPlayer ent = MidnightCore.getServer().getPlayerList().getPlayer(u);

        if(ent == null) {
            removePlayer(u);
            return;
        }

        tpLocation.teleport(ent);
        MidnightCoreAPI.getInstance().getModule(ISavePointModule.class).resetPlayer(u);
        ent.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);

        broadcastRawMessage(HideAndSeek.getInstance().getLangProvider().getMessageAsComponent("lobby.join", this, ent));

        board.setLine(1, new TextComponent("Players: ").append(new TextComponent(getPlayerCount() + " / " + lobby.getMaxPlayers()).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))));
        board.update();

        board.addPlayer(ent);
    }

    @Override
    protected void onPlayerRemoved(UUID u) {
        super.onPlayerRemoved(u);

        board.setLine(1, new TextComponent("Players: ").append(new TextComponent(getPlayerCount() + " / " + lobby.getMaxPlayers()).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))));
        board.update();

        ServerPlayer ent = MidnightCore.getServer().getPlayerList().getPlayer(u);

        if(ent != null) {
            board.removePlayer(ent);
            broadcastRawMessage(HideAndSeek.getInstance().getLangProvider().getMessageAsComponent("lobby.leave", this, ent));
        }
    }

    @Override
    public void onDamaged(UUID u, UUID damager, DamageSource damageSource, float amount) {

        if(isRunning()) {

            runningInstance.onDamaged(u, damager, damageSource, amount);

        } else {

            ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if (player == null) return;

            if (damageSource == DamageSource.VOID) {
                tpLocation.teleport(player);
            }
        }
    }

    @Override
    public void broadcastMessage(String message) {
        for(UUID u : getPlayerIds()) {
            ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if(pl == null) continue;
            pl.sendMessage(TextUtil.parse(message), ChatType.SYSTEM, Util.NIL_UUID);
        }
    }

    @Override
    public void doGameStart(UUID player, AbstractMap map) {
        GameType type = lobby.getGameType();

        if(type == null) {
            shutdown();
            return;
        }

        runningInstance = type.create(this, player, map);
        runningInstance.addCallback(this::shutdown);
        runningInstance.start();
    }

    public void broadcastRawMessage(Component cmp) {

        for(UUID u : getPlayerIds()) {
            ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(u);

            if(pl != null) pl.sendMessage(cmp, ChatType.SYSTEM, Util.NIL_UUID);
        }
    }


}
