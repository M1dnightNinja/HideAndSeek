package me.m1dnightninja.hideandseek.fabric.game;

import me.m1dnightninja.hideandseek.api.game.AbstractMap;
import me.m1dnightninja.hideandseek.api.game.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.DamageSource;
import me.m1dnightninja.hideandseek.fabric.HideAndSeek;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.util.ConversionUtil;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditingSession extends AbstractSession {

    private MapInstance instance;
    private final AbstractMap map;

    private final List<ServerPlayer> waiting = new ArrayList<>();
    boolean initialized = false;

    public EditingSession(AbstractMap map) {
        this.map = map;

        HideAndSeek.getInstance().getDimensionManager().loadMapWorld(map, map.getId() + "_editing", "world_editing", world -> {
            if(world == null) return;
            instance = new MapInstance(this, map, world, false);

            for(ServerPlayer player : waiting) {
                instance.getHiderSpawn().teleport(player);
                instance.onJoin(player);
            }

            initialized = true;
        });
    }

    @Override
    protected boolean shouldAddPlayer(UUID u) {

        return instance != null || !initialized;
    }

    @Override
    protected void onPlayerAdded(UUID u) {

        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if(player == null) {
            removePlayer(u);
            return;
        }

        if(instance == null) {
            waiting.add(player);
        } else {
            instance.getHiderSpawn().teleport(player);
            instance.onJoin(player);
        }
        MidnightCoreAPI.getInstance().getModule(ISavePointModule.class).resetPlayer(u);
    }

    @Override
    protected void onPlayerRemoved(UUID u) {

        if(instance != null) {
            ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
            instance.onLeave(player);
        }
    }

    @Override
    protected void broadcastMessage(MComponent comp) {
        Component send = ConversionUtil.toMinecraftComponent(comp);

        for(UUID u : players) {
            ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if(pl == null) continue;

            pl.sendMessage(send, ChatType.SYSTEM, Util.NIL_UUID);
        }

    }

    @Override
    protected void onShutdown() {

        HideAndSeek.getInstance().getDimensionManager().unloadMapWorld(map, map.getId() + "_editing", true);
    }

    @Override
    public void onTick() { }

    @Override
    public void onDamaged(UUID u, UUID damager, DamageSource damageSource, float amount) { }

}
