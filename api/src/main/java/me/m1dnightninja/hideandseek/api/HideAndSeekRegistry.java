package me.m1dnightninja.hideandseek.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class HideAndSeekRegistry {

    private final HashMap<String, AbstractMap> maps = new HashMap<>();
    private final HashMap<String, AbstractLobby> lobbies = new HashMap<>();
    private final HashMap<String, AbstractClass> classes = new HashMap<>();
    private final HashMap<String, SkinOption> skins = new HashMap<>();
    private final HashMap<String, GameType> gameTypes = new HashMap<>();

    private final HashMap<UUID, HashMap<AbstractMap, HashMap<PositionType, AbstractClass>>> preferredClasses = new HashMap<>();

    public void registerMap(AbstractMap map) {
        if(maps.containsKey(map.getId())) return;
        maps.put(map.getId(), map);
    }

    public void registerLobby(AbstractLobby lobby) {
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

    public AbstractLobby getLobby(String id) {
        return lobbies.get(id);
    }

    public AbstractClass getClass(String id) {
        return classes.get(id);
    }

    public SkinOption getSkin(String id) {
        return skins.get(id);
    }

    public GameType getGameType(String id) {
        return gameTypes.get(id);
    }


    public List<AbstractMap> getMaps() {
        return new ArrayList<>(maps.values());
    }

    public List<AbstractLobby> getLobbies() {
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


    public List<AbstractLobby> getLobbies(UUID u) {

        if(u == null) return getLobbies();

        List<AbstractLobby> lbs = new ArrayList<>();
        for(AbstractLobby l : lobbies.values()) {
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

}
