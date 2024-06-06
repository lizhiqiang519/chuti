package com.tencent.wxcloudrun.model;

import lombok.Data;

@Data
public class LinkUnhandleQuestion {
    private Long id;
    private String uuid;
    private String tishici;
    private String result;
    private String link;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTishici() {
        return tishici;
    }

    public void setTishici(String tishici) {
        this.tishici = tishici;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}