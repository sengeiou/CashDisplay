package com.resonance.cashdisplay.slide_show;

public class Slide {
    private String pathToImgFile;

    public Slide() {
    }

    public Slide(String pathTofile) {
        this.pathToImgFile = pathTofile;
    }

    public String getPathToImgFile() {
        return this.pathToImgFile;
    }

    public void setPathToImgFile(String pathTofile) {
        this.pathToImgFile = pathTofile;
    }

}