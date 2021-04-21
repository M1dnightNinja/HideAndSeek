package me.m1dnightninja.hideandseek.fabric.gamemode;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.hideandseek.api.game.AbstractMap;
import me.m1dnightninja.hideandseek.common.AbstractClassicGameMode;
import me.m1dnightninja.hideandseek.fabric.HideAndSeek;
import me.m1dnightninja.hideandseek.fabric.game.MapInstance;
import me.m1dnightninja.hideandseek.fabric.mixin.AccessorMoveEntityPacket;
import me.m1dnightninja.hideandseek.fabric.mixin.AccessorPlayerSpawnPacket;
import me.m1dnightninja.hideandseek.fabric.util.ConversionUtil;
import me.m1dnightninja.hideandseek.fabric.util.FireworkUtil;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.api.text.MStyle;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import me.m1dnightninja.midnightcore.fabric.api.event.PacketSendEvent;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.FireworkRocketItem;

import java.util.*;

public class ClassicGameMode extends AbstractClassicGameMode {

    private MapInstance currentMap;

    private final HashMap<UUID, List<UUID>> hidden = new HashMap<>();

    private final boolean useAntiCheat;

    public ClassicGameMode(AbstractLobbySession lobby, UUID seeker, AbstractMap map) {
        super(lobby, seeker, map);

        useAntiCheat = HideAndSeekAPI.getInstance().getMainSettings().isAntiCheatEnabled();

        Event.register(PacketSendEvent.class, this, event -> {
            if(state == ClassicGameState.UNINITIALIZED || (!useAntiCheat && state != ClassicGameState.HIDING)) return;
            if(event.getPacket() instanceof ClientboundAddPlayerPacket) {

                UUID id = ((AccessorPlayerSpawnPacket) event.getPacket()).getPlayerId();
                if(hidden.containsKey(event.getPlayer().getUUID()) && hidden.get(event.getPlayer().getUUID()).contains(id)) {
                    event.setCancelled(true);
                }
            }
            if(event.getPacket() instanceof ClientboundMoveEntityPacket) {

                int id = ((AccessorMoveEntityPacket) event.getPacket()).getEntityId();
                Entity ent = currentMap.getWorld().getEntity(id);

                if(ent instanceof ServerPlayer && hidden.containsKey(event.getPlayer().getUUID()) && hidden.get(event.getPlayer().getUUID()).contains(ent.getUUID())) {
                    event.setCancelled(true);
                }

            }
        });
    }

