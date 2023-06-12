package com.alter.obsyncshare.bo;

import java.util.Arrays;
import java.util.stream.Collectors;

public class FileInfo {

    /**
     * 路径
     */
    private String path;

    /**
     * 修改时间
     */
    private long mtime;

    public FileInfo(String path, long mtime) {

        String[] split = path.split("[\\\\/]");
        this.path = Arrays.stream(split).filter(s -> !s.isBlank() && !s.equals(".") && !s.equals("..")).collect(Collectors.joining("/"));
        this.mtime = mtime;
    }

    public String getPath() {
        return path;
    }

    public long getMtime() {
        return mtime;
    }


    @Override
    public String toString() {
        return "FileInfo{" +
                "path='" + path + '\'' +
                ", mtime=" + mtime +
                '}';
    }
}
