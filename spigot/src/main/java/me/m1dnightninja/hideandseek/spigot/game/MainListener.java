package me.m1dnightninja.hideandseek.spigot.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.core.AbstractSession;
import me.m1dnightninja.hideandseek.api.game.DamageSource;
import me.m1dnightninja.hideandseek.spigot.util.ConversionUtil;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.spigot.player.SpigotPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MainListener implements Listener {

    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {

        MPlayer player = SpigotPlayer.wrap(event.getPlayer());

        AbstractSession sess = HideAndSeekAPI.getInstance().getSessionManager().getSession(player);
        if(sess != null) sess.removePlayer(player);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {

        if(!(event.getEntity() instanceof Player)) return;

        MPlayer pl = SpigotPlayer.wrap((Player) event.getEntity());
        DamageSource source = ConversionUtil.toDamageSource(event.getCause());
        event.setCancelled(HideAndSeekAPI.getInstance().getSessionManager().onDamaged(pl, null, source, (float) event.getDamage()));
    }

    @EventHandler
    public void onAttacked(EntityDamageByEntityEvent event) {


        if(!(event.getEntity() instanceof Player)) return;

        MPlayer pl = SpigotPlayer.wrap((Player) event.getEntity());
        MPlayer damager = event.getDamager() instanceof Player ? SpigotPlayer.wrap((Player) event.getDamager()) : null;

        DamageSource source = ConversionUtil.toDamageSource(event.getCause());
        event.setCancelled(HideAndSeekAPI.getInstance().getSessionManager().onDamaged(pl, damager, source, (float) event.getDamage()));

    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {

        if(HideAndSeekAPI.getInstance().getSessionManager().getSession(SpigotPlayer.wrap((Player) event.getEntity())) != null) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        HideAndSeekAPI.getInstance().getRegistry().loadData(event.getPlayer().getUniqueId());
    }

}
