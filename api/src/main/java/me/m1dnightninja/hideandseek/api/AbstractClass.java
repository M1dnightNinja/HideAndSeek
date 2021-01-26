package me.m1dnightninja.hideandseek.api;

import java.util.*;

public abstract class AbstractClass {

    protected final String id;

    protected final List<SkinOption> skins = new ArrayList<>();

    protected final HashMap<PositionType, String> tempEquivalencies = new HashMap<>();
    protected final HashMap<PositionType, AbstractClass> equivalencies = new HashMap<>();


    public AbstractClass(String id) {
        this.id = id;
    }


    public String getId() {
        return id;
    }

    public List<SkinOption> getSkins() {
        return skins;
    }

    public AbstractClass getEquivalent(PositionType type) {
        return equivalencies.get(type);
    }

    public void updateEquivalencies() {
        for(Map.Entry<PositionType, String> ent : tempEquivalencies.entrySet()) {
            AbstractClass clazz = HideAndSeekAPI.getInstance().getRegistry().getClass(ent.getValue());
            equivalencies.put(ent.getKey(), clazz);
        }
        tempEquivalencies.clear();
    }

    public abstract void applyToPlayer(UUID uid, SkinOption option);
}
