package com.alter.obsyncshare.bo;

import java.util.ArrayList;
import java.util.List;

public class DiffActionInfo {
    private List<String> uploadFiles = new ArrayList<>();
    private List<String> downloadFiles = new ArrayList<>();
    private List<String> deleteFiles = new ArrayList<>();
    private List<String> deleteDeleteHistorys = new ArrayList<>();
    private List<String> uploadDeleteHistorys = new ArrayList<>();

    public List<String> getUploadFiles() {
        return uploadFiles;
    }

    public List<String> getDownloadFiles() {
        return downloadFiles;
    }

    public List<String> getDeleteFiles() {
        return deleteFiles;
    }

    public List<String> getDeleteDeleteHistorys() {
        return deleteDeleteHistorys;
    }

    public List<String> getUploadDeleteHistorys() {
        return uploadDeleteHistorys;
    }
}
