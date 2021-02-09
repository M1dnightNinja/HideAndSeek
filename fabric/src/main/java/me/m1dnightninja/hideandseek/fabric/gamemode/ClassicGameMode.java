package me.m1dnightninja.hideandseek.fabric.gamemode;

import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.api.AbstractMap;
import me.m1dnightninja.hideandseek.common.AbstractClassicGameMode;
import me.m1dnightninja.hideandseek.fabric.HideAndSeek;
import me.m1dnightninja.hideandseek.fabric.MapInstance;
import me.m1dnightninja.hideandseek.fabric.PositionData;
import me.m1dnightninja.hideandseek.fabric.mixin.AccessorMoveEntityPacket;
import me.m1dnightninja.hideandseek.fabric.mixin.AccessorPlayerSpawnPacket;
import me.m1dnightninja.hideandseek.fabric.util.ConversionUtil;
import me.m1dnightninja.hideandseek.fabric.util.FireworkUtil;
import me.m1dnightninja.midnightcore.api.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.CustomScoreboard;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import me.m1dnightninja.midnightcore.fabric.api.event.PacketSendEvent;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import me.m1dnightninja.midnightcore.fabric.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.FireworkRocketItem;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.*;

public class ClassicGameMode extends AbstractClassicGameMode {

    private MapInstance currentMap;

    private final HashMap<UUID, CustomScoreboard> scoreboards = new HashMap<>();

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

