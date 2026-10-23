package com.carbon.studio.storage;

import com.carbon.studio.settings.ThemeLibrary.ThemeDefinition;
import com.google.gson.Gson;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** JDBC custom-theme records introduced by schema migration 2. */
public class ThemeRepository {
    private final CarbonDatabase db;private final Gson gson=new Gson();
    public ThemeRepository(CarbonDatabase db){this.db=db;}
    public void save(ThemeDefinition theme)throws SQLException{try(PreparedStatement p=db.connection().prepareStatement("INSERT INTO themes(id,name,category,mode,payload_json,updated_at) VALUES(?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,category=excluded.category,mode=excluded.mode,payload_json=excluded.payload_json,updated_at=excluded.updated_at")){p.setString(1,theme.id());p.setString(2,theme.name());p.setString(3,theme.category());p.setString(4,theme.mode());p.setString(5,gson.toJson(theme));p.setLong(6,System.currentTimeMillis());p.executeUpdate();}}
    public List<ThemeDefinition>list()throws SQLException{List<ThemeDefinition>out=new ArrayList<>();try(PreparedStatement p=db.connection().prepareStatement("SELECT payload_json FROM themes ORDER BY name COLLATE NOCASE,id");ResultSet r=p.executeQuery()){while(r.next())out.add(gson.fromJson(r.getString(1),ThemeDefinition.class));}return out;}
    public void delete(String id)throws SQLException{try(PreparedStatement p=db.connection().prepareStatement("DELETE FROM themes WHERE id=?")){p.setString(1,id);p.executeUpdate();}}
}
