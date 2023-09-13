package com.alter.obsyncshare.dto;

public class ShareNoteDTO {

    private String mainPath;
    private String title;
    private Long expirationDate;
    private String headerPosition;
    private String shareLinkId;

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

    public Long getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Long expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getHeaderPosition() {
        return headerPosition;
    }

    public void setHeaderPosition(String headerPosition) {
        this.headerPosition = headerPosition;
    }

    public String getShareLinkId() {
        return shareLinkId;
    }

    public void setShareLinkId(String shareLinkId) {
        this.shareLinkId = shareLinkId;
    }

    @Override
    public String toString() {
        return "ShareNoteDTO{" +
                "mainPath='" + mainPath + '\'' +
                ", title='" + title + '\'' +
                ", expirationDate=" + expirationDate +
                ", headerPosition='" + headerPosition + '\'' +
                ", shareLinkId='" + shareLinkId + '\'' +
                '}';
    }
}
