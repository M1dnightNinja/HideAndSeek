package me.m1dnightninja.hideandseek.fabric.gamemode;

import me.m1dnightninja.hideandseek.fabric.HideAndSeek;
import me.m1dnightninja.hideandseek.fabric.MapInstance;
import me.m1dnightninja.hideandseek.fabric.mixin.AccessorPlayerSpawnPacket;
import me.m1dnightninja.hideandseek.fabric.util.ConversionUtil;
import me.m1dnightninja.hideandseek.fabric.util.FireworkUtil;
import me.m1dnightninja.hideandseek.api.*;
import me.m1dnightninja.hideandseek.api.AbstractMap;
import me.m1dnightninja.midnightcore.api.AbstractTimer;
import me.m1dnightninja.midnightcore.api.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.api.Location;
import me.m1dnightninja.midnightcore.fabric.api.Timer;
import me.m1dnightninja.midnightcore.fabric.api.event.PacketSendEvent;
import me.m1dnightninja.midnightcore.fabric.api.event.ServerTickEvent;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import me.m1dnightninja.midnightcore.fabric.util.TextUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.FireworkRocketItem;

import java.util.*;
import java.util.List;

public class ClassicGameMode extends AbstractGameInstance {

    private final ServerPlayer seeker;
    private final AbstractMap map;

    private MapInstance currentMap;
    private ClassicGameState state = ClassicGameState.UNINITIALIZED;

    private final HashMap<ServerPlayer, Vec3d> locations = new HashMap<>();
    private final List<ServerPlayer> toTeleport = new ArrayList<>();

    private final List<AbstractTimer> runningTimers = new ArrayList<>();

    private AbstractTimer hiderTimer;
    private AbstractTimer seekerTimer;

