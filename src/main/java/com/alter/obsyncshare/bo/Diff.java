package com.alter.obsyncshare.bo;

public class Diff {


    private DiffActionInfo clientDiffActionInfo;
    private DiffActionInfo serverDiffActionInfo;


    public Diff(DiffActionInfo clientDiffActionInfo, DiffActionInfo serverDiffActionInfo) {
        this.clientDiffActionInfo = clientDiffActionInfo;
        this.serverDiffActionInfo = serverDiffActionInfo;
    }

    public DiffActionInfo getClientDiffActionInfo() {
        return clientDiffActionInfo;
    }

    public DiffActionInfo getServerDiffActionInfo() {
        return serverDiffActionInfo;
    }
}
