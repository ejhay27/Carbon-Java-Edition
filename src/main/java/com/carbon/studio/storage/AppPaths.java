package com.carbon.studio.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Per-user application data. No project or database is written into the installation directory. */
public final class AppPaths {
    private final Path root;
    public AppPaths(){this(defaultRoot());}
    public AppPaths(Path root){this.root=root.toAbsolutePath().normalize();}
    public static Path defaultRoot(){
        String override=System.getProperty("carbon.dataDir");if(override!=null&&!override.isBlank())return Path.of(override);
        String os=System.getProperty("os.name","").toLowerCase();String appData=System.getenv("APPDATA");
        if(os.contains("win")&&appData!=null&&!appData.isBlank())return Path.of(appData,"Carbon Java Edition");
        String xdg=System.getenv("XDG_DATA_HOME");if(xdg!=null&&!xdg.isBlank())return Path.of(xdg,"carbon-java-edition");
        if(os.contains("mac"))return Path.of(System.getProperty("user.home"),"Library","Application Support","Carbon Java Edition");
        return Path.of(System.getProperty("user.home"),".local","share","carbon-java-edition");
    }
    public Path root(){return root;}
    public Path database(){return root.resolve("carbon.db");}
    public Path recovery(){return root.resolve("recovery");}
    public Path library(){return root.resolve("library");}
    public Path exports(){return root.resolve("exports");}
    public void initialize()throws IOException{Files.createDirectories(root);Files.createDirectories(recovery());Files.createDirectories(library());Files.createDirectories(exports());}
}
