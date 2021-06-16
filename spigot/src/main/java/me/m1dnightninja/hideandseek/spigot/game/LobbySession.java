package me.m1dnightninja.hideandseek.spigot.game;

import me.m1dnightninja.hideandseek.api.game.AbstractLobbySession;
import me.m1dnightninja.hideandseek.api.game.DamageSource;
import me.m1dnightninja.hideandseek.api.game.Lobby;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.module.ISavePointModule;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.spigot.player.SpigotPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class LobbySession extends AbstractLobbySession {

    private final Location tpLocation;

    public LobbySession(Lobby lobby) {
        super(lobby);

        if(lobby.getWorld() == null) {
            shutdown();
        }

        World world = Bukkit.getWorld(lobby.getWorld());
        tpLocation = new Location(world, lobby.getLocation().getX(), lobby.getLocation().getY(), lobby.getLocation().getZ(), lobby.getRotation(), 0);
    }

    @Override
    protected boolean shouldAddPlayer(MPlayer u) {
        return true;
    }

    @Override
    protected void onPlayerAdded(MPlayer u) {
        super.onPlayerAdded(u);

        Player ent = ((SpigotPlayer) u).getSpigotPlayer();

        if(ent == null) {
            removePlayer(u);
            return;
        }

        ent.teleport(tpLocation);

        MidnightCoreAPI.getInstance().getModule(ISavePointModule.class).resetPlayer(u);
        ent.setGameMode(GameMode.ADVENTURE);
    }

    @Override
    public void onDamaged(MPlayer u, MPlayer damager, DamageSource damageSource, float amount) {

        if(isRunning()) {

            runningInstance.onDamaged(u, damager, damageSource, amount);

        } else {

            Player player = ((SpigotPlayer) u).getSpigotPlayer();
            if (player == null) return;

            if (damageSource == DamageSource.VOID) {
                player.teleport(tpLocation);
            }
        }
    }

    @Override
    public void broadcastMessage(MComponent message) {

        for (MPlayer u : getPlayers()) {

            u.sendMessage(message);
        }
    }
}
