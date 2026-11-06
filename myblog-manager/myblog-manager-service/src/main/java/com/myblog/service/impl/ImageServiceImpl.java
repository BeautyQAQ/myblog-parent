package com.myblog.service.impl;

import com.myblog.mapper.TbImageMapper;
import com.myblog.pojo.TbImage;
import com.myblog.service.ImageService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ImageServiceImpl implements ImageService {
    private final TbImageMapper imageMapper;

    public ImageServiceImpl(TbImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    @Override
    public String saveImage(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("请选择图片");
        }
        if (data.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("图片不能超过 5MB");
        }
        TbImage image = new TbImage();
        image.setId(UUID.randomUUID().toString());
        image.setContentType(detectContentType(data));
        image.setData(data);
        image.setCreatedAt(Instant.now());
        if (imageMapper.insert(image) != 1) {
            throw new IllegalStateException("图片保存失败");
        }
        return image.getId();
    }

    @Override
    public TbImage getImage(String id) {
        if (id == null || id.length() != 36) {
            return null;
        }
        try {
            if (!UUID.fromString(id).toString().equals(id)) {
                return null;
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return imageMapper.selectByPrimaryKey(id);
    }

    private static String detectContentType(byte[] data) {
        if (matches(data, 0, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) {
            return "image/png";
        }
        if (matches(data, 0, 0xff, 0xd8, 0xff)) {
            return "image/jpeg";
        }
        if (matches(data, 0, 'G', 'I', 'F', '8', '7', 'a')
                || matches(data, 0, 'G', 'I', 'F', '8', '9', 'a')) {
            return "image/gif";
        }
        if (matches(data, 0, 'R', 'I', 'F', 'F') && matches(data, 8, 'W', 'E', 'B', 'P')) {
            return "image/webp";
        }
        throw new IllegalArgumentException("仅支持 PNG、JPEG、GIF、WebP 图片");
    }

    private static boolean matches(byte[] data, int offset, int... signature) {
        if (data.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[offset + i] & 0xff) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
