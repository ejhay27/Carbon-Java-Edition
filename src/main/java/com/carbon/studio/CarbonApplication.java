package com.carbon.studio;

import com.carbon.studio.storage.AppPaths;
import com.carbon.studio.storage.CarbonDatabase;
import com.carbon.studio.ui.StudioController;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/** JavaFX process entry point. No browser, Node, TypeScript or Electron runtime is embedded. */
public class CarbonApplication extends Application {
    private StudioController controller;
    @Override public void start(Stage stage){
        AppPaths paths=new AppPaths();CarbonDatabase database=null;Exception startupError=null;
        try{paths.initialize();database=CarbonDatabase.open(paths.database());}catch(Exception e){startupError=e;}
        controller=new StudioController(stage,paths,database);stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/com/carbon/studio/ui/carbon.png")));stage.setScene(controller.scene());stage.setMinWidth(980);stage.setMinHeight(650);stage.setTitle("Carbon: Java Edition");stage.setOnCloseRequest(e->{if(!controller.closeRequested())e.consume();});stage.show();
        if(startupError!=null){Alert alert=new Alert(Alert.AlertType.WARNING,"Local settings/database could not be opened: "+startupError.getMessage()+"\n\nDrawing and .carbon files remain available, but settings and recents will not persist.");alert.setTitle("Local storage warning");alert.setHeaderText("SQLite initialization failed");alert.setResizable(true);alert.showAndWait();}
    }
    @Override public void stop(){if(controller!=null)controller.close();}
}
