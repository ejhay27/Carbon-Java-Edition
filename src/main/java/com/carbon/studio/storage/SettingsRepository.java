package com.carbon.studio.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Typed-at-the-boundary JSON settings stored with prepared statements. */
public class SettingsRepository {
    private final CarbonDatabase db;
    public SettingsRepository(CarbonDatabase db){this.db=db;}
    public Optional<String>get(String key)throws SQLException{try(PreparedStatement p=db.connection().prepareStatement("SELECT value_json FROM settings WHERE key=?")){p.setString(1,key);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(r.getString(1)):Optional.empty();}}}
    public String get(String key,String fallback)throws SQLException{return get(key).orElse(fallback);}
    public JsonElement getJson(String key,JsonElement fallback)throws SQLException{String value=get(key,null);if(value==null)return fallback;try{return JsonParser.parseString(value);}catch(Exception e){return fallback;}}
    public void set(String key,String json)throws SQLException{try(PreparedStatement p=db.connection().prepareStatement("INSERT INTO settings(key,value_json,updated_at) VALUES(?,?,?) ON CONFLICT(key) DO UPDATE SET value_json=excluded.value_json,updated_at=excluded.updated_at")){p.setString(1,key);p.setString(2,json);p.setLong(3,System.currentTimeMillis());p.executeUpdate();}}
    public void setAll(Map<String,String>values)throws SQLException{db.transaction(c->{try(PreparedStatement p=c.prepareStatement("INSERT INTO settings(key,value_json,updated_at) VALUES(?,?,?) ON CONFLICT(key) DO UPDATE SET value_json=excluded.value_json,updated_at=excluded.updated_at")){long now=System.currentTimeMillis();for(var e:values.entrySet()){p.setString(1,e.getKey());p.setString(2,e.getValue());p.setLong(3,now);p.addBatch();}p.executeBatch();}});}
    public Map<String,String>all()throws SQLException{Map<String,String>out=new LinkedHashMap<>();try(PreparedStatement p=db.connection().prepareStatement("SELECT key,value_json FROM settings ORDER BY key");ResultSet r=p.executeQuery()){while(r.next())out.put(r.getString(1),r.getString(2));}return out;}
    public void delete(String key)throws SQLException{try(PreparedStatement p=db.connection().prepareStatement("DELETE FROM settings WHERE key=?")){p.setString(1,key);p.executeUpdate();}}
}
