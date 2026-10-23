package com.carbon.studio.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Recents/Library metadata and file references. Project pixels remain in .carbon files. */
public class ProjectIndexRepository {
    private final CarbonDatabase db;
    public ProjectIndexRepository(CarbonDatabase db){this.db=db;}
    public ProjectRecord upsert(String uuid,String name,String path,String thumbnailPath,long modified,long bytes)throws SQLException{
        String id="prj-"+UUID.nameUUIDFromBytes(uuid.getBytes(java.nio.charset.StandardCharsets.UTF_8));long now=System.currentTimeMillis();
        try(PreparedStatement p=db.connection().prepareStatement("INSERT INTO projects(id,uuid,name,path,thumbnail_path,last_opened,modified,bytes,deleted) VALUES(?,?,?,?,?,?,?,?,0) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name,path=excluded.path,thumbnail_path=excluded.thumbnail_path,last_opened=excluded.last_opened,modified=excluded.modified,bytes=excluded.bytes,deleted=0")){p.setString(1,id);p.setString(2,uuid);p.setString(3,name);p.setString(4,path);p.setString(5,thumbnailPath);p.setLong(6,now);p.setLong(7,modified);p.setLong(8,bytes);p.executeUpdate();}
        return new ProjectRecord(id,uuid,name,path,thumbnailPath,now,modified,bytes,false);
    }
    public List<ProjectRecord>recent(int limit)throws SQLException{List<ProjectRecord>out=new ArrayList<>();try(PreparedStatement p=db.connection().prepareStatement("SELECT id,uuid,name,path,thumbnail_path,last_opened,modified,bytes,deleted FROM projects WHERE deleted=0 ORDER BY last_opened DESC LIMIT ?")){p.setInt(1,Math.max(1,Math.min(500,limit)));try(ResultSet r=p.executeQuery()){while(r.next())out.add(read(r));}}return out;}
    public Optional<ProjectRecord>byPath(String path)throws SQLException{try(PreparedStatement p=db.connection().prepareStatement("SELECT id,uuid,name,path,thumbnail_path,last_opened,modified,bytes,deleted FROM projects WHERE path=? AND deleted=0")){p.setString(1,path);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}}}
    public void rename(String id,String name)throws SQLException{if(name==null||name.isBlank())throw new IllegalArgumentException("Project name is required");try(PreparedStatement p=db.connection().prepareStatement("UPDATE projects SET name=? WHERE id=?")){p.setString(1,name);p.setString(2,id);p.executeUpdate();}}
    public void setDeleted(String id,boolean deleted)throws SQLException{try(PreparedStatement p=db.connection().prepareStatement("UPDATE projects SET deleted=? WHERE id=?")){p.setInt(1,deleted?1:0);p.setString(2,id);p.executeUpdate();}}
    private static ProjectRecord read(ResultSet r)throws SQLException{return new ProjectRecord(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getLong(6),r.getLong(7),r.getLong(8),r.getInt(9)!=0);}
    public record ProjectRecord(String id,String uuid,String name,String path,String thumbnailPath,long lastOpened,long modified,long bytes,boolean deleted){}
}
