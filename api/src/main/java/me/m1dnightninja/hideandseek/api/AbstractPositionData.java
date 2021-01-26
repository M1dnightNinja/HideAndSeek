package me.m1dnightninja.hideandseek.api;

import me.m1dnightninja.midnightcore.api.Color;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPositionData {

    protected final PositionType type;
    protected final List<AbstractClass> classes;

    protected String name;
    protected String pluralName;
    protected String properName;

    protected Color color;

    public AbstractPositionData(PositionType type) {
        this.type = type;
        this.classes = new ArrayList<>();

        this.name = type.name;
        this.pluralName = name + "s";
        this.properName = "The " + name;
        this.color = type.defaultColor;
    }

    public PositionType getType() {
        return type;
    }

    public List<AbstractClass> getClasses() {
        return classes;
    }

    public String getName() {
        return name;
    }

    public String getPluralName() {
        return pluralName;
    }

    public String getProperName() {
        return properName;
    }

    public Color getColor() {
        return color;
    }
}
