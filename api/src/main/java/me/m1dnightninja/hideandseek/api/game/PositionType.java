package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.text.MComponent;

public enum PositionType {

    HIDER("hider", "Hider", false, new Color("1d8bf2")),
    MAIN_HIDER("main_hider", "Main Hider", false, new Color("1d8bf2")),
    SEEKER("seeker", "Seeker", true, new Color("d12828")),
    MAIN_SEEKER("main_seeker", "Main Seeker", true, new Color("d12828"));

    String id;
    MComponent name;
    boolean seeker;
    Color defaultColor;

    PositionType(String id, String name, boolean seeker, Color defaultColor) {
        this.id = id;
        this.name = MComponent.createTextComponent(name);
        this.seeker = seeker;
        this.defaultColor = defaultColor;
    }

    public static PositionType getById(String id) {
        for(PositionType t : values()) {
            if(t.getId().equals(id)) return t;
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public MComponent getName() {
        return name;
    }

    public boolean isSeeker() {
        return seeker;
    }

    public Color getDefaultColor() {
        return defaultColor;
    }
}
