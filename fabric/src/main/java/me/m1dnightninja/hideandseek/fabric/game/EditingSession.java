package me.m1dnightninja.hideandseek.fabric.game;

import me.m1dnightninja.hideandseek.api.game.AbstractMap;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.DamageSource;
import me.m1dnightninja.hideandseek.fabric.HideAndSeek;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import me.m1dnightninja.midnightcore.fabric.util.ConversionUtil;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

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
    protected boolean shouldAddPlayer(MPlayer u) {

        return instance != null || !initialized;
    }

    @Override
    protected void onPlayerAdded(MPlayer u) {

        ServerPlayer player = ((FabricPlayer) u).getMinecraftPlayer();
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
    protected void onPlayerRemoved(MPlayer u) {

        if(instance != null) {
            ServerPlayer player = ((FabricPlayer) u).getMinecraftPlayer();
            instance.onLeave(player);
        }
    }

    @Override
    protected void onShutdown() {

        HideAndSeek.getInstance().getDimensionManager().unloadMapWorld(map, map.getId() + "_editing", true);
    }

    @Override
    public void onTick() { }

    @Override
    public void onDamaged(MPlayer u, MPlayer damager, DamageSource damageSource, float amount) { }

}
