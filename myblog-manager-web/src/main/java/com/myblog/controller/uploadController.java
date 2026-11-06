package com.myblog.controller;

import com.myblog.common.utils.JsonUtils;
import com.myblog.pojo.TbImage;
import com.myblog.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Map;

@Controller
public class uploadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(uploadController.class);
    private static final CacheControl IMAGE_CACHE = CacheControl.maxAge(Duration.ofDays(365))
            .cachePublic().immutable();

    @Autowired
    private ImageService imageService;

    @Value("${image.public-base-url}")
    private String publicBaseUrl;

    @PostMapping(value = "/upload", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    @ResponseBody
    public String picture(@RequestParam(value = "picture", required = false) MultipartFile picture) {
        try {
            if (picture == null || picture.isEmpty()) {
                throw new IllegalArgumentException("请选择图片");
            }
            if (picture.getSize() > ImageService.MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("图片不能超过 5MB");
            }
            String imageId = imageService.saveImage(picture.getBytes());
            String url = publicBaseUrl.replaceAll("/+$", "") + "/images/" + imageId;
            return JsonUtils.objectToJson(Map.of("error", 0, "url", url));
        } catch (IllegalArgumentException e) {
            return JsonUtils.objectToJson(Map.of("error", 1, "message", e.getMessage()));
        } catch (Exception e) {
            LOGGER.error("图片上传失败", e);
            return JsonUtils.objectToJson(Map.of("error", 1, "message", "图片上传失败"));
        }
    }

    @GetMapping("/images/{imageId}")
    public ResponseEntity<byte[]> image(@PathVariable String imageId) {
        TbImage image = imageService.getImage(imageId);
        if (image == null) {
            return ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .contentLength(image.getData().length)
                .eTag(image.getId())
                .cacheControl(IMAGE_CACHE)
                .lastModified(image.getCreatedAt())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.getData());
    }
}
