package com.myblog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.pojo.TbImage;
import com.myblog.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ImageControllerTest {
    private static final String IMAGE_ID = "1032cf0e-89ab-4b9e-8c0f-f27b84de65bc";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StubImageService service = new StubImageService();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        uploadController controller = new uploadController();
        ReflectionTestUtils.setField(controller, "imageService", service);
        ReflectionTestUtils.setField(controller, "publicBaseUrl", "https://blog.example.test/");
        mockMvc = standaloneSetup(controller).build();
    }

    @Test
    void uploadsTheBytesAndReturnsTheLegacyResponseUsingTheGeneratedId() throws Exception {
        MockMultipartFile file = new MockMultipartFile("picture", "../../original.txt", "text/html", PNG);
        MockHttpServletResponse response = mockMvc.perform(multipart("/upload").file(file))
                .andReturn().getResponse();

        JsonNode result = objectMapper.readTree(response.getContentAsString());
        assertEquals(200, response.getStatus());
        assertEquals(0, result.path("error").asInt());
        assertEquals("https://blog.example.test/images/" + IMAGE_ID, result.path("url").asText());
        assertArrayEquals(PNG, service.uploadedBytes);
    }

    @Test
    void rejectsMissingAndOversizedUploadsBeforeCallingTheService() throws Exception {
        MockHttpServletResponse missing = mockMvc.perform(multipart("/upload")).andReturn().getResponse();
        assertEquals(1, objectMapper.readTree(missing.getContentAsString()).path("error").asInt());

        MockMultipartFile tooLarge = new MockMultipartFile("picture", "large.png", "image/png",
                new byte[ImageService.MAX_IMAGE_BYTES + 1]);
        MockHttpServletResponse oversized = mockMvc.perform(multipart("/upload").file(tooLarge))
                .andReturn().getResponse();
        assertEquals(1, objectMapper.readTree(oversized.getContentAsString()).path("error").asInt());
        assertNull(service.uploadedBytes);
    }

    @Test
    void exposesTheImageWithMimeTypeAndSupportsConditionalRequests() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/images/" + IMAGE_ID)).andReturn().getResponse();

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertArrayEquals(PNG, response.getContentAsByteArray());
        assertTrue(response.getHeader(HttpHeaders.CACHE_CONTROL).contains("immutable"));
        String etag = response.getHeader(HttpHeaders.ETAG);
        assertNotNull(etag);

        MockHttpServletResponse cached = mockMvc.perform(get("/images/" + IMAGE_ID)
                        .header(HttpHeaders.IF_NONE_MATCH, etag))
                .andReturn().getResponse();
        assertEquals(304, cached.getStatus());
        assertEquals(0, cached.getContentAsByteArray().length);
    }

    @Test
    void returns404ForAnUnknownImage() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/images/unknown")).andReturn().getResponse();
        assertEquals(404, response.getStatus());
    }

    private static class StubImageService implements ImageService {
        private byte[] uploadedBytes;

        @Override
        public String saveImage(byte[] data) {
            uploadedBytes = data;
            return IMAGE_ID;
        }

        @Override
        public TbImage getImage(String id) {
            if (!IMAGE_ID.equals(id)) {
                return null;
            }
            TbImage image = new TbImage();
            image.setId(IMAGE_ID);
            image.setContentType("image/png");
            image.setData(PNG);
            image.setCreatedAt(Instant.parse("2020-01-01T00:00:00Z"));
            return image;
        }
    }
}
