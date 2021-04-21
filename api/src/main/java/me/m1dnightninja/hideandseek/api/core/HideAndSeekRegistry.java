package me.m1dnightninja.hideandseek.api.core;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import me.m1dnightninja.hideandseek.api.integration.SkinSetterIntegration;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.module.IPlayerDataModule;
import me.m1dnightninja.midnightcore.api.module.skin.Skin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class HideAndSeekRegistry {

    private final HashMap<String, AbstractMap> maps = new HashMap<>();
    private final HashMap<String, Lobby> lobbies = new HashMap<>();
    private final HashMap<String, AbstractClass> classes = new HashMap<>();
    private final HashMap<String, SkinOption> skins = new HashMap<>();
    private final HashMap<String, GameType> gameTypes = new HashMap<>();

    private final HashMap<UUID, HashMap<AbstractMap, HashMap<PositionType, AbstractClass>>> preferredClasses = new HashMap<>();

    private final boolean skinSetterPresent;

    private final IPlayerDataModule playerData;

    public HideAndSeekRegistry() {
        boolean ss;
        try {
            Class.forName("me.m1dnightninja.skinsetter.api.SkinSetterAPI");
            HideAndSeekAPI.getLogger().info("SkinSetter found!");
            ss = true;
        } catch (ClassNotFoundException ex) {
            ss = false;
        }

        skinSetterPresent = ss;

        playerData = MidnightCoreAPI.getInstance().getModule(IPlayerDataModule.class);
    }

    public void registerMap(AbstractMap map) {
        if(maps.containsKey(map.getId())) return;
        maps.put(map.getId(), map);
    }

    public void registerLobby(Lobby lobby) {
        if(lobbies.containsKey(lobby.getId())) return;
        if(lobby.getMaps().size() == 0) return;

        lobbies.put(lobby.getId(), lobby);
    }

    public void registerClass(AbstractClass clazz) {
        if(classes.containsKey(clazz.getId())) return;
        classes.put(clazz.getId(), clazz);
    }

    public void registerSkin(SkinOption skin) {
        if(skins.containsKey(skin.getId())) return;
        skins.put(skin.getId(), skin);
    }

    public void registerGameType(GameType type) {
        if(gameTypes.containsKey(type.getId())) return;
        gameTypes.put(type.getId(), type);
    }


    public AbstractMap getMap(String id) {
        return maps.get(id);
    }

    public Lobby getLobby(String id) {
        return lobbies.get(id);
    }

    public AbstractClass getClass(String id) {
        return classes.get(id);
    }

    private boolean checked = false;
    public SkinOption getSkin(String id) {

        if(checked) {
            checked = false;
            return null;
        }
        checked = true;

        SkinOption s = skins.get(id);

        if(s == null && skinSetterPresent) {
            Skin s1 = SkinSetterIntegration.getSkin(id);
            s = new SkinOption(id, s1);
        }

        checked = false;
        return s;
    }

    public GameType getGameType(String id) {
        return gameTypes.get(id);
    }


    public List<AbstractMap> getMaps() {
        return new ArrayList<>(maps.values());
    }

    public List<Lobby> getLobbies() {
        return new ArrayList<>(lobbies.values());
    }

    public List<AbstractClass> getClasses() {
        return new ArrayList<>(classes.values());
    }

    public List<SkinOption> getSkins() {
        return new ArrayList<>(skins.values());
    }

    public List<GameType> getGameTypes() {
        return new ArrayList<>(gameTypes.values());
    }


    public void setPreferredClass(UUID u, AbstractMap map, PositionType type, AbstractClass clazz) {

        preferredClasses.computeIfAbsent(u, k -> new HashMap<>());
        preferredClasses.get(u).computeIfAbsent(map, k -> new HashMap<>());
        preferredClasses.get(u).get(map).put(type, clazz);

        ConfigSection sec = playerData.getPlayerData(u);
        ConfigSection has = sec.getOrCreateSection("hideandseek");
        ConfigSection cls = has.getOrCreateSection("classes");
        ConfigSection mpd = cls.getOrCreateSection(map.getId());

        mpd.set(type.getId(), clazz.getId());

        playerData.savePlayerData(u);
    }

    public AbstractClass getPreferredClass(UUID u, AbstractMap map, PositionType type) {

        if(!preferredClasses.containsKey(u)) return null;
        return preferredClasses.get(u).get(map).get(type);
    }

    public AbstractClass chooseClass(UUID u, AbstractMap map, PositionType type) {

        AbstractClass out = getPreferredClass(u, map, type);
        if(out == null) {
            out = map.chooseRandomClass(type);
        }

        return out;
    }


    public List<Lobby> getLobbies(UUID u) {

        if(u == null) return getLobbies();

        List<Lobby> lbs = new ArrayList<>();
        for(Lobby l : lobbies.values()) {
            if(l.canAccess(u)) lbs.add(l);
        }

        return lbs;
    }

    public void clear() {
        maps.clear();
        lobbies.clear();
        classes.clear();
        skins.clear();
        gameTypes.clear();
    }

    public void loadData(UUID u) {

        ConfigSection sec = playerData.getPlayerData(u);

        // Root
        if(!sec.has("hideandseek", ConfigSection.class)) return;
        ConfigSection has = sec.getSection("hideandseek");

        // Classes
        if(!has.has("classes", ConfigSection.class)) return;
        ConfigSection classes = has.getSection("classes");

        // Find all maps within classes
        for(String s : classes.getKeys()) {

            if(!classes.has(s, ConfigSection.class) || !maps.containsKey(s)) continue;

            ConfigSection map = classes.getSection(s);

            for(String pt : map.getKeys()) {

                PositionType pos = PositionType.getById(pt);
                if(pos == null) continue;

                AbstractMap mp = maps.get(s);
                AbstractClass clazz = mp.getClassOrGlobal(map.getString(pt));

                setPreferredClass(u, mp, pos, clazz);
            }
        }

        playerData.savePlayerData(u);
    }

}
