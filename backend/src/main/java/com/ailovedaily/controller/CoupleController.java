package com.ailovedaily.controller;

import com.ailovedaily.dto.CoupleBindDTO;
import com.ailovedaily.service.CoupleService;
import com.ailovedaily.service.FileService;
import com.ailovedaily.vo.LoveInfoVO;
import com.ailovedaily.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 情侣关系控制器
 */
@RestController
@RequestMapping("/api/couple")
@RequiredArgsConstructor
@Tag(name = "情侣关系", description = "情侣绑定、解绑相关接口")
public class CoupleController {

    private final CoupleService coupleService;
    private final FileService fileService;

    @PostMapping("/create")
    @Operation(summary = "创建情侣关系", description = "发起方创建情侣关系，生成邀请码")
    public ResultVO<Map<String, Object>> createCouple(@RequestAttribute("userId") Long userId,
                                                      @RequestBody CoupleBindDTO bindDTO) {
        Map<String, Object> result = coupleService.createCouple(userId, bindDTO);
        return ResultVO.success(result);
    }

    @PostMapping("/bind")
    @Operation(summary = "绑定情侣关系", description = "接受方通过邀请码绑定")
    public ResultVO<Void> bindCouple(@RequestAttribute("userId") Long userId,
                                     @RequestParam String inviteCode) {
        coupleService.bindCouple(userId, inviteCode);
        return ResultVO.success();
    }

    @GetMapping("/info")
    @Operation(summary = "获取恋爱信息", description = "获取恋爱天数、纪念日等信息")
    public ResultVO<LoveInfoVO> getLoveInfo(@RequestAttribute("coupleId") Long coupleId) {
        LoveInfoVO loveInfo = coupleService.getLoveInfo(coupleId);
        return ResultVO.success(loveInfo);
    }

    @PutMapping("/motto")
    @Operation(summary = "更新爱情宣言", description = "更新情侣的爱情宣言")
    public ResultVO<Void> updateMotto(@RequestAttribute("coupleId") Long coupleId,
                                      @RequestParam String motto) {
        coupleService.updateMotto(coupleId, motto);
        return ResultVO.success();
    }

    @PostMapping("/unbind")
    @Operation(summary = "解除绑定", description = "解除情侣关系")
    public ResultVO<Void> unbindCouple(@RequestAttribute("coupleId") Long coupleId,
                                       @RequestAttribute("userId") Long userId) {
        coupleService.unbindCouple(coupleId, userId);
        return ResultVO.success();
    }

    @PostMapping("/photo")
    @Operation(summary = "上传合照", description = "上传情侣合照")
    public ResultVO<String> uploadCouplePhoto(@RequestAttribute("coupleId") Long coupleId,
                                              @RequestParam("file") MultipartFile file) {
        String url = fileService.uploadImageWithThumbnail(file, "couple")[1];
        coupleService.updateCouplePhoto(coupleId, url);
        return ResultVO.success(url);
    }
}
