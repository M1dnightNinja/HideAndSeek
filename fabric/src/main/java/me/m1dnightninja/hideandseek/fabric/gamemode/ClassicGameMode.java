package me.m1dnightninja.hideandseek.fabric.gamemode;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.api.game.AbstractMap;
import me.m1dnightninja.hideandseek.common.AbstractClassicGameMode;
import me.m1dnightninja.hideandseek.fabric.HideAndSeek;
import me.m1dnightninja.hideandseek.fabric.event.HideAndSeekRoleUpdatedEvent;
import me.m1dnightninja.hideandseek.fabric.game.MapInstance;
import me.m1dnightninja.hideandseek.fabric.mixin.AccessorMoveEntityPacket;
import me.m1dnightninja.hideandseek.fabric.mixin.AccessorPlayerSpawnPacket;
import me.m1dnightninja.hideandseek.fabric.util.FireworkUtil;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import me.m1dnightninja.midnightcore.fabric.api.event.PacketSendEvent;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.FireworkRocketItem;

import java.util.*;

public class ClassicGameMode extends AbstractClassicGameMode {

    private MapInstance currentMap;

    private final HashMap<MPlayer, List<MPlayer>> hidden = new HashMap<>();

    private final boolean useAntiCheat;

    public ClassicGameMode(AbstractLobbySession lobby, MPlayer seeker, AbstractMap map) {
        super(lobby, seeker, map);

        useAntiCheat = HideAndSeekAPI.getInstance().getMainSettings().isAntiCheatEnabled();

        Event.register(PacketSendEvent.class, this, event -> {
            if(state == ClassicGameState.UNINITIALIZED || (!useAntiCheat && state != ClassicGameState.HIDING)) return;

            MPlayer pl = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(event.getPlayer().getUUID());

            if(event.getPacket() instanceof ClientboundAddPlayerPacket) {


                UUID id = ((AccessorPlayerSpawnPacket) event.getPacket()).getPlayerId();
                MPlayer other = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(id);

                if(hidden.containsKey(pl) && hidden.get(pl).contains(other)) {
                    event.setCancelled(true);
                }
            }
            if(event.getPacket() instanceof ClientboundMoveEntityPacket) {

                int id = ((AccessorMoveEntityPacket) event.getPacket()).getEntityId();
                Entity ent = currentMap.getWorld().getEntity(id);
                if(ent == null) return;

                MPlayer other = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(ent.getUUID());

                if(ent instanceof ServerPlayer && hidden.containsKey(pl) && hidden.get(pl).contains(other)) {
                    event.setCancelled(true);
                }

            }
        });
    }

    @Override
    protected void startSeeking() {
        super.startSeeking();

        if(!useAntiCheat) {

            for(MPlayer u : hidden.keySet()) {

                ServerPlayer player = ((FabricPlayer) u).getMinecraftPlayer();
                if(player == null) continue;

                for(MPlayer uu : hidden.get(u)) {

                    ServerPlayer other = ((FabricPlayer) uu).getMinecraftPlayer();
                    if(other == null) continue;

                    player.connection.send(new ClientboundAddPlayerPacket(other));
                    player.connection.send(new ClientboundSetEntityDataPacket(other.getId(), other.getEntityData(), true));
                }
            }

            hidden.clear();
        }

        Event.unregisterAll(this);

    }

    @Override
    public void onTick() {
        if(state == ClassicGameState.UNINITIALIZED) return;

        for(Map.Entry<MPlayer, PositionType> ent : positions.entrySet()) {

            ServerPlayer player = ((FabricPlayer) ent.getKey()).getMinecraftPlayer();
            if(player == null) continue;

            Vec3d newLoc = new Vec3d(player.getX(), player.getY(), player.getZ());
            locations.putIfAbsent(ent.getKey(), newLoc);

            if(state == ClassicGameState.HIDING && positions.get(ent.getKey()).isSeeker()) {

                if(!newLoc.equals(locations.get(ent.getKey()))) toTeleport.add(ent.getKey());
            }

            for(Region reg : map.getRegions()) {
                if(reg.getDenied().contains(ent.getValue()) && reg.isInRegion(newLoc)) {

                    HideAndSeekAPI.getInstance().getLangProvider().sendMessage(getKey("deny_region_entry", ent.getKey(), ent.getValue()), ent.getKey(), player, reg);
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
                for(Map.Entry<MPlayer, PositionType> ent1 : positions.entrySet()) {
                    if(ent1.getValue().isSeeker()) continue;

                    ServerPlayer other = ((FabricPlayer) ent1.getKey()).getMinecraftPlayer();
                    if(other == null) continue;

                    if(hidden.get(ent.getKey()).contains(ent1.getKey()) && player.canSee(other)) {
                        hidden.get(ent.getKey()).remove(ent1.getKey());
                        player.connection.send(new ClientboundAddPlayerPacket(other));
                        player.connection.send(new ClientboundSetEntityDataPacket(other.getId(), other.getEntityData(), true));
                    }

                    if(!hidden.get(ent.getKey()).contains(ent1.getKey()) && !player.canSee(other)) {
                        hidden.get(ent.getKey()).add(ent1.getKey());
                        player.connection.send(new ClientboundRemoveEntitiesPacket(other.getId()));
                    }

                }
            }

        }

    }

    @Override
    protected void loadWorld(AbstractMap map, Runnable callback) {

        HideAndSeek.getInstance().getDimensionManager().loadMapWorld(map, lobby.getLobby().getId(), lobby.getLobby().getId(), (world) -> {
            if(world == null) {
                shutdown();
                return;
            }

            try {

                currentMap = new MapInstance(this, map, world, true);
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
        HideAndSeek.getInstance().getDimensionManager().unloadMapWorld(map, lobby.getLobby().getId(), false);
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
