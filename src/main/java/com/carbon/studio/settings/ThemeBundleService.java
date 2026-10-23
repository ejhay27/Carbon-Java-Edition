package com.carbon.studio.settings;

import com.carbon.studio.settings.ThemeLibrary.ThemeDefinition;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Carbon Theme Studio v1/v2 JSON with strict fields, token validation and contrast checks. */
public class ThemeBundleService {
    public static final int MAX_BYTES=65_536,MAX_THEMES=50;
    public static final List<String>TOKENS=List.of("background","surface","surfaceRaised","surfaceSunken","canvasSurround","toolbar","popover","tooltip","text","muted","subtleText","disabledText","border","divider","accent","accentSecondary","onAccent","selection","selectionText","focus","interactiveHover","interactivePressed","success","warning","danger");
    private static final List<String>LEGACY=List.of("background","surface","surfaceRaised","text","muted","border","accent","onAccent","focus","success","warning","danger");
    public String exportBundle(List<ThemeDefinition>themes){JsonObject root=new JsonObject();root.addProperty("format","carbon-theme-studio");root.addProperty("version",2);JsonArray array=new JsonArray();for(ThemeDefinition theme:themes){JsonObject spec=new JsonObject();spec.addProperty("name",theme.name());spec.addProperty("description",theme.description());spec.addProperty("mode",theme.mode());spec.add("tokens",theme.tokens());array.add(spec);}root.add("themes",array);return new GsonBuilder().setPrettyPrinting().create().toJson(root);}
    public List<ThemeDefinition>importBundle(String json){
        if(json.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new IllegalArgumentException("Theme JSON must be smaller than 64 KiB.");JsonObject root;try{root=JsonParser.parseString(json).getAsJsonObject();}catch(Exception e){throw new IllegalArgumentException("The selected file is not valid JSON.",e);}if(!exact(root,Set.of("format","version","themes"))||!"carbon-theme-studio".equals(str(root,"format"))||!(root.get("version").getAsInt()==1||root.get("version").getAsInt()==2))throw new IllegalArgumentException("This is not a supported Carbon Theme Studio JSON file.");
        JsonArray input=root.getAsJsonArray("themes");if(input==null||input.isEmpty()||input.size()>MAX_THEMES)throw new IllegalArgumentException("A theme file must contain between 1 and 50 themes.");List<ThemeDefinition>out=new ArrayList<>();Set<String>ids=new HashSet<>();for(int i=0;i<input.size();i++){
            JsonObject spec=input.get(i).getAsJsonObject();if(!(spec.keySet().equals(Set.of("name","mode","tokens"))||spec.keySet().equals(Set.of("name","description","mode","tokens"))))throw new IllegalArgumentException("Theme "+(i+1)+": unsupported or missing fields.");String name=plain(spec.get("name").getAsString(),40),description=spec.has("description")?plain(spec.get("description").getAsString(),120):"",mode=str(spec,"mode");if(name.isBlank())throw new IllegalArgumentException("Theme name is required.");if(!mode.equals("dark")&&!mode.equals("light"))throw new IllegalArgumentException("Theme mode must be dark or light.");JsonObject tokens=tokens(spec.getAsJsonObject("tokens"));validateContrast(tokens);String id="custom-"+UUID.randomUUID().toString().replace("-","").substring(0,16);while(!ids.add(id))id="custom-"+UUID.randomUUID().toString().replace("-","").substring(0,16);out.add(new ThemeDefinition(id,name,"Custom",mode,description,tokens));
        }return out;
    }
    private static JsonObject tokens(JsonObject value){if(value==null)throw new IllegalArgumentException("Theme colors are missing.");JsonObject out=new JsonObject();if(exact(value,new HashSet<>(LEGACY))){for(String key:LEGACY)out.addProperty(key,color(value,key));out.addProperty("surfaceSunken",str(out,"background"));out.addProperty("canvasSurround",str(out,"background"));out.addProperty("toolbar",str(out,"surface"));out.addProperty("popover",str(out,"surfaceRaised"));out.addProperty("tooltip",str(out,"surface"));out.addProperty("subtleText",str(out,"muted"));out.addProperty("disabledText",str(out,"muted"));out.addProperty("divider",str(out,"border"));out.addProperty("accentSecondary",str(out,"focus"));out.addProperty("selection",str(out,"accent"));out.addProperty("selectionText",str(out,"onAccent"));out.addProperty("interactiveHover",str(out,"surfaceRaised"));out.addProperty("interactivePressed",str(out,"background"));return out;}
        if(!exact(value,new HashSet<>(TOKENS)))throw new IllegalArgumentException("Theme colors must contain exactly the supported token set.");for(String key:TOKENS)out.addProperty(key,color(value,key));return out;
    }
    private static void validateContrast(JsonObject t){String[]surfaces={"background","surface","surfaceRaised","surfaceSunken","canvasSurround","toolbar","popover","tooltip"};for(String text:new String[]{"text","muted"})for(String surface:surfaces)if(ratio(str(t,text),str(t,surface))<4.5)throw new IllegalArgumentException(text+" needs at least 4.5:1 contrast on all interface surfaces.");for(String text:new String[]{"subtleText","disabledText"})for(String surface:surfaces)if(ratio(str(t,text),str(t,surface))<3)throw new IllegalArgumentException(text+" needs at least 3:1 contrast.");if(ratio(str(t,"onAccent"),str(t,"accent"))<4.5||ratio(str(t,"selectionText"),str(t,"selection"))<4.5)throw new IllegalArgumentException("Text on accent and selection fills needs at least 4.5:1 contrast.");for(String key:new String[]{"accent","selection","focus","accentSecondary","success","warning","danger"})for(String surface:surfaces)if(ratio(str(t,key),str(t,surface))<3)throw new IllegalArgumentException(key+" needs at least 3:1 contrast on every interface surface.");for(String key:new String[]{"border","divider"})for(String surface:surfaces)if(ratio(str(t,key),str(t,surface))<1.5)throw new IllegalArgumentException(key+" needs at least 1.5:1 contrast on every interface surface.");}
    public static double ratio(String first,String second){double a=luminance(first),b=luminance(second),light=Math.max(a,b),dark=Math.min(a,b);return(light+.05)/(dark+.05);}
    private static double luminance(String hex){int r=Integer.parseInt(hex.substring(1,3),16),g=Integer.parseInt(hex.substring(3,5),16),b=Integer.parseInt(hex.substring(5,7),16);return .2126*channel(r)+.7152*channel(g)+.0722*channel(b);}
    private static double channel(int value){double c=value/255.0;return c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4);}
    private static String color(JsonObject o,String key){if(!o.has(key))throw new IllegalArgumentException("Missing theme color: "+key);String value=o.get(key).getAsString();if(!value.matches("^#[0-9a-fA-F]{6}$"))throw new IllegalArgumentException(key+" must be a six-digit HEX color.");return value.toLowerCase();}
    private static String plain(String value,int max){if(value==null||value.length()>max||value.chars().anyMatch(c->c=='<'||c<32||c==127))throw new IllegalArgumentException("Theme text must be plain text within the length limit.");return value.trim();}
    private static boolean exact(JsonObject o,Set<String>keys){Set<String>actual=o.keySet();return actual.equals(keys);}
    private static String str(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
}
