package com.carbon.studio.tools;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class BrushPresetLibraryTest {
    @Test public void auditedBrushDataIsAvailableOffline(){BrushPresetLibrary lib=new BrushPresetLibrary();assertEquals(152,lib.builtins().size());assertEquals(19,lib.packs().size());assertEquals(396,lib.packs().stream().mapToInt(p->p.brushes().size()).sum());assertNotNull(lib.byId("pencil-hb"));assertNotNull(lib.defaultEraser());}
    @Test public void packRoundTripAndDuplicateDetectionWork(){BrushPresetLibrary lib=new BrushPresetLibrary();Brush b=lib.defaultBrush();b.setName("My HB");String json=lib.exportPack("Test",List.of(b),"test-pack","Custom");BrushPresetLibrary.ImportResult first=lib.importPack(json,List.of(),1_000_000);assertEquals(1,first.pack().brushes().size());assertThrows(IllegalArgumentException.class,()->lib.importPack(json,new ArrayList<>(first.pack().brushes()),1_000_000));}
    @Test(expected=IllegalArgumentException.class)public void duplicateOnlyPackIsRejected(){BrushPresetLibrary lib=new BrushPresetLibrary();Brush b=lib.defaultBrush();String json=lib.exportPack("Test",List.of(b),"test-pack","Custom");BrushPresetLibrary.ImportResult first=lib.importPack(json,List.of(),1_000_000);lib.importPack(json,new ArrayList<>(first.pack().brushes()),1_000_000);}
    @Test public void searchMatchesMultipleFields(){BrushPresetLibrary lib=new BrushPresetLibrary();assertFalse(lib.search(lib.builtins(),"pencil graphite").isEmpty());}
}
