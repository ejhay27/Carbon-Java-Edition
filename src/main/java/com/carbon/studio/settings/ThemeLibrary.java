package com.carbon.studio.settings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Built-in UI-only Carbon themes. Artwork and document colors never consume these tokens. */
public class ThemeLibrary {
    private static final String RESOURCE="/com/carbon/studio/data/themes.json";
    private final List<ThemeDefinition> themes=new ArrayList<>();
    public ThemeLibrary(){
        try(InputStream in=ThemeLibrary.class.getResourceAsStream(RESOURCE)){
            if(in==null)throw new IllegalStateException("Missing theme library");JsonObject root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var e:root.getAsJsonArray("themes")){JsonObject o=e.getAsJsonObject();themes.add(new ThemeDefinition(o.get("id").getAsString(),o.get("name").getAsString(),o.get("category").getAsString(),o.get("mode").getAsString(),o.get("description").getAsString(),o.getAsJsonObject("tokens").deepCopy()));}
        }catch(Exception e){throw new IllegalStateException("Could not load built-in themes",e);}
    }
    public List<ThemeDefinition> themes(){return List.copyOf(themes);}
    public ThemeDefinition byId(String id){return themes.stream().filter(t->t.id().equals(id)).findFirst().orElse(themes.get(0));}
    public record ThemeDefinition(String id,String name,String category,String mode,String description,JsonObject tokens){public ThemeDefinition{tokens=tokens.deepCopy();}@Override public JsonObject tokens(){return tokens.deepCopy();}}
}
