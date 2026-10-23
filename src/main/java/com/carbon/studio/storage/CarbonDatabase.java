package com.carbon.studio.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** SQLite lifecycle and explicit, transactional schema migrations. Artwork pixels stay in .carbon files. */
public final class CarbonDatabase implements AutoCloseable {
    public static final int SCHEMA_VERSION=2;
    private final Connection connection;
    private CarbonDatabase(Connection connection){this.connection=connection;}
    public static CarbonDatabase open(Path path)throws SQLException,IOException{
        Path absolute=path.toAbsolutePath().normalize();if(absolute.getParent()!=null)Files.createDirectories(absolute.getParent());
        try{Class.forName("org.sqlite.JDBC");}catch(ClassNotFoundException e){throw new SQLException("SQLite JDBC driver is missing. Check the xerial runtime dependency.",e);}
        Connection c=DriverManager.getConnection("jdbc:sqlite:"+absolute);CarbonDatabase db=new CarbonDatabase(c);
        try{db.configure();db.migrate();return db;}catch(Exception e){try{c.close();}catch(SQLException ignored){}throw e;}
    }
    private void configure()throws SQLException{
        try(Statement s=connection.createStatement()){
            s.execute("PRAGMA foreign_keys=ON");s.execute("PRAGMA busy_timeout=5000");s.execute("PRAGMA synchronous=NORMAL");s.execute("PRAGMA journal_mode=WAL");
        }
    }
    private void migrate()throws SQLException{
        int version=userVersion();if(version>SCHEMA_VERSION)throw new SQLException("Database schema v"+version+" is newer than this application supports (v"+SCHEMA_VERSION+").");
        if(version==SCHEMA_VERSION)return;boolean auto=connection.getAutoCommit();connection.setAutoCommit(false);
        try{
            if(version<1){
                try(Statement s=connection.createStatement()){
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS schema_migrations(version INTEGER PRIMARY KEY,applied_at INTEGER NOT NULL)");
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY,value_json TEXT NOT NULL,updated_at INTEGER NOT NULL)");
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS palettes(id TEXT PRIMARY KEY,name TEXT NOT NULL,colors_json TEXT NOT NULL,updated_at INTEGER NOT NULL)");
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS brush_packs(id TEXT PRIMARY KEY,name TEXT NOT NULL,category TEXT NOT NULL,version INTEGER NOT NULL,payload_json TEXT NOT NULL,installed_at INTEGER NOT NULL)");
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS projects(id TEXT PRIMARY KEY,uuid TEXT NOT NULL UNIQUE,name TEXT NOT NULL,path TEXT NOT NULL UNIQUE,thumbnail_path TEXT,last_opened INTEGER NOT NULL,modified INTEGER NOT NULL,bytes INTEGER NOT NULL DEFAULT 0,deleted INTEGER NOT NULL DEFAULT 0)");
                    s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_projects_recent ON projects(deleted,last_opened DESC)");
                    s.executeUpdate("INSERT OR IGNORE INTO schema_migrations(version,applied_at) VALUES(1,"+System.currentTimeMillis()+")");s.execute("PRAGMA user_version=1");
                }
            }
            if(version<2){
                try(Statement s=connection.createStatement()){
                    s.executeUpdate("CREATE TABLE IF NOT EXISTS themes(id TEXT PRIMARY KEY,name TEXT NOT NULL,category TEXT NOT NULL,mode TEXT NOT NULL,payload_json TEXT NOT NULL,updated_at INTEGER NOT NULL)");
                    s.executeUpdate("INSERT OR IGNORE INTO schema_migrations(version,applied_at) VALUES(2,"+System.currentTimeMillis()+")");s.execute("PRAGMA user_version=2");
                }
            }
            connection.commit();
        }catch(SQLException|RuntimeException e){try{connection.rollback();}catch(SQLException ignored){}throw e;}finally{connection.setAutoCommit(auto);}
    }
    public int userVersion()throws SQLException{try(Statement s=connection.createStatement();ResultSet r=s.executeQuery("PRAGMA user_version")){return r.next()?r.getInt(1):0;}}
    Connection connection()throws SQLException{if(!isOpen())throw new SQLException("Database is closed.");return connection;}
    public boolean isOpen(){try{return connection!=null&&!connection.isClosed();}catch(SQLException e){return false;}}
    public synchronized void transaction(SQLWork work)throws SQLException{
        Connection c=connection();boolean auto=c.getAutoCommit();c.setAutoCommit(false);try{work.run(c);c.commit();}catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException ignored){}throw e;}finally{c.setAutoCommit(auto);}
    }
    @Override public synchronized void close()throws SQLException{if(connection!=null)connection.close();}
    @FunctionalInterface public interface SQLWork{void run(Connection connection)throws SQLException;}
}
