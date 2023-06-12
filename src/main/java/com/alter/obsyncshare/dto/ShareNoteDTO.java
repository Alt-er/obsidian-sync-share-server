package com.alter.obsyncshare.dto;

public class ShareNoteDTO {

    private String mainPath;
    private String title;

    public String getMainPath() {
        return mainPath;
    }

    public void setMainPath(String mainPath) {
        this.mainPath = mainPath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "ShareNoteDTO{" +
                "mainPath='" + mainPath + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
