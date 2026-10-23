package com.carbon.studio.storage;

import static org.junit.Assert.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DatabaseTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    @Test public void migrationsAndSettingsSurviveReopen()throws Exception{
        Path file=temp.getRoot().toPath().resolve("data/carbon.db");try(CarbonDatabase db=CarbonDatabase.open(file)){assertEquals(1,db.userVersion());SettingsRepository settings=new SettingsRepository(db);settings.set("theme","\"aurora\"");settings.setAll(Map.of("autosaveSeconds","60","maxUndoBytes","536870912"));assertEquals("\"aurora\"",settings.get("theme",null));}
        try(CarbonDatabase db=CarbonDatabase.open(file)){SettingsRepository settings=new SettingsRepository(db);assertEquals(3,settings.all().size());assertEquals("60",settings.get("autosaveSeconds",null));assertEquals(1,db.userVersion());}
    }
    @Test public void palettesUseParametersAndPersistArbitraryNames()throws Exception{
        Path file=temp.newFile("palette.db").toPath();try(CarbonDatabase db=CarbonDatabase.open(file)){PaletteRepository repo=new PaletteRepository(db);String hostile="Green'); DROP TABLE settings;--";repo.save("p1",hostile,List.of("#112233","#aabbcc80"));repo.save("p2","Second",List.of("#ffffff"));List<PaletteRepository.PaletteRecord>all=repo.list();assertEquals(2,all.size());assertEquals(2,repo.list().stream().filter(p->p.id().equals("p1")).findFirst().orElseThrow().colors().size());repo.delete("p2");assertEquals(1,repo.list().size());}
    }
    @Test public void schemaContainsMetadataNotPixelOrStrokeRows()throws Exception{
        try(CarbonDatabase db=CarbonDatabase.open(temp.newFile("schema.db").toPath())){var tables=new java.util.ArrayList<String>();try(var p=db.connection().prepareStatement("SELECT name FROM sqlite_master WHERE type='table'");var r=p.executeQuery()){while(r.next())tables.add(r.getString(1));}assertTrue(tables.contains("projects"));assertTrue(tables.contains("settings"));assertFalse(tables.stream().anyMatch(n->n.toLowerCase().contains("pixel")||n.toLowerCase().contains("stroke")));}
    }
}
