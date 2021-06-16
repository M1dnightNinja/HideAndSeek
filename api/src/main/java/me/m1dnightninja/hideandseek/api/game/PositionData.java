package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PositionData {

    protected final PositionType type;
    protected final List<AbstractClass> classes;

    protected MComponent name;
    protected MComponent pluralName;
    protected MComponent properName;

    protected Color color;

    public PositionData(PositionType type) {
        this.type = type;
        this.classes = new ArrayList<>();

        this.name = type.getName();
        this.color = type.getDefaultColor();
    }

    public PositionType getType() {
        return type;
    }

    public List<AbstractClass> getClasses() {
        return classes;
    }

    public MComponent getName() {
        return name;
    }

    public MComponent getPluralName() {
        return pluralName == null ? name : pluralName;
    }

    public MComponent getProperName() {
        return properName == null ? name : properName;
    }

    public Color getColor() {
        return color;
    }

    public List<AbstractClass> getPlayerClasses(MPlayer player) {

        List<AbstractClass> classes = new ArrayList<>();
        for(AbstractClass clazz : getClasses()) {
            if(clazz.canUse(player)) {
                classes.add(clazz);
            }
        }

        return classes;
    }

    public boolean canCustomize(MPlayer player) {

        return getPlayerClasses(player).size() > 1;
    }

    public void fromConfig(ConfigSection sec, Map map) {

        if(sec.has("name", String.class)) {
            name = MComponent.Serializer.parse(sec.getString("name"));
        }

        if(sec.has("name_plural", String.class)) {
            pluralName = MComponent.Serializer.parse(sec.getString("name_plural"));
        } else {
            pluralName = name.copy();
            pluralName.addChild(MComponent.createTextComponent("s"));
        }

        if(sec.has("name_proper", String.class)) {
            properName = MComponent.Serializer.parse(sec.getString("name_proper"));
        } else {
            properName = MComponent.createTextComponent("The ").withStyle(name.getStyle());
            properName.addChild(name.copy());
        }

        if(sec.has("color", String.class)) {
            color = Color.parse(sec.getString("color"));
        } else if(sec.has("color", Number.class)) {
            color = new Color(sec.getInt("color"));
        }

        if(sec.has("classes", List.class)) {

            HashMap<String, AbstractClass> tclasses = new HashMap<>();

            for(AbstractClass clazz : HideAndSeekAPI.getInstance().getRegistry().getClasses()) {
                tclasses.put(clazz.getId(), clazz);
            }
            if(map != null) {
                for (AbstractClass clazz : map.getMapClasses()) {
                    tclasses.put(clazz.getId(), clazz);
                }
            }

            for(String o : sec.getListFiltered("classes", String.class)) {

                AbstractClass clazz = tclasses.get(o);
                if(clazz != null) classes.add(clazz);
            }
        }
    }

    public static PositionData parse(ConfigSection sec, PositionType type, Map map) {

        PositionData dt = new PositionData(type);
        dt.fromConfig(sec, map);

        return dt;
    }

}
