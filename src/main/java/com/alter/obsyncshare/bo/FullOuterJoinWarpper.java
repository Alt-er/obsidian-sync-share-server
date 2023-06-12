package com.alter.obsyncshare.bo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FullOuterJoinWarpper {

    private String path;
    private FileInfo clientFileInfo;
    private FileInfo serverFileInfo;
    private DeleteHistory clientDeleteHistory;
    private DeleteHistory serverDeleteHistory;


    public FullOuterJoinWarpper(String path , FileInfo clientFileInfo, FileInfo serverFileInfo, DeleteHistory clientDeleteHistory, DeleteHistory serverDeleteHistory) {
        this.path = path;
        this.clientFileInfo = clientFileInfo;
        this.serverFileInfo = serverFileInfo;
        this.clientDeleteHistory = clientDeleteHistory;
        this.serverDeleteHistory = serverDeleteHistory;
    }


    public FileInfo getClientFileInfo() {
        return clientFileInfo;
    }

    public FileInfo getServerFileInfo() {
        return serverFileInfo;
    }

    public DeleteHistory getClientDeleteHistory() {
        return clientDeleteHistory;
    }

    public DeleteHistory getServerDeleteHistory() {
        return serverDeleteHistory;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {

        List<String> list = new ArrayList<>();
        list.add(clientFileInfo == null ? "" : clientFileInfo.getPath() + "<br>" + clientFileInfo.getMtime());
        list.add(serverFileInfo == null ? "" : serverFileInfo.getPath() + "<br>" + serverFileInfo.getMtime());
        list.add(clientDeleteHistory == null ? "" : clientDeleteHistory.getPath() + "<br>" + clientDeleteHistory.getDeleteTime());
        list.add(serverDeleteHistory == null ? "" : serverDeleteHistory.getPath() + "<br>" + serverDeleteHistory.getDeleteTime());

        String collect = list.stream().collect(Collectors.joining(" | "));
        return "| " + collect + " |";
//        return "FullOuterJoinWarpper{" +
//                "clientFileInfo=" + clientFileInfo +
//                ", serverFileInfo=" + serverFileInfo +
//                ", clientDeleteHistory=" + clientDeleteHistory +
//                ", serverDeleteHistory=" + serverDeleteHistory +
//                '}';
    }
}
