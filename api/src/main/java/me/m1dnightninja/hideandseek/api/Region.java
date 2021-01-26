package me.m1dnightninja.hideandseek.api;

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
}
