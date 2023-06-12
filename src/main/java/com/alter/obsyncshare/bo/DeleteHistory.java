package com.alter.obsyncshare.bo;

import java.util.Arrays;
import java.util.stream.Collectors;

public class DeleteHistory {

    /**
     * 路径
     */
    private String path;

    /**
     * 删除时间
     */
    private long deleteTime;

    public DeleteHistory(String path, long deleteTime) {
        String[] split = path.split("[\\\\/]");
        this.path = Arrays.stream(split).filter(s -> !s.isBlank() && !s.equals(".") && !s.equals("..")).collect(Collectors.joining("/"));
        this.deleteTime = deleteTime;
    }

    public String getPath() {
        return path;
    }

    public long getDeleteTime() {
        return deleteTime;
    }

    @Override
    public String toString() {
        return "DeleteHistory{" +
                "path='" + path + '\'' +
                ", deleteTime=" + deleteTime +
                '}';
    }
}
