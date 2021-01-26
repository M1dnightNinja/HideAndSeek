package me.m1dnightninja.hideandseek.fabric;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import me.m1dnightninja.midnightcore.fabric.util.TextUtil;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.ChatType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class LobbySession extends AbstractLobbySession {

    private final AbstractLobby base;
    private Location tpLocation;

    public LobbySession(AbstractLobby lobby) {
        super(lobby);
        this.base = lobby;
    }

    @Override
    public void initialize() {

        if(base.getWorld() == null) {
            shutdown();
        }

        ResourceLocation world = new ResourceLocation(base.getWorld());

        ServerLevel dim = MidnightCore.getServer().getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, world));
        if(dim == null) {
            shutdown();
        }

        tpLocation = new Location(world, base.getLocation().getX(), base.getLocation().getY(), base.getLocation().getZ(), base.getRotation(), 0);

    }

    @Override
    protected boolean onJoined(UUID u) {
        boolean out = super.onJoined(u);
        if(!out) return false;

        ServerPlayer ent = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if(ent == null) return false;

        tpLocation.teleport(ent);
        MidnightCoreAPI.getInstance().getModule(ISavePointModule.class).resetPlayer(u);
        ent.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);

        return true;
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
        for(UUID u : players) {
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
        runningInstance.addListener(this::shutdown);
        runningInstance.start();
    }

}
