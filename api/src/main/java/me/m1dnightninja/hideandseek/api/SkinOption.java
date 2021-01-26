package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.skin.Skin;

public class SkinOption {

    private final String id;
    private final Skin skin;

    private String display;

    public SkinOption(String id, Skin skin) {
        this.id = id;
        this.skin = skin;
        this.display = id;
    }

    public String getId() {
        return id;
    }

    public Skin getSkin() {
        return skin;
    }

    public String getDisplayName() {
        return display;
    }

    public void setDisplayName(String s) { this.display = s; }
}
