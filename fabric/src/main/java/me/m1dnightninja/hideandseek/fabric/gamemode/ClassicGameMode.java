package me.m1dnightninja.hideandseek.fabric.gamemode;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.api.game.Map;
import me.m1dnightninja.hideandseek.common.AbstractClassicGameMode;
import me.m1dnightninja.hideandseek.fabric.event.HideAndSeekRoleUpdatedEvent;
import me.m1dnightninja.hideandseek.fabric.game.MapInstance;
import me.m1dnightninja.hideandseek.fabric.util.FireworkUtil;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.FireworkRocketItem;

import java.util.*;

public class ClassicGameMode extends AbstractClassicGameMode {

    private MapInstance currentMap;

    private final HashMap<MPlayer, List<MPlayer>> hidden = new HashMap<>();

    private final boolean useAntiCheat;

    public ClassicGameMode(AbstractLobbySession lobby, MPlayer seeker, Map map) {
        super(lobby, seeker, map);

        useAntiCheat = HideAndSeekAPI.getInstance().getMainSettings().isAntiCheatEnabled();
    }

    @Override
    public void onTick() {
        if(state == ClassicGameState.UNINITIALIZED) return;

        for(java.util.Map.Entry<MPlayer, PositionType> ent : positions.entrySet()) {

            ServerPlayer player = ((FabricPlayer) ent.getKey()).getMinecraftPlayer();
            if(player == null) continue;

            Vec3d newLoc = new Vec3d(player.getX(), player.getY(), player.getZ());
            locations.putIfAbsent(ent.getKey(), newLoc);

            if(state == ClassicGameState.HIDING && positions.get(ent.getKey()).isSeeker()) {

                if(!newLoc.equals(locations.get(ent.getKey()))) toTeleport.add(ent.getKey());
            }

            for(Region reg : map.getRegions()) {
                if(reg.getDenied().contains(ent.getValue()) && reg.isInRegion(newLoc)) {

                    HideAndSeekAPI.getInstance().getLangProvider().sendMessage(getKey("deny_region_entry", ent.getKey(), ent.getValue()), ent.getKey(), map.getData(ent.getValue()), reg);
                    toTeleport.add(ent.getKey());
                }
            }

            if(toTeleport.contains(ent.getKey())) {
                player.teleportTo(locations.get(ent.getKey()).getX(), locations.get(ent.getKey()).getY(), locations.get(ent.getKey()).getZ());
                toTeleport.remove(ent.getKey());
            } else {
                locations.put(ent.getKey(), newLoc);
            }

            if(useAntiCheat && state != ClassicGameState.HIDING && positions.get(ent.getKey()).isSeeker()) {
                for(java.util.Map.Entry<MPlayer, PositionType> ent1 : positions.entrySet()) {
                    if(ent1.getValue().isSeeker()) continue;

                    ServerPlayer other = ((FabricPlayer) ent1.getKey()).getMinecraftPlayer();
                    if(other == null) continue;

                    if(hidden.get(ent.getKey()).contains(ent1.getKey()) && player.hasLineOfSight(other)) {
                        hidden.get(ent.getKey()).remove(ent1.getKey());
                        vanishModule.showPlayerFor(ent1.getKey(), ent.getKey());
                    }

                    if(!hidden.get(ent.getKey()).contains(ent1.getKey()) && !player.hasLineOfSight(other)) {
                        hidden.get(ent.getKey()).add(ent1.getKey());
                        vanishModule.hidePlayerFor(ent1.getKey(), ent.getKey());
                    }

                }
            }

        }

    }

    @Override
    protected void loadWorld(Map map, Runnable callback) {

        HideAndSeekAPI.getInstance().getDimensionManager().loadMapWorld(map, lobby.getLobby().getId(), lobby.getLobby().getId(), (world) -> {
            if(world == null) {
                shutdown();
                return;
            }

            try {

                currentMap = new MapInstance(this, map, (ServerLevel) world, true);
            } catch (Throwable th) {
                th.printStackTrace();
                shutdown();
            }

            callback.run();
        });

    }

