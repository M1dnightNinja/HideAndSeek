package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.skin.Skin;

import java.util.UUID;

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

    public static SkinOption parse(ConfigSection sec) {

        String id = sec.getString("id");

        String uid = sec.getString("uid");
        String b64 = sec.getString("b64");
        String sig = sec.getString("sig");

        SkinOption out = new SkinOption(id, new Skin(UUID.fromString(uid), b64, sig));

        if(sec.has("name", String.class)) {
            out.display = sec.getString("name");
        }

        return out;
    }

}
