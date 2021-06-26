package me.m1dnightninja.hideandseek.api.core;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.game.PositionType;
import me.m1dnightninja.hideandseek.api.integration.SkinSetterIntegration;
import me.m1dnightninja.hideandseek.api.game.*;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.inventory.AbstractInventoryGUI;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.module.IPlayerDataModule;
import me.m1dnightninja.midnightcore.api.module.skin.Skin;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class HideAndSeekRegistry {

    private final HashMap<String, Map> maps = new HashMap<>();
    private final HashMap<String, Lobby> lobbies = new HashMap<>();
    private final HashMap<String, AbstractClass> classes = new HashMap<>();
    private final HashMap<String, SavedSkin> skins = new HashMap<>();
    private final HashMap<String, GameType> gameTypes = new HashMap<>();

    private final HashMap<UUID, HashMap<Map, HashMap<PositionType, AbstractClass>>> preferredClasses = new HashMap<>();

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

    public void registerMap(Map map) {
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

    public void registerSkin(SavedSkin skin) {
        if(skins.containsKey(skin.getId())) return;
        if(skin.getSkin() == null) {
            HideAndSeekAPI.getLogger().warn("Attempt to register null skin!");
        }
        skins.put(skin.getId(), skin);
    }

    public void registerGameType(GameType type) {

        if(gameTypes.containsKey(type.getId())) return;
        gameTypes.put(type.getId(), type);
    }


    public Map getMap(String id) {
        return maps.get(id);
    }

    public Lobby getLobby(String id) {
        return lobbies.get(id);
    }

    public AbstractClass getClass(String id) {
        return classes.get(id);
    }

    private boolean checked = false;
    public SavedSkin getSkin(String id) {

        if(checked) {
            checked = false;
            return null;
        }
        checked = true;

        SavedSkin s = skins.get(id);

        if(s == null && skinSetterPresent) {
            Skin s1 = SkinSetterIntegration.getSkin(id);
            if(s1 == null) return null;

            s = new SavedSkin(id, s1);
        }

        checked = false;
        return s;
    }

    public GameType getGameType(String id) {
        return gameTypes.get(id);
    }


    public List<Map> getMaps() {
        return new ArrayList<>(maps.values());
    }

    public List<Lobby> getLobbies() {
        return new ArrayList<>(lobbies.values());
    }

    public List<AbstractClass> getClasses() {
        return new ArrayList<>(classes.values());
    }

    public List<SavedSkin> getSkins() {
        return new ArrayList<>(skins.values());
    }

    public void setPreferredClass(UUID u, Map map, PositionType type, AbstractClass clazz) {

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

    public AbstractClass getPreferredClass(UUID u, Map map, PositionType type) {

        if(!preferredClasses.containsKey(u) || !preferredClasses.get(u).containsKey(map)) return null;
        return preferredClasses.get(u).get(map).get(type);
    }

    public AbstractClass chooseClass(MPlayer u, Map map, PositionType type) {

        AbstractClass out = getPreferredClass(u.getUUID(), map, type);
        if(out == null) {
            out = map.chooseRandomClass(u, type);
        }

        return out;
    }


    public List<Lobby> getLobbies(MPlayer u) {

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

                Map mp = maps.get(s);
                AbstractClass clazz = mp.getClassOrGlobal(map.getString(pt));

                setPreferredClass(u, mp, pos, clazz);
            }
        }

        playerData.savePlayerData(u);
    }

    public void openLobbyGUI(MPlayer player, Function<Lobby, Boolean> test, BiConsumer<Lobby, AbstractInventoryGUI.ClickType> func) {

        MComponent title = HideAndSeekAPI.getInstance().getLangProvider().getMessage("gui.lobby.title", player);
        AbstractInventoryGUI gui = MidnightCoreAPI.getInstance().createInventoryGUI(title);

        int index = 0;
        for(Lobby l : lobbies.values()) {

            if(!test.apply(l)) continue;
            MItemStack is = l.getDisplayStack();

            gui.setItem(is, index, (type, user) -> func.accept(l, type));

            index++;
        }

        gui.open(player, 0);
    }

    public void openMapGUI(MPlayer player, Function<Map, Boolean> test, BiConsumer<Map, AbstractInventoryGUI.ClickType> func) {

        MComponent title = HideAndSeekAPI.getInstance().getLangProvider().getMessage("gui.map.title", player);
        AbstractInventoryGUI gui = MidnightCoreAPI.getInstance().createInventoryGUI(title);

        int index = 0;
        for(Map m : maps.values()) {

            if(!test.apply(m)) continue;
            MItemStack is = m.getDisplayItem();

            gui.setItem(is, index, (type, user) -> func.accept(m, type));

            index++;
        }

        gui.open(player, 0);
    }

    public void openRoleGUI(MPlayer player, Map map, Function<PositionType, Boolean> test, BiConsumer<PositionType, AbstractInventoryGUI.ClickType> func) {

        MComponent title = HideAndSeekAPI.getInstance().getLangProvider().getMessage("gui.role.title", player);
        AbstractInventoryGUI gui = MidnightCoreAPI.getInstance().createInventoryGUI(title);

        int index = 0;
        for(PositionType t : PositionType.values()) {

            if(!test.apply(t)) continue;
            PositionData data = map.getData(t);

            MItemStack is = MItemStack.Builder.woolWithColor(data.getColor()).withName(data.getName()).build();

            gui.setItem(is, index, (type, user) -> func.accept(t, type));

            index++;
        }

        gui.open(player, 0);
    }

    public void openClassGUI(MPlayer player, Map map, PositionType role, Function<AbstractClass, Boolean> test, BiConsumer<AbstractClass, AbstractInventoryGUI.ClickType> func) {

        MComponent title = HideAndSeekAPI.getInstance().getLangProvider().getMessage("gui.class.title", player);
        AbstractInventoryGUI gui = MidnightCoreAPI.getInstance().createInventoryGUI(title);

        PositionData data = map.getData(role);

        int index = 0;
        for(AbstractClass c : data.getClasses()) {

            if(!test.apply(c)) continue;

            MItemStack is;
            if(!c.getSkins().isEmpty()) {
                is = MItemStack.Builder.headWithSkin(skins.get(c.getSkins().get(0)).getSkin()).withName(c.getName()).build();
            } else {
                is = MItemStack.Builder.woolWithColor(data.getColor()).withName(c.getName()).build();
            }

            gui.setItem(is, index, (type, user) -> func.accept(c, type));

            index++;
        }

        gui.open(player, 0);
    }

    public void openCustomizeGUI(MPlayer player) {

        MComponent title = HideAndSeekAPI.getInstance().getLangProvider().getMessage("gui.customize.title", player);
        AbstractInventoryGUI gui = MidnightCoreAPI.getInstance().createInventoryGUI(title);

        MItemStack customize = MItemStack.Builder.of(HideAndSeekAPI.getInstance().getMainSettings().getPreferredClassesItem()).withName(HideAndSeekAPI.getInstance().getLangProvider().getMessage("gui.customize.preferred_classes", player)).build();
        gui.setItem(customize, 4, (click, user) ->
            openMapGUI(user, map -> {
                for(PositionType t : PositionType.values()) {
                    if(map.getData(t).canCustomize(user)) return true;
                }
                return false;
            }, (map, clickM) ->
                openRoleGUI(user, map, type -> map.getData(type).canCustomize(user), (type, clickR) ->
                    openClassGUI(user, map, type, clazz -> clazz.canUse(user), (clazz, clickC) -> {
                        setPreferredClass(user.getUUID(), map, type, clazz);
                        HideAndSeekAPI.getInstance().getLangProvider().sendMessage("command.customize.class_updated", user, map, type, map.getData(type), clazz);
                    })
                )
            )
        );

        gui.open(player, 0);
    }

}
