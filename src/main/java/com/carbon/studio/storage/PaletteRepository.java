package com.carbon.studio.storage;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** User palettes are metadata records; no artwork or stroke pixels are stored in SQLite. */
public class PaletteRepository {
    private final CarbonDatabase db;private final Gson gson=new Gson();
    public PaletteRepository(CarbonDatabase db){this.db=db;}
    public PaletteRecord save(String id,String name,List<String>colors)throws SQLException{
        if(name==null||name.isBlank())throw new IllegalArgumentException("Palette name is required.");if(colors.size()>512)throw new IllegalArgumentException("A palette cannot exceed 512 colors.");
        List<String>checked=new ArrayList<>();for(String color:colors){if(color==null||!color.matches("^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$"))throw new IllegalArgumentException("Invalid palette color: "+color);checked.add(color);}
        String actual=id==null||id.isBlank()?UUID.randomUUID().toString():id;long now=System.currentTimeMillis();String json=gson.toJson(checked);
        try(PreparedStatement p=db.connection().prepareStatement("INSERT INTO palettes(id,name,colors_json,updated_at) VALUES(?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,colors_json=excluded.colors_json,updated_at=excluded.updated_at")){p.setString(1,actual);p.setString(2,name);p.setString(3,json);p.setLong(4,now);p.executeUpdate();}return new PaletteRecord(actual,name,checked,now);
    }
    public List<PaletteRecord>list()throws SQLException{List<PaletteRecord>out=new ArrayList<>();try(PreparedStatement p=db.connection().prepareStatement("SELECT id,name,colors_json,updated_at FROM palettes ORDER BY name COLLATE NOCASE,id");ResultSet r=p.executeQuery()){while(r.next())out.add(read(r));}return out;}
    public void delete(String id)throws SQLException{try(PreparedStatement p=db.connection().prepareStatement("DELETE FROM palettes WHERE id=?")){p.setString(1,id);p.executeUpdate();}}
    private PaletteRecord read(ResultSet r)throws SQLException{List<String>colors=new ArrayList<>();for(var e:JsonParser.parseString(r.getString(3)).getAsJsonArray())colors.add(e.getAsString());return new PaletteRecord(r.getString(1),r.getString(2),List.copyOf(colors),r.getLong(4));}
    public record PaletteRecord(String id,String name,List<String>colors,long updatedAt){public PaletteRecord{colors=List.copyOf(colors);}}
}
