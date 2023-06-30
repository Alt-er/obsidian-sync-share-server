package com.alter.obsyncshare.dto;

import java.util.Date;

public class FileHistoryDTO {


    private String commitId;
    private String author;
    private Date time;
    private String message;
    private String path;


    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "FileHistoryDTO{" +
                "commitId='" + commitId + '\'' +
                ", author='" + author + '\'' +
                ", time=" + time +
                ", message='" + message + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