    @Override
    protected void unloadWorld() {
        currentMap.clearTeams();
        HideAndSeekAPI.getInstance().getDimensionManager().unloadMapWorld(map, lobby.getLobby().getId(), false);
    }

    @Override
    protected void playTickSound() {
        for(MPlayer u : getPlayers()) {
            ServerPlayer pl = ((FabricPlayer) u).getMinecraftPlayer();
            if(pl == null) continue;
            pl.playNotifySound(SoundEvents.NOTE_BLOCK_PLING, SoundSource.HOSTILE, 1.0f, 1.0f);
        }
    }

    @Override
    protected void playReleaseSound() {
        for(MPlayer u : getPlayers()) {
            ServerPlayer pl = ((FabricPlayer) u).getMinecraftPlayer();
            if(pl != null) pl.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0f, 1.0f);
        }
    }

    @Override
    protected void setupPlayer(MPlayer u) {

        ServerPlayer player = ((FabricPlayer) u).getMinecraftPlayer();
        if(player == null) return;

        if (u.equals(seeker)) {

            List<MPlayer> pls = new ArrayList<>();
            for(MPlayer uu : getPlayers()) {
                if(uu.equals(u)) continue;
                pls.add(uu);
            }
            hidden.put(u, pls);
            currentMap.getSeekerSpawn().teleport(player);

        } else {

            currentMap.getHiderSpawn().teleport(player);
            hidden.put(u, new ArrayList<>());
        }

        currentMap.onJoin(player);
        currentMap.addToTeam(player.getGameProfile().getName(), positions.get(u));
    }

    @Override
    protected void onPlayerRemoved(MPlayer u) {

        ServerPlayer pl = ((FabricPlayer) u).getMinecraftPlayer();
        if(pl != null) {

            currentMap.onLeave(pl);
            currentMap.removeFromTeam(pl.getGameProfile().getName());
        }

        super.onPlayerRemoved(u);

    }

    @Override
    protected void spawnFirework(Vec3d location, Color c1, boolean instant, boolean large, boolean flicker) {

        if(instant) {
            FireworkUtil.spawnFireworkExplosion(
                    Collections.singletonList(c1),
                    Collections.singletonList(new Color(0xFFFFFF)),
                    large ? FireworkRocketItem.Shape.LARGE_BALL : FireworkRocketItem.Shape.SMALL_BALL,
                    new Location(currentMap.getWorld().dimension().location(), location.getX(), location.getY(), location.getZ(), 0.0f, 0.0f));
        } else {
            FireworkUtil.spawnFireworkEntity(
                    Collections.singletonList(c1),
                    Collections.singletonList(new Color(0xFFFFFF)),
                    large ? FireworkRocketItem.Shape.LARGE_BALL : FireworkRocketItem.Shape.SMALL_BALL,
                    new Location(currentMap.getWorld().dimension().location(), location.getX(), location.getY(), location.getZ(), 0.0f, 0.0f));
        }
    }

    @Override
    protected void broadcastVictorySound(PositionType winner) {

        for(MPlayer u : lobby.getPlayers()) {

            ServerPlayer player = ((FabricPlayer) u).getMinecraftPlayer();
            if(player == null) continue;

            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 1.0f, 1.3f);
        }

    }

    @Override
    protected boolean onPositionChanged(MPlayer u, PositionType oldType, PositionType newType) {

        HideAndSeekRoleUpdatedEvent event = new HideAndSeekRoleUpdatedEvent((FabricPlayer) u, lobby, this, oldType, newType);
        Event.invoke(event);

        if(event.isCancelled()) {

            return false;
        }

        return super.onPositionChanged(u, oldType, newType);
    }

    @Override
    protected void onTagged(MPlayer u, MPlayer tagger, PositionType newRole) {

        ServerPlayer player = ((FabricPlayer) u).getMinecraftPlayer();
        if(player == null) return;

        currentMap.removeFromTeam(player.getGameProfile().getName());
        currentMap.addToTeam(player.getGameProfile().getName(), PositionType.SEEKER);

    }
}
