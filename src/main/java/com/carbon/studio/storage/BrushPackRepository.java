package com.carbon.studio.storage;

import com.carbon.studio.tools.BrushPresetLibrary;
import com.google.gson.Gson;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Installed brush packs are JSON metadata/definitions, never external code. */
public class BrushPackRepository {
    private final CarbonDatabase db;private final Gson gson=new Gson();
    public BrushPackRepository(CarbonDatabase db){this.db=db;}
    public void install(BrushPresetLibrary.PackDefinition pack)throws SQLException{String payload=gson.toJson(pack);try(PreparedStatement p=db.connection().prepareStatement("INSERT INTO brush_packs(id,name,category,version,payload_json,installed_at) VALUES(?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,category=excluded.category,version=excluded.version,payload_json=excluded.payload_json,installed_at=excluded.installed_at")){p.setString(1,pack.id());p.setString(2,pack.name());p.setString(3,pack.category());p.setInt(4,pack.version());p.setString(5,payload);p.setLong(6,System.currentTimeMillis());p.executeUpdate();}}
    public List<BrushPresetLibrary.PackDefinition>list()throws SQLException{List<BrushPresetLibrary.PackDefinition>out=new ArrayList<>();try(PreparedStatement p=db.connection().prepareStatement("SELECT payload_json FROM brush_packs ORDER BY name COLLATE NOCASE,id");ResultSet r=p.executeQuery()){while(r.next())out.add(gson.fromJson(r.getString(1),BrushPresetLibrary.PackDefinition.class));}return out;}
    public void remove(String id)throws SQLException{try(PreparedStatement p=db.connection().prepareStatement("DELETE FROM brush_packs WHERE id=?")){p.setString(1,id);p.executeUpdate();}}
}
