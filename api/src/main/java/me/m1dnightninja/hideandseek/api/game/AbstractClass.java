package me.m1dnightninja.hideandseek.api.game;

import me.m1dnightninja.hideandseek.api.HideAndSeekAPI;
import me.m1dnightninja.hideandseek.api.integration.MidnightItemsIntegration;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.module.lang.CustomPlaceholderInline;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.api.text.MComponent;

import java.util.*;

public abstract class AbstractClass {

    protected final String id;
    protected MComponent name;

    protected final List<String> desc = new ArrayList<>();
    protected final List<String> skins = new ArrayList<>();
    protected final List<MItemStack> items = new ArrayList<>();
    protected final HashMap<String, MItemStack> equipment = new HashMap<>();

    protected final HashMap<PositionType, String> equivalencies = new HashMap<>();

    protected final HashMap<CommandActivationPoint, List<String>> commands = new HashMap<>();

    protected boolean taggable = true;
    protected String permission = null;

    public AbstractClass(String id) {
        this.id = id;
        this.name = MComponent.createTextComponent(id);
    }


    public String getId() {
        return id;
    }

    public List<String> getDescription() {
        return desc;
    }

    public MComponent getName() {
        return name;
    }

    public List<String> getSkins() {
        return skins;
    }

    public AbstractClass getEquivalent(PositionType type, Map map) {

        return map.getClassOrGlobal(equivalencies.get(type));
    }

    public void executeCommands(CommandActivationPoint point, MPlayer player, MPlayer tagger) {
        if(!commands.containsKey(point)) return;

        for(String s : commands.get(point)) {
            s = HideAndSeekAPI.getInstance().getLangProvider().getModule().applyPlaceholdersFlattened(s, player, new CustomPlaceholderInline("tagger_name", tagger == null ? "" : tagger.getName().allContent()), this);
            executeCommand(s);
        }
    }

    public boolean isTaggable() {
        return taggable;
    }

    public String getPermission() {
        return permission;
    }

    public boolean canUse(MPlayer pl) {
        return permission == null || pl.hasPermission(permission);
    }

    protected abstract void executeCommand(String s);

    public abstract void applyToPlayer(MPlayer uid);

    public void fromConfig(ConfigSection sec) {

        skins.clear();
        equivalencies.clear();

        if(sec.has("name")) {
            name = MComponent.Serializer.parse(sec.getString("name"));
        }

        if(sec.has("description", List.class)) {
            desc.addAll(sec.getStringList("description"));
        }

        if(sec.has("skins", List.class)) {
            for(Object o : sec.get("skins", List.class)) {

                if(!(o instanceof String)) continue;
                skins.add((String) o);
            }
        }

        if(sec.has("items", List.class)) {
            for(ConfigSection item : sec.getListFiltered("items", ConfigSection.class)) {
                items.add(parseItem(item));
            }
        }

        if(sec.has("equipment", ConfigSection.class)) {
            ConfigSection equipment = sec.getSection("equipment");
            for(String s : equipment.getKeys()) {
                if(!equipment.has(s, ConfigSection.class)) continue;
                this.equipment.put(s, parseItem(equipment.getSection(s)));
            }
        }

        if(sec.has("equivalencies", ConfigSection.class)) {
            for(HashMap.Entry<String, Object> ent : sec.get("equivalencies", ConfigSection.class).getEntries().entrySet()) {
                if(!(ent.getValue() instanceof String)) continue;

                for(PositionType t : PositionType.values()) {
                    if(t.getId().equals(ent.getKey())) {
                        equivalencies.put(t, (String) ent.getValue());
                    }
                }
            }
        }

        if(sec.has("commands", ConfigSection.class)) {
            ConfigSection cmds = sec.getSection("commands");
            for(String s : cmds.getKeys()) {
                CommandActivationPoint act = CommandActivationPoint.byId(s);
                if(act == null || !cmds.has(s, List.class)) continue;

                commands.put(act, cmds.getStringList(s));
            }
        }

        if(sec.has("taggable", Boolean.class)) {
            taggable = sec.getBoolean("taggable");
        }

        if(sec.has("permission", String.class)) {
            permission = sec.getString("permission");
        }
    }

    private static MItemStack parseItem(ConfigSection sec) {
        if(sec.has("midnight_item", String.class)) {

            return MidnightItemsIntegration.getItem(MIdentifier.parse(sec.getString("midnight_item")));
        }
        return MItemStack.SERIALIZER.deserialize(sec);
    }

    public enum CommandActivationPoint {

        SETUP("setup"),
        TAGGED("tagged"),
        TAGGED_ENVIRONMENT("tagged_environment"),
        TAG("tag"),
        END("end");

        String id;
        CommandActivationPoint(String id) {
            this.id = id;
        }

        static CommandActivationPoint byId(String id) {
            for(CommandActivationPoint act : values()) {
                if(id.equals(act.id)) return act;
            }
            return null;
        }

    }

}
