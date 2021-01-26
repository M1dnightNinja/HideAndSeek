package me.m1dnightninja.hideandseek.fabric;

import me.m1dnightninja.hideandseek.api.AbstractMap;
import me.m1dnightninja.hideandseek.api.AbstractSession;
import me.m1dnightninja.hideandseek.api.DamageSource;
import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
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
        super(map.getId());
        this.map = map;
    }

    @Override
    public void initialize() {

        HideAndSeek.getInstance().getDimensionManager().loadMapWorld(map, map.getId() + "_editing", "world_editing",  world -> {
            if(world == null) return;
            instance = new MapInstance(map, world, false);

            for(ServerPlayer player : waiting) {
                instance.getHiderSpawn().teleport(player);
            }

            initialized = true;
        });

    }

    @Override
    protected boolean onJoined(UUID u) {

        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if(player == null) return false;

        if(instance == null) {
            if(initialized) return false;
            waiting.add(player);
        } else {
            instance.getHiderSpawn().teleport(player);
        }
        MidnightCoreAPI.getInstance().getModule(ISavePointModule.class).resetPlayer(u);
        return true;
    }

    @Override
    protected void onLeft(UUID u) { }

    @Override
    protected void onShutdown() {

        HideAndSeek.getInstance().getDimensionManager().unloadMapWorld(map, map.getId() + "_editing", true);
    }

    @Override
    public void onDamaged(UUID u, UUID damager, DamageSource damageSource, float amount) { }

    @Override
    public void broadcastMessage(String message) { }
}
