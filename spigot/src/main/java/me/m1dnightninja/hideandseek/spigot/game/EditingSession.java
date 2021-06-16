package me.m1dnightninja.hideandseek.spigot.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.DamageSource;
import me.m1dnightninja.hideandseek.api.game.Map;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.spigot.player.SpigotPlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class EditingSession extends AbstractSession {

    private MapInstance instance;
    private final Map map;

    private final List<Player> waiting = new ArrayList<>();
    boolean initialized = false;

    public EditingSession(Map map) {
        this.map = map;

        HideAndSeekAPI.getInstance().getDimensionManager().loadMapWorld(map, map.getId() + "_editing", map.getId() + "_editing", world -> {
            if(world == null) return;
            instance = new MapInstance(this, map, (World) world, false);

            for(Player player : waiting) {
                player.teleport(instance.getHiderSpawn());
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

        Player player = ((SpigotPlayer) u).getSpigotPlayer();
        if(player == null) {
            removePlayer(u);
            return;
        }

        if(instance == null) {
            waiting.add(player);
        } else {
            player.teleport(instance.getHiderSpawn());
            instance.onJoin(player);
        }

        MidnightCoreAPI.getInstance().getModule(ISavePointModule.class).resetPlayer(u);
    }

    @Override
    protected void onPlayerRemoved(MPlayer u) {

        if(instance != null) {
            Player player = ((SpigotPlayer) u).getSpigotPlayer();
            instance.onLeave(player);
        }
    }

    @Override
    protected void onShutdown() {

        HideAndSeekAPI.getInstance().getDimensionManager().unloadMapWorld(map, map.getId() + "_editing", true);
    }

    @Override
    public void onTick() { }

    @Override
    public void onDamaged(MPlayer u, MPlayer damager, DamageSource src, float amount) { }

    public Map getMap() {
        return map;
    }
}
