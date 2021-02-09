package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Region {

    private final String id;
    private final Vec3d pos;
    private final Vec3d size;

    private final List<PositionType> denied = new ArrayList<>();

    private String display;

    public Region(String id, Vec3d pos, Vec3d size) {
        this.id = id;
        this.pos = pos;
        this.size = size;
        this.display = id;
    }

    public String getId() {
        return id;
    }

    public List<PositionType> getDenied() {
        return denied;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public boolean isInRegion(Vec3d v) {
        return pos.getX() <= v.getX() && v.getX() <= pos.getX() + size.getX() &&
                pos.getY() <= v.getY() && v.getY() <= pos.getY() + size.getY() &&
                pos.getZ() <= v.getZ() && v.getZ() <= pos.getZ() + size.getZ();
    }

    public static Region parse(ConfigSection sec) {

        if(!sec.has("id", String.class) || !sec.has("position", String.class) || !sec.has("size", String.class)) return null;

        String id = sec.getString("id");
        Vec3d pos = Vec3d.parse(sec.getString("position"));
        Vec3d size = Vec3d.parse(sec.getString("size"));

        Region out = new Region(id, pos, size);

        if(sec.has("name", String.class)) {
            out.display = sec.getString("name");
        }

        if(sec.has("denied", List.class)) {
            for(Object o : sec.get("denied", List.class)) {
                if(!(o instanceof String)) continue;

                out.denied.add(PositionType.getById((String) o));
            }
        }

        return out;
    }
}
