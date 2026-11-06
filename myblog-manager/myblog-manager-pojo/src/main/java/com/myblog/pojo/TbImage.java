package com.myblog.pojo;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

public class TbImage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String contentType;
    private byte[] data;
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
