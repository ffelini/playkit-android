package com.kaltura.playkit;

public class PKMediaSource {
    private String id;
    private String url;
    private String mimeType;
    private DRMData drmData;

    public String getId() {
        return id;
    }

    public PKMediaSource setId(String id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public PKMediaSource setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public PKMediaSource setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public DRMData getDrmData() {
        return drmData;
    }

    public PKMediaSource setDrmData(DRMData drmData) {
        this.drmData = drmData;
        return this;
    }
}
