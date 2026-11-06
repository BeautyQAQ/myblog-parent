package com.myblog.service.impl;

import com.myblog.mapper.TbImageMapper;
import com.myblog.pojo.TbImage;
import com.myblog.service.ImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageServiceImplTest {
    private final RecordingMapper mapper = new RecordingMapper();
    private final ImageService service = new ImageServiceImpl(mapper);

    @ParameterizedTest
    @MethodSource("rasterSignatures")
    void storesTheDetectedMimeTypeAndGeneratedUuid(byte[] data, String expectedType) {
        String id = service.saveImage(data);
        TbImage image = service.getImage(id);

        assertEquals(id, UUID.fromString(id).toString());
        assertEquals(expectedType, image.getContentType());
        assertArrayEquals(data, image.getData());
        assertNotNull(image.getCreatedAt());
    }

    @Test
    void rejectsEmptyUnsupportedAndOversizedFilesWithoutWritingToTheDatabase() {
        assertThrows(IllegalArgumentException.class, () -> service.saveImage(null));
        assertThrows(IllegalArgumentException.class, () -> service.saveImage(new byte[0]));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveImage("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveImage(new byte[ImageService.MAX_IMAGE_BYTES + 1]));
        assertEquals(0, mapper.images.size());
    }

    @Test
    void acceptsAFileAtTheFiveMebibyteLimit() {
        byte[] data = Arrays.copyOf(new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff}, ImageService.MAX_IMAGE_BYTES);
        String id = service.saveImage(data);
        assertEquals(ImageService.MAX_IMAGE_BYTES, service.getImage(id).getData().length);
    }

    @Test
    void returnsNullForUnknownImagesAndRejectsNonCanonicalIdsBeforeQuerying() {
        assertNull(service.getImage(UUID.randomUUID().toString()));
        int previousReads = mapper.reads;
        assertNull(service.getImage("../../etc/passwd"));
        assertNull(service.getImage("1-1-1-1-1"));
        assertNull(service.getImage(null));
        assertEquals(previousReads, mapper.reads);
    }

    private static Stream<Arguments> rasterSignatures() {
        return Stream.of(
                Arguments.of(new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10}, "image/png"),
                Arguments.of(new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0}, "image/jpeg"),
                Arguments.of("GIF87a".getBytes(StandardCharsets.US_ASCII), "image/gif"),
                Arguments.of("GIF89a".getBytes(StandardCharsets.US_ASCII), "image/gif"),
                Arguments.of(new byte[]{'R', 'I', 'F', 'F', 4, 0, 0, 0, 'W', 'E', 'B', 'P'}, "image/webp"));
    }

    private static class RecordingMapper implements TbImageMapper {
        private final Map<String, TbImage> images = new HashMap<>();
        private int reads;

        @Override
        public int insert(TbImage image) {
            images.put(image.getId(), image);
            return 1;
        }

        @Override
        public TbImage selectByPrimaryKey(String id) {
            reads++;
            return images.get(id);
        }
    }
}