    @Override
    protected void startSeeking() {
        super.startSeeking();

        if(!useAntiCheat) {

            for(UUID u : hidden.keySet()) {

                ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
                if(player == null) continue;

                for(UUID uu : hidden.get(u)) {

                    ServerPlayer other = MidnightCore.getServer().getPlayerList().getPlayer(uu);
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

        for(Map.Entry<UUID, PositionType> ent : positions.entrySet()) {

            ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(ent.getKey());
            if(player == null) continue;

            Vec3d newLoc = new Vec3d(player.getX(), player.getY(), player.getZ());
            locations.putIfAbsent(ent.getKey(), newLoc);

            if(state == ClassicGameState.HIDING && positions.get(ent.getKey()).isSeeker()) {

                if(!newLoc.equals(locations.get(ent.getKey()))) toTeleport.add(ent.getKey());
            }

            for(Region reg : map.getRegions()) {
                if(reg.getDenied().contains(ent.getValue()) && reg.isInRegion(newLoc)) {

                    HideAndSeek.getInstance().getLangProvider().sendMessage(getKey("deny_region_entry", player.getUUID(), ent.getValue()), player.getUUID(), player, reg);
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
                for(Map.Entry<UUID, PositionType> ent1 : positions.entrySet()) {
                    if(ent1.getValue().isSeeker()) continue;

                    ServerPlayer other = MidnightCore.getServer().getPlayerList().getPlayer(ent1.getKey());
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
        for(UUID u : getPlayerIds()) {
            ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if(pl != null) pl.playNotifySound(SoundEvents.NOTE_BLOCK_PLING, SoundSource.HOSTILE, 1.0f, 1.0f);
        }
    }

    @Override
    protected void playReleaseSound() {
        for(UUID u : getPlayerIds()) {
            ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if(pl != null) pl.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0f, 1.0f);
        }
    }

    @Override
    protected void broadcastTagMessage(UUID tagged, UUID tagger, DamageSource src) {

        ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(tagged);
        if(pl == null) return;

        MComponent bc;

        if(tagger != null) {

            ServerPlayer attacker = MidnightCore.getServer().getPlayerList().getPlayer(tagger);
            if(attacker == null) return;

            bc = MComponent.createTextComponent("")
                    .addChild(MComponent.createTextComponent(pl.getName().getContents()).withStyle(new MStyle().withColor(map.getData(PositionType.HIDER).getColor())))
                    .addChild(MComponent.createTextComponent(" was tagged by "))
                    .addChild(MComponent.createTextComponent(attacker.getName().getContents()).withStyle(new MStyle().withColor(map.getData(PositionType.SEEKER).getColor())))
                    .addChild(MComponent.createTextComponent("! "))
                    .addChild(getRemainsText());

        } else if(src != null) {

            net.minecraft.world.damagesource.DamageSource minecraft = ConversionUtil.convertDamageSource(src);

            bc = MComponent.createTextComponent("")
                    .addChild(MComponent.createTextComponent(pl.getName().getContents()).withStyle(new MStyle().withColor(map.getData(PositionType.HIDER).getColor())))
                    .addChild(MComponent.createTranslatableComponent("death.attack." + minecraft.msgId))
                    .addChild(MComponent.createTextComponent("! "))
                    .addChild(getRemainsText());

        } else {

            bc = MComponent.createTextComponent("")
                    .addChild(MComponent.createTextComponent(pl.getName().getContents()).withStyle(new MStyle().withColor(map.getData(PositionType.HIDER).getColor())))
                    .addChild(MComponent.createTextComponent(" was tagged! "))
                    .addChild(getRemainsText());

        }

        broadcastMessage(bc);

    }

    private MComponent getRemainsText() {
        int hiders = 0;
        for(PositionType t : positions.values()) {
            if(t == PositionType.HIDER) hiders++;
        }

        MComponent remain = MComponent.createTextComponent(hiders + "").withStyle(new MStyle().withColor(map.getData(PositionType.HIDER).getColor()));
        remain.addChild(MComponent.createTextComponent(" "));
        if(hiders == 1) {
            remain.addChild(map.getData(PositionType.HIDER).getName());
            remain.addChild(MComponent.createTextComponent(" remains!"));
        } else {
            remain.addChild(map.getData(PositionType.HIDER).getPluralName());
            remain.addChild(MComponent.createTextComponent(" remain!"));
        }

        return remain;
    }

    @Override
    protected void setupPlayer(UUID u) {

        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if (player == null) return;

        if (u.equals(seeker)) {
            setPosition(u, PositionType.MAIN_SEEKER);

            List<UUID> pls = new ArrayList<>();
            for(UUID uu : getPlayerIds()) {
                if(uu.equals(u)) continue;
                pls.add(uu);
            }
            hidden.put(u, pls);
            currentMap.getSeekerSpawn().teleport(player);

        } else {
            setPosition(u, PositionType.HIDER);

            currentMap.getHiderSpawn().teleport(player);
            hidden.put(u, new ArrayList<>());
        }
        AbstractClass clazz = HideAndSeekAPI.getInstance().getRegistry().chooseClass(u, map, positions.get(u));

        if (clazz != null) {
            clazz.applyToPlayer(u, null);
            classes.put(u, clazz);
        } else {
            HideAndSeekAPI.getLogger().warn("clazz is null!");
        }

        currentMap.onJoin(player);
        currentMap.addToTeam(player.getGameProfile().getName(), positions.get(u));
    }

    @Override
    protected void onPlayerRemoved(UUID u) {

        ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if(pl != null) {

            currentMap.onLeave(pl);
            currentMap.removeFromTeam(pl.getGameProfile().getName());
        }

        super.onPlayerRemoved(u);

    }

    @Override
    protected void broadcastMessage(MComponent comp) {

        Component cmp = me.m1dnightninja.midnightcore.fabric.util.ConversionUtil.toMinecraftComponent(comp);

        for(UUID u : lobby.getPlayerIds()) {

            ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if(player == null) continue;

            player.sendMessage(cmp, ChatType.SYSTEM, Util.NIL_UUID);
        }

    }

    @Override
    protected void broadcastVictorySound(PositionType winner) {

        for(UUID u : lobby.getPlayerIds()) {

            ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if(player == null) continue;

            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 1.0f, 1.3f);
        }

    }

    @Override
    protected void setPlayerSeeker(UUID u) {

        super.setPlayerSeeker(u);

        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if(player == null) return;

        AbstractClass clazz = null;
        if(classes.containsKey(player.getUUID())) {
            clazz = classes.get(player.getUUID()).getEquivalent(PositionType.SEEKER);
        }

        if(clazz == null) clazz = HideAndSeekAPI.getInstance().getRegistry().chooseClass(player.getUUID(), map, PositionType.SEEKER);

        if(clazz != null) {
            clazz.applyToPlayer(player.getUUID(), null);
            classes.put(player.getUUID(), clazz);
        }

        Location loc = Location.getEntityLocation(player);

        currentMap.removeFromTeam(player.getGameProfile().getName());
        currentMap.addToTeam(player.getGameProfile().getName(), PositionType.SEEKER);

        FireworkUtil.spawnFireworkExplosion(Collections.singletonList(map.getData(PositionType.SEEKER).getColor()), Collections.singletonList(new Color("FFFFFF")), FireworkRocketItem.Shape.LARGE_BALL, loc);
        FireworkUtil.spawnFireworkExplosion(Collections.singletonList(new Color("FFFFFF")), Collections.singletonList(new Color("FFFFFF")), FireworkRocketItem.Shape.SMALL_BALL, loc);

    }
}
