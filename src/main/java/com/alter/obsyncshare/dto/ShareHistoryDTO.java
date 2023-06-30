package com.alter.obsyncshare.dto;

import java.util.Date;

public class ShareHistoryDTO {

    private String uuid;

    private Date expirationDate;

    public ShareHistoryDTO(String uuid, Date expirationDate) {
        this.uuid = uuid;
        this.expirationDate = expirationDate;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }
}
