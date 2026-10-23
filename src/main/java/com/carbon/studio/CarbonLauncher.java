package com.carbon.studio;

/** Separate launcher avoids requiring JavaFX modules on the application class's superclass. */
public final class CarbonLauncher {
    private CarbonLauncher(){}
    public static void main(String[] args){javafx.application.Application.launch(CarbonApplication.class,args);}
}