    public ClassicGameMode(AbstractLobbySession lobby, UUID seeker, AbstractMap startMap) {
        super(lobby);

        if(seeker == null) seeker = lobby.getPlayers().get(HideAndSeekAPI.getInstance().getRandom().nextInt(lobby.getPlayers().size()));
        if(startMap == null) startMap = lobby.getLobby().getMaps().get(HideAndSeekAPI.getInstance().getRandom().nextInt(lobby.getLobby().getMaps().size()));

        this.seeker = MidnightCore.getServer().getPlayerList().getPlayer(seeker);
        this.map = startMap;

        Event.register(ServerTickEvent.class, this, event -> {

            if(state == ClassicGameState.UNINITIALIZED) return;

            for(Map.Entry<UUID, PositionType> ent : positions.entrySet()) {

                ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(ent.getKey());
                if(player == null) continue;

                Vec3d newLoc = new Vec3d(player.getX(), player.getY(), player.getZ());
                locations.computeIfAbsent(player, k -> newLoc);

                if(state == ClassicGameState.HIDING && positions.get(ent.getKey()).isSeeker()) {

                    if(!newLoc.equals(locations.get(player))) toTeleport.add(player);
                }

                for(Region reg : map.getRegions()) {
                    if(reg.getDenied().contains(ent.getValue()) && reg.isInRegion(newLoc)) {

                        player.sendMessage(new TextComponent("").setStyle(Style.EMPTY.withColor(ChatFormatting.RED))
                                .append(TextUtil.parse(map.getData(positions.get(ent.getKey())).getPluralName()))
                                .append(new TextComponent(" cannot enter "))
                                .append(TextUtil.parse(reg.getDisplay()))
                                .append("!"), ChatType.SYSTEM, Util.NIL_UUID);
                        toTeleport.add(player);
                    }
                }

                if(toTeleport.contains(player)) {
                    player.teleportTo(locations.get(player).getX(), locations.get(player).getY(), locations.get(player).getZ());
                    toTeleport.remove(player);
                } else {
                    locations.put(player, newLoc);
                }
            }
        });

        Event.register(PacketSendEvent.class, this, event -> {
            if(state == ClassicGameState.HIDING && event.getPacket() instanceof ClientboundAddPlayerPacket && positions.containsKey(event.getPlayer().getUUID()) && positions.get(event.getPlayer().getUUID()).isSeeker()) {

                UUID id = ((AccessorPlayerSpawnPacket) event.getPacket()).getPlayerId();
                if(positions.containsKey(id) && !positions.get(id).isSeeker()) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @Override
    public void start() {

        HideAndSeek.getInstance().getDimensionManager().loadMapWorld(map, lobby.getLobby().getId(), lobby.getLobby().getId(), (world) -> {
            if(world == null) {
                shutdown();
                return;
            }

            try {
                currentMap = new MapInstance(map, world, true);
            } catch (Throwable th) {
                th.printStackTrace();
                shutdown();
            }
            startHiding();
        });

    }

    private void startHiding() {

        state = ClassicGameState.HIDING;

        Timer timer = new Timer(TextUtil.parse(map.getData(PositionType.MAIN_SEEKER).getName()).append(" released in "), map.getHideTime(), false, new AbstractTimer.TimerCallback() {
            @Override
            public void tick(int secondsLeft) {
                if(secondsLeft == 0) {
                    for (UUID u : lobby.getPlayers()) {

                        ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(u);
                        if(pl == null) continue;

                        pl.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0f, 1.0f);
                    }
                } else if(secondsLeft <= 5) {
                    for (UUID u : lobby.getPlayers()) {

                        ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(u);
                        if(pl == null) continue;

                        pl.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0f, 1.0f);
                    }
                }
            }

            @Override
            public void finish() {
                startSeeking();
            }
        });

        runningTimers.add(timer);

        try {
            for (UUID u : lobby.getPlayers()) {

                ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
                if (player == null) continue;

                Component subtitle;
                if (u.equals(seeker.getUUID())) {
                    setPosition(u, PositionType.MAIN_SEEKER);

                    subtitle = new TextComponent("Find the ").append(TextUtil.parse(map.getData(PositionType.HIDER).getPluralName()));

                    currentMap.getSeekerSpawn().teleport(player);
                } else {
                    setPosition(u, PositionType.HIDER);

                    subtitle = new TextComponent("Hide from ").append(TextUtil.parse(map.getData(PositionType.MAIN_SEEKER).getProperName()));

                    currentMap.getHiderSpawn().teleport(player);
                }

                Component title = TextUtil.parse(map.getData(positions.get(u)).getName());

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

                timer.addPlayer(player.getUUID());

            }
        } catch(Throwable th) {
            th.printStackTrace();
        }

        setHidersVisible(false);

        timer.start();

    }

    private void startSeeking() {

        state = ClassicGameState.SEEKING;

        setHidersVisible(true);
        cancelTimers();

        hiderTimer = new Timer(TextUtil.parse(map.getData(PositionType.HIDER).getName()).append(" "), map.getSeekTime(), false, new AbstractTimer.TimerCallback() {
            @Override
            public void tick(int secondsLeft) { }

            @Override
            public void finish() {
                endGame(PositionType.HIDER);
            }
        });
        seekerTimer = new Timer(TextUtil.parse(map.getData(PositionType.SEEKER).getName()).append(" "), map.getSeekTime(), false, null);
        Timer mainSeekerTimer = new Timer(TextUtil.parse(map.getData(PositionType.MAIN_SEEKER).getName()).append(" "), map.getSeekTime(), false, null);

        runningTimers.add(hiderTimer);
        runningTimers.add(seekerTimer);
        runningTimers.add(mainSeekerTimer);

        for(HashMap.Entry<UUID, PositionType> ent : positions.entrySet()) {

            switch(ent.getValue()) {
                case HIDER:
                    hiderTimer.addPlayer(ent.getKey());
                    break;
                case SEEKER:
                    seekerTimer.addPlayer(ent.getKey());
                    break;
                case MAIN_SEEKER:
                    mainSeekerTimer.addPlayer(ent.getKey());
            }

        }

        seekerTimer.start();
        hiderTimer.start();
        mainSeekerTimer.start();

    }

    private void endGame(PositionType winner) {

        state = ClassicGameState.ENDING;

        cancelTimers();

        Component title;
        if(winner == null) {
            title = new TextComponent("Draw");
        } else {
            title = new TextComponent("The ").setStyle(Style.EMPTY.withColor(TextColor.parseColor(map.getData(winner).getColor().toHex()))).append(TextUtil.parse(map.getData(winner).getPluralName()).append(" win!"));
        }

        Timer timer = new Timer(new TextComponent("FINISH ").setStyle(Style.EMPTY.withColor(TextColor.parseColor(lobby.getLobby().getColor().toHex()))), 15, false, new AbstractTimer.TimerCallback() {
            @Override
            public void tick(int secondsLeft) { }

            @Override
            public void finish() {
                shutdown();
            }
        });
        runningTimers.add(timer);
        ClientboundSetTitlesPacket packet = new ClientboundSetTitlesPacket(ClientboundSetTitlesPacket.Type.TITLE, title, 12, 80, 20);

        for(UUID u : lobby.getPlayers()) {

            ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if(player == null) continue;

            player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 1.0f, 1.3f);
            player.connection.send(packet);

            timer.addPlayer(u);
        }

        timer.start();

    }

    @Override
    protected void onPlayerRemoved(UUID u) {

        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if(currentMap != null && player != null) {
            currentMap.removeFromTeam(player.getGameProfile().getName());
        }

        for(AbstractTimer timer : runningTimers) {
            timer.removePlayer(u);
        }

        checkVictory();
    }

    @Override
    public void onDamaged(UUID u, UUID damager, DamageSource source, float amount) {

        ServerPlayer player = MidnightCore.getServer().getPlayerList().getPlayer(u);
        if(player == null) return;

        if(state == ClassicGameState.SEEKING && positions.containsKey(u) && !positions.get(u).isSeeker()) {

            if (damager != null) {

                ServerPlayer attacker = MidnightCore.getServer().getPlayerList().getPlayer(damager);
                if (attacker == null || !positions.containsKey(damager) || !positions.get(damager).isSeeker()) return;

                setPlayerSeeker(player);
                broadcastMessage(new TextComponent("")
                        .append(player.getName().plainCopy().withStyle(Style.EMPTY.withColor(TextColor.parseColor(map.getData(PositionType.HIDER).getColor().toHex()))))
                        .append(" was tagged by ")
                        .append(attacker.getName().plainCopy().withStyle(Style.EMPTY.withColor(TextColor.parseColor(map.getData(PositionType.SEEKER).getColor().toHex()))))
                        .append("! ")
                        .append(getRemainsText()));

            } else if (map.getTagSources().contains(source)) {

                net.minecraft.world.damagesource.DamageSource minecraft = ConversionUtil.convertDamageSource(source);

                setPlayerSeeker(player);
                broadcastMessage(new TextComponent("")
                        .append(player.getName().plainCopy().withStyle(Style.EMPTY.withColor(TextColor.parseColor(map.getData(PositionType.HIDER).getColor().toHex()))))
                        .append(new TranslatableComponent("death.attack." + minecraft.msgId))
                        .append("! ")
                        .append(getRemainsText()));

            }
        }

        if(map.getResetSources().contains(source)) {
            if(positions.get(u).isSeeker()) {
                locations.put(player, map.getSeekerSpawn());
            } else {
                locations.put(player, map.getHiderSpawn());
            }
            toTeleport.add(player);
        }

    }

    @Override
    protected void onShutdown() {
        cancelTimers();
        Event.unregisterAll(this);
        if(currentMap != null) {
            currentMap.clearTeams();
            HideAndSeek.getInstance().getDimensionManager().unloadMapWorld(map, lobby.getLobby().getId(), false);
        }
    }

    private void cancelTimers() {
        for(AbstractTimer timer : runningTimers) {
            timer.cancel();
            timer.clearPlayers();
        }
    }

    private void setPlayerSeeker(ServerPlayer player) {

        if(!positions.containsKey(player.getUUID()) || positions.get(player.getUUID()).isSeeker()) return;

        hiderTimer.removePlayer(player.getUUID());
        seekerTimer.addPlayer(player.getUUID());

        positions.put(player.getUUID(), PositionType.SEEKER);

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

        checkVictory();
    }

    private void checkVictory() {

        if(state == ClassicGameState.ENDING) return;

        if(positions.size() <= 1) {
            endGame(null);
            return;
        }

        int hiders = 0;
        int seekers = 0;
        for(PositionType t : positions.values()) {

            if(t.isSeeker()) {
                seekers++;
            } else {
                hiders++;
            }
        }

        if(hiders == 0) {
            endGame(PositionType.SEEKER);
        } else if(seekers == 0) {
            endGame(PositionType.HIDER);
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

    private void broadcastMessage(Component t) {

        for(UUID u : positions.keySet()) {

            ServerPlayer pl = MidnightCore.getServer().getPlayerList().getPlayer(u);
            if(pl == null) continue;

            pl.sendMessage(t, ChatType.SYSTEM, Util.NIL_UUID);

        }

    }

    private void setHidersVisible(boolean visible) {
        if(visible) {

            List<Packet<?>> packets = new ArrayList<>();

            for (Map.Entry<UUID, PositionType> ent : positions.entrySet()) {
                if (ent.getValue().isSeeker()) continue;

                ServerPlayer hider = MidnightCore.getServer().getPlayerList().getPlayer(ent.getKey());
                if (hider == null) continue;

                packets.add(new ClientboundAddPlayerPacket(hider));
                packets.add(new ClientboundSetEntityDataPacket(hider.getId(), hider.getEntityData(), true));
            }

            for (Map.Entry<UUID, PositionType> ent : positions.entrySet()) {
                if (!ent.getValue().isSeeker()) continue;

                ServerPlayer seeker = MidnightCore.getServer().getPlayerList().getPlayer(ent.getKey());
                if (seeker == null) continue;

                for(Packet<?> packet : packets) {
                    seeker.connection.send(packet);
                }
            }

        } else {

            List<Integer> hiders = new ArrayList<>();
            for (Map.Entry<UUID, PositionType> ent : positions.entrySet()) {
                if (ent.getValue().isSeeker()) continue;

                ServerPlayer hider = MidnightCore.getServer().getPlayerList().getPlayer(ent.getKey());
                if (hider == null) continue;

                hiders.add(hider.getId());
            }

            int[] destroy = new int[hiders.size()];

            for(int i = 0 ; i < hiders.size() ; i++) {
                destroy[i] = hiders.get(i);
            }

            ClientboundRemoveEntitiesPacket pck = new ClientboundRemoveEntitiesPacket(destroy);

            for (Map.Entry<UUID, PositionType> ent : positions.entrySet()) {
                if (!ent.getValue().isSeeker()) continue;

                ServerPlayer seeker = MidnightCore.getServer().getPlayerList().getPlayer(ent.getKey());
                if (seeker == null) continue;

                seeker.connection.send(pck);
            }
        }
    }


    private enum ClassicGameState {
        UNINITIALIZED,
        HIDING,
        SEEKING,
        ENDING
    }
}
