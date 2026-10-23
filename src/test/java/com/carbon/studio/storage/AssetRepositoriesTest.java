package com.carbon.studio.storage;

import static org.junit.Assert.*;
import com.carbon.studio.settings.ThemeBundleService;
import com.carbon.studio.settings.ThemeLibrary;
import com.carbon.studio.tools.BrushPresetLibrary;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AssetRepositoriesTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    @Test public void installedBrushPackSurvivesDatabaseReopen()throws Exception{var pack=new BrushPresetLibrary().packs().get(0);var file=temp.newFile("packs.db").toPath();try(CarbonDatabase db=CarbonDatabase.open(file)){new BrushPackRepository(db).install(pack);}try(CarbonDatabase db=CarbonDatabase.open(file)){BrushPackRepository repo=new BrushPackRepository(db);assertEquals(1,repo.list().size());assertEquals(pack.brushes().size(),repo.list().get(0).brushes().size());repo.remove(pack.id());assertTrue(repo.list().isEmpty());}}
    @Test public void themeBundleV2RoundTripValidatesTokensAndContrast(){ThemeLibrary.ThemeDefinition theme=new ThemeLibrary().byId("high-contrast-dark");ThemeBundleService service=new ThemeBundleService();String json=service.exportBundle(List.of(theme));var imported=service.importBundle(json);assertEquals(1,imported.size());assertEquals(25,imported.get(0).tokens().keySet().size());assertEquals("Custom",imported.get(0).category());assertTrue(service.importBundle(json).get(0).id().startsWith("custom-"));assertThrows(IllegalArgumentException.class,()->service.importBundle("{}"));}
    @Test public void customThemesUseSchemaMigrationTwo()throws Exception{ThemeLibrary.ThemeDefinition theme=new ThemeBundleService().importBundle(new ThemeBundleService().exportBundle(List.of(new ThemeLibrary().byId("high-contrast-dark")))).get(0);var file=temp.newFile("themes.db").toPath();try(CarbonDatabase db=CarbonDatabase.open(file)){assertEquals(2,db.userVersion());new ThemeRepository(db).save(theme);}try(CarbonDatabase db=CarbonDatabase.open(file)){ThemeRepository repo=new ThemeRepository(db);assertEquals(1,repo.list().size());assertEquals(theme.name(),repo.list().get(0).name());repo.delete(theme.id());assertTrue(repo.list().isEmpty());}}
}
