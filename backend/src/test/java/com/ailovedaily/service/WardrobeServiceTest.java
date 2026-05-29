package com.ailovedaily.service;

import com.ailovedaily.entity.User;
import com.ailovedaily.entity.WardrobeItem;
import com.ailovedaily.mapper.UserMapper;
import com.ailovedaily.mapper.WardrobeItemMapper;
import com.ailovedaily.service.impl.WardrobeServiceImpl;
import com.ailovedaily.vo.AiRecognizeResultVO;
import com.ailovedaily.vo.WardrobeItemVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WardrobeService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class WardrobeServiceTest {

    @InjectMocks
    private WardrobeServiceImpl wardrobeService;

    @Mock
    private WardrobeItemMapper wardrobeItemMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private FileService fileService;

    @Mock
    private AiVisionService aiVisionService;

    @Test
    void testRecognizeAndSave() {
        // 准备数据
        Long userId = 1L;
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test image content".getBytes());

        User user = new User();
        user.setId(userId);
        user.setCoupleId(100L);

        String[] urls = {"/uploads/wardrobe/2026/05/27/abc.jpg", "/uploads/thumbnails/wardrobe/2026/05/27/abc_thumb.jpg"};

        AiRecognizeResultVO recognizeResult = new AiRecognizeResultVO();
        recognizeResult.setSuccess(true);
        recognizeResult.setCategory("top");
        recognizeResult.setSubType("T恤");
        recognizeResult.setColor("白色");
        recognizeResult.setStyle("简约");
        recognizeResult.setSeason(Arrays.asList("spring", "summer"));
        recognizeResult.setOccasion(Arrays.asList("daily"));
        recognizeResult.setTags(Arrays.asList("纯棉", "圆领"));

        // Mock 行为
        when(userMapper.selectById(userId)).thenReturn(user);
        when(fileService.uploadImageWithThumbnail(file, "wardrobe")).thenReturn(urls);
        when(aiVisionService.recognizeClothing(urls[0])).thenReturn(recognizeResult);
        when(wardrobeItemMapper.insert(any(WardrobeItem.class))).thenAnswer(invocation -> {
            WardrobeItem item = invocation.getArgument(0);
            item.setId(1L);
            return 1;
        });

        // 设置 aiEnabled = true
        setField(wardrobeService, "aiEnabled", true);

        // 执行
        WardrobeItemVO result = wardrobeService.upload(file, userId);

        // 验证
        assertNotNull(result);
        assertEquals("top", result.getCategoryCode());
        assertEquals("T恤", result.getSubType());
        assertEquals("白色", result.getColor());
        assertEquals("简约", result.getStyle());
        assertTrue(result.getAiRecognized());
        assertEquals(2, result.getSeason().size());
        assertEquals(1, result.getOccasion().size());
        assertEquals(2, result.getTags().size());

        verify(wardrobeItemMapper, times(1)).insert(any(WardrobeItem.class));
        verify(aiVisionService, times(1)).recognizeClothing(anyString());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