        for(CustomScoreboard sb : scoreboards.values()) {

            sb.setLine(6, new TextComponent("Phase: ").append(new TextComponent("Seeking").setStyle(Style.EMPTY.withColor(ChatFormatting.RED))));
            sb.update();

        }

    }

    @Override
    protected void onTick() {
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

                    player.sendMessage(HideAndSeek.getInstance().getLangProvider().getMessageAsComponent("deny_region_entry", player, reg),  ChatType.SYSTEM, Util.NIL_UUID);

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
        for(CustomScoreboard cs : scoreboards.values()) {
            cs.clearPlayers();
        }
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

        MutableComponent bc;

        if(tagger != null) {

            ServerPlayer attacker = MidnightCore.getServer().getPlayerList().getPlayer(tagger);
            if(attacker == null) return;

            bc = new TextComponent("")
                    .append(pl.getName().plainCopy().withStyle(Style.EMPTY.withColor(TextColor.parseColor(map.getData(PositionType.HIDER).getColor().toHex()))))
                    .append(" was tagged by ")
                    .append(attacker.getName().plainCopy().withStyle(Style.EMPTY.withColor(TextColor.parseColor(map.getData(PositionType.SEEKER).getColor().toHex()))))
                    .append("! ")
                    .append(getRemainsText());

        } else if(src != null) {

            net.minecraft.world.damagesource.DamageSource minecraft = ConversionUtil.convertDamageSource(src);

            bc = new TextComponent("")
                    .append(pl.getName().plainCopy().withStyle(Style.EMPTY.withColor(TextColor.parseColor(map.getData(PositionType.HIDER).getColor().toHex()))))
                    .append(new TranslatableComponent("death.attack." + minecraft.msgId))
                    .append("! ")
                    .append(getRemainsText());

        } else {

            bc = new TextComponent("")
                    .append(pl.getName().plainCopy().withStyle(Style.EMPTY.withColor(TextColor.parseColor(map.getData(PositionType.HIDER).getColor().toHex()))))
                    .append(" was tagged! ")
                    .append(getRemainsText());

        }

        for(UUID u : getPlayerIds()) {

            ServerPlayer op = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if(op != null) op.sendMessage(bc, ChatType.SYSTEM, Util.NIL_UUID);

        }

    }

    private Component getRemainsText() {
        int hiders = 0;
        for(PositionType t : positions.values()) {
            if(t == PositionType.HIDER) hiders++;
        }

        MutableComponent remain = new TextComponent(hiders + "").withStyle(Style.EMPTY.withColor(TextColor.parseColor(map.getData(PositionType.HIDER).getColor().toHex())));
        remain.append(" ");
        if(hiders == 1) {
            remain.append(TextUtil.parse(map.getData(PositionType.HIDER).getName()));
            remain.append(" remains!");
        } else {
            remain.append(TextUtil.parse(map.getData(PositionType.HIDER).getPluralName()));
            remain.append(" remain!");
        }

        return remain;
    }

    @Override
    protected void endGame(PositionType winner) {
        super.endGame(winner);

        for(CustomScoreboard sb : scoreboards.values()) {
            sb.setLine(6, new TextComponent("Phase: ").append(new TextComponent("Ended").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))));
            sb.update();
        }
    }

    @Override
    protected void setupPlayer(UUID u) {

        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if (player == null) return;

        Component subtitle;
        if (u.equals(seeker)) {
            setPosition(u, PositionType.MAIN_SEEKER);

            List<UUID> pls = new ArrayList<>();
            for(UUID uu : getPlayerIds()) {
                if(uu.equals(u)) continue;
                pls.add(uu);
            }
            hidden.put(u, pls);

            //subtitle = new TextComponent("Find the ").append(((PositionData) map.getData(PositionType.HIDER)).getRawPluralName());
            subtitle = HideAndSeek.getInstance().getLangProvider().getMessageAsComponent(getKey("start_subtitle", PositionType.MAIN_SEEKER), map.getData(PositionType.HIDER));

            currentMap.getSeekerSpawn().teleport(player);

        } else {
            setPosition(u, PositionType.HIDER);

            //subtitle = new TextComponent("Hide from ").append(((PositionData) map.getData(PositionType.MAIN_SEEKER)).getRawProperName());
            subtitle = HideAndSeek.getInstance().getLangProvider().getMessageAsComponent(getKey("start_subtitle", PositionType.HIDER), map.getData(PositionType.MAIN_SEEKER));

            currentMap.getHiderSpawn().teleport(player);

            hidden.put(u, new ArrayList<>());
        }

        Component title = HideAndSeek.getInstance().getLangProvider().getMessageAsComponent(getKey("start_title", positions.get(u)), player, map.getData(positions.get(u)));

        AbstractClass clazz = HideAndSeekAPI.getInstance().getRegistry().chooseClass(u, map, positions.get(u));

        if (clazz != null) {
            clazz.applyToPlayer(u, null);
            classes.put(u, clazz);
        }

        ClientboundSetTitlesPacket tp = new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.TITLE, title, 12, 80, 20);
        ClientboundSetTitlesPacket sp = new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.SUBTITLE, subtitle, 12, 80, 20);

        player.connection.send(tp);
        player.connection.send(sp);

        currentMap.addToTeam(player.getGameProfile().getName(), positions.get(u));

        try {

            CustomScoreboard sb = new CustomScoreboard(RandomStringUtils.random(15, true, true), new TextComponent("HideAndSeek").setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true)));

            sb.setLine(7, new TextComponent("                         "));
            sb.setLine(6, new TextComponent("Phase: ").append(new TextComponent("Hiding").setStyle(Style.EMPTY.withColor(ChatFormatting.BLUE))));
            sb.setLine(5, new TextComponent("                         "));
            sb.setLine(4, new TextComponent("Position: ").append(((PositionData) map.getData(positions.get(u))).getRawName()));
            sb.setLine(3, new TextComponent("Map: ").append(TextUtil.parse(map.getName())));
            sb.setLine(2, new TextComponent("                         "));
            sb.setLine(1, ((PositionData) map.getData(PositionType.HIDER)).getRawPluralName().plainCopy().append(new TextComponent(": ").append(new TextComponent(getPlayerCount() - 1 + "").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(map.getData(PositionType.HIDER).getColor().toDecimal()))))));

            sb.addPlayer(player);

            scoreboards.put(u, sb);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override
    protected void onPlayerRemoved(UUID u) {
        super.onPlayerRemoved(u);

        ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if(pl == null) return;

        currentMap.removeFromTeam(pl.getGameProfile().getName());
        currentMap.removeTeams(pl);

        scoreboards.get(u).removePlayer(pl);

    }

    @Override
    protected void broadcastVictoryTitle(PositionType winner) {

        for(UUID u : lobby.getPlayerIds()) {

            ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if(player == null) continue;

            Component title;
            if(winner == null) {
                title = HideAndSeek.getInstance().getLangProvider().getMessageAsComponent(getKey("end_title_draw", null));
            } else {
                if(winner.isSeeker() == positions.get(u).isSeeker()) {
                    title = HideAndSeek.getInstance().getLangProvider().getMessageAsComponent(getKey("end_title_win", winner), map.getData(winner));
                } else {
                    title = HideAndSeek.getInstance().getLangProvider().getMessageAsComponent(getKey("end_title_lose", winner), map.getData(winner));
                }
            }

            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 1.0f, 1.3f);
            player.connection.send(new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.TITLE, title, 12, 80, 20));
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

        scoreboards.get(u).setLine(3, new TextComponent("Position: ").append(((PositionData) map.getData(positions.get(u))).getRawName()));
    }

    @Override
    protected void checkVictory() {
        super.checkVictory();

        for(CustomScoreboard sb : scoreboards.values()) {
            sb.setLine(1, ((PositionData) map.getData(PositionType.HIDER)).getRawPluralName().plainCopy().append(new TextComponent(": ").append(new TextComponent(countPosition(PositionType.HIDER) + "").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(map.getData(PositionType.HIDER).getColor().toDecimal()))))));
            sb.update();
        }
    }
}
