package com.alter.obsyncshare.dto;

public class GitConfigDTO {

    private boolean syncToLocalGit = false;
    private int maximumCommitFileSize = 1;
    private String remoteGitAddress = "";
    private String remoteGitUsername = "";
    private String remoteGitAccessToken = "";

    public boolean isSyncToLocalGit() {
        return syncToLocalGit;
    }

    public void setSyncToLocalGit(boolean syncToLocalGit) {
        this.syncToLocalGit = syncToLocalGit;
    }

    public int getMaximumCommitFileSize() {
        return maximumCommitFileSize;
    }

    public void setMaximumCommitFileSize(int maximumCommitFileSize) {
        this.maximumCommitFileSize = maximumCommitFileSize;
    }

    public String getRemoteGitAddress() {
        return remoteGitAddress;
    }

    public void setRemoteGitAddress(String remoteGitAddress) {
        this.remoteGitAddress = remoteGitAddress;
    }

    public String getRemoteGitUsername() {
        return remoteGitUsername;
    }

    public void setRemoteGitUsername(String remoteGitUsername) {
        this.remoteGitUsername = remoteGitUsername;
    }

    public String getRemoteGitAccessToken() {
        return remoteGitAccessToken;
    }

    public void setRemoteGitAccessToken(String remoteGitAccessToken) {
        this.remoteGitAccessToken = remoteGitAccessToken;
    }

    @Override
    public String toString() {
        return "GitConfigDTO{" +
                "syncToLocalGit=" + syncToLocalGit +
                ", maximumCommitFileSize=" + maximumCommitFileSize +
                ", remoteGitAddress='" + remoteGitAddress + '\'' +
                ", remoteGitUsername='" + remoteGitUsername + '\'' +
                ", remoteGitAccessToken='" + remoteGitAccessToken + '\'' +
                '}';
    }
}
