package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;

import java.util.*;

public abstract class AbstractClass {

    protected final String id;
    protected String name;

    protected final List<String> desc = new ArrayList<>();

    protected final List<SkinOption> skins = new ArrayList<>();

    protected final HashMap<PositionType, String> tempEquivalencies = new HashMap<>();
    protected final HashMap<PositionType, AbstractClass> equivalencies = new HashMap<>();

    private boolean dirty = false;

    public AbstractClass(String id) {
        this.id = id;
        this.name = id;
    }


    public String getId() {
        return id;
    }

    public List<String> getDescription() {
        return desc;
    }

    public String getName() {
        return name;
    }

    public List<SkinOption> getSkins() {
        return skins;
    }

    public AbstractClass getEquivalent(PositionType type) {
        if(dirty) updateEquivalencies();
        return equivalencies.get(type);
    }

    public void updateEquivalencies() {
        equivalencies.clear();

        for(Map.Entry<PositionType, String> ent : tempEquivalencies.entrySet()) {
            AbstractClass clazz = HideAndSeekAPI.getInstance().getRegistry().getClass(ent.getValue());
            equivalencies.put(ent.getKey(), clazz);
        }
        tempEquivalencies.clear();
        dirty = false;
    }

    public abstract void applyToPlayer(UUID uid, SkinOption option);

    public void fromConfig(ConfigSection sec) {

        skins.clear();
        tempEquivalencies.clear();
        equivalencies.clear();

        if(sec.has("name")) {
            name = sec.getString("name");
        }

        if(sec.has("description", List.class)) {
            desc.addAll(sec.getStringList("description"));
        }

        if(sec.has("skins", List.class)) {
            for(Object o : sec.get("skins", List.class)) {

                if(!(o instanceof String)) continue;

                SkinOption opt = HideAndSeekAPI.getInstance().getRegistry().getSkin((String) o);
                skins.add(opt);

            }
        }

        if(sec.has("equivalencies", ConfigSection.class)) {
            for(Map.Entry<String, Object> ent : sec.get("equivalencies", ConfigSection.class).getEntries().entrySet()) {
                if(!(ent.getValue() instanceof String)) continue;

                for(PositionType t : PositionType.values()) {
                    if(t.getId().equals(ent.getKey())) {
                        tempEquivalencies.put(t, (String) ent.getValue());
                    }
                }
            }
        }

        dirty = true;

    }

}
