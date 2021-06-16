package me.m1dnightninja.hideandseek.spigot.gamemode;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.AbstractLobbySession;
import me.m1dnightninja.hideandseek.api.game.Map;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import me.m1dnightninja.hideandseek.api.game.Region;
import me.m1dnightninja.hideandseek.common.AbstractClassicGameMode;
import me.m1dnightninja.hideandseek.spigot.HideAndSeek;
import me.m1dnightninja.hideandseek.spigot.event.HideAndSeekRoleUpdatedEvent;
import me.m1dnightninja.hideandseek.spigot.game.MapInstance;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.spigot.player.SpigotPlayer;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClassicGameMode extends AbstractClassicGameMode {

    private final HashMap<MPlayer, Vec3d> locations = new HashMap<>();
    private final List<MPlayer> toTeleport = new ArrayList<>();

    private MapInstance currentMap;

    public ClassicGameMode(AbstractLobbySession lobby, MPlayer seeker, Map map) {
        super(lobby, seeker, map);
    }

    @Override
    public void onTick() {

        for(MPlayer pl : toTeleport) {
            pl.teleport(locations.get(pl), pl.getYaw(), pl.getPitch());
        }

        toTeleport.clear();

        for(HashMap.Entry<MPlayer, PositionType> ent : positions.entrySet()) {

            if(!locations.containsKey(ent.getKey())) {
                locations.put(ent.getKey(), ent.getKey().getLocation());
                continue;
            }

            Vec3d newLoc = ent.getKey().getLocation();
            Vec3d oldLoc = locations.get(ent.getKey());

            boolean allowed = true;
            if(state == ClassicGameState.HIDING && ent.getValue().isSeeker() && !oldLoc.equals(newLoc)) {
                allowed = false;
                toTeleport.add(ent.getKey());

            } else {
                for (Region r : map.getRegions()) {
                    if (r.getDenied().contains(ent.getValue()) && r.isInRegion(newLoc)) {
                        allowed = false;
                        toTeleport.add(ent.getKey());
                    }
                }
            }

            if(allowed) {
                locations.put(ent.getKey(), newLoc);
            }
        }

    }

    @Override
    protected void startSeeking() {
        super.startSeeking();

        for(HashMap.Entry<MPlayer, PositionType> ent : positions.entrySet()) {
            if(ent.getValue().isSeeker()) {
                Player pl = ((SpigotPlayer) ent.getKey()).getSpigotPlayer();
                if(pl != null) pl.removePotionEffect(PotionEffectType.BLINDNESS);
            }
        }
    }

    @Override
    protected void onTagged(MPlayer u, MPlayer tagger, PositionType newRole) {

        Player player = ((SpigotPlayer) u).getSpigotPlayer();
        if(player == null) return;

        currentMap.removeFromTeam(player);
        currentMap.addToTeam(player, PositionType.SEEKER);
    }

    @Override
    protected void loadWorld(Map map, Runnable callback) {

        HideAndSeekAPI.getInstance().getDimensionManager().loadMapWorld(map, lobby.getLobby().getId(), lobby.getLobby().getId(), (world) -> {
            if(world == null) {
                shutdown();
                return;
            }

            try {

                currentMap = new MapInstance(this, map, (World) world, true);
            } catch (Throwable th) {
                th.printStackTrace();
                shutdown();
            }

            callback.run();
        });

    }

    @Override
    protected void unloadWorld() {

        HideAndSeekAPI.getInstance().getDimensionManager().unloadMapWorld(map, lobby.getLobby().getId(), false);
    }

    @Override
    protected void playTickSound() {

        for(MPlayer player : players) {

            Player pl = ((SpigotPlayer) player).getSpigotPlayer();
            if(pl == null) return;

            pl.playSound(pl.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }

    @Override
    protected void playReleaseSound() {

        for(MPlayer player : players) {

            Player pl = ((SpigotPlayer) player).getSpigotPlayer();
            if (pl == null) return;

            pl.playSound(pl.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }

    @Override
    protected void setupPlayer(MPlayer u) {

        Player player = ((SpigotPlayer) u).getSpigotPlayer();
        if(player == null) return;

        if (u.equals(seeker)) {

            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 0, true, false, false));
            player.teleport(currentMap.getSeekerSpawn());

        } else {

            player.teleport(currentMap.getHiderSpawn());
        }

        currentMap.onJoin(player);
        currentMap.addToTeam(player, positions.get(u));

    }

    @Override
    protected void onPlayerRemoved(MPlayer u) {

        Player pl = ((SpigotPlayer) u).getSpigotPlayer();
        if(pl != null) {

            currentMap.onLeave(pl);
            currentMap.removeFromTeam(pl);
        }

        super.onPlayerRemoved(u);

    }

    @Override
    protected void broadcastVictorySound(PositionType winner) {

        for(MPlayer player : players) {

            Player pl = ((SpigotPlayer) player).getSpigotPlayer();
            if (pl == null) return;

            pl.playSound(pl.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE,1.0f, 1.3f);
        }

    }

    @Override
    protected boolean onPositionChanged(MPlayer u, PositionType oldType, PositionType newType) {

        HideAndSeekRoleUpdatedEvent event = new HideAndSeekRoleUpdatedEvent(((SpigotPlayer) u).getSpigotPlayer(), lobby, this, oldType, newType);
        Bukkit.getPluginManager().callEvent(event);

        if(event.isCancelled()) {

            return false;
        }

        return super.onPositionChanged(u, oldType, newType);
    }

    @Override
    protected void spawnFirework(Vec3d location, Color c1, boolean instant, boolean large, boolean flicker) {

        Location l = new Location(currentMap.getWorld(), location.getX(), location.getY(), location.getZ(), 0.0f, 0.0f);

        Firework fw = (Firework) currentMap.getWorld().spawnEntity(l, EntityType.FIREWORK);
        FireworkMeta meta = fw.getFireworkMeta();

        FireworkEffect effect = FireworkEffect.builder()
                .flicker(flicker)
                .withColor(org.bukkit.Color.fromRGB(c1.toDecimal()))
                .with(large ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.BALL)
                .build();

        meta.addEffect(effect);

        if(instant) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    fw.detonate();
                }
            }.runTaskLater(HideAndSeek.getInstance(), 1L);
        }
    }
}
