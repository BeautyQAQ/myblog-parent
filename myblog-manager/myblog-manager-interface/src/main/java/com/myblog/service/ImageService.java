package com.myblog.service;

import com.myblog.pojo.TbImage;

public interface ImageService {
    int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    String saveImage(byte[] data);

    TbImage getImage(String id);
}
