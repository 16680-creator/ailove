package com.ailovedaily.controller;

import com.ailovedaily.dto.UserUpdateDTO;
import com.ailovedaily.service.UserService;
import com.ailovedaily.vo.ResultVO;
import com.ailovedaily.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息相关接口")
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户信息，包含伴侣信息")
    public ResultVO<UserVO> getUserInfo(@RequestAttribute("userId") Long userId) {
        UserVO userInfo = userService.getUserInfo(userId);
        return ResultVO.success(userInfo);
    }

    @PutMapping("/info")
    @Operation(summary = "更新用户信息", description = "更新当前用户基本信息")
    public ResultVO<Void> updateUserInfo(@RequestAttribute("userId") Long userId,
                                         @Valid @RequestBody UserUpdateDTO updateDTO) {
        userService.updateUser(userId, updateDTO);
        return ResultVO.success();
    }

    @GetMapping("/partner")
    @Operation(summary = "获取伴侣信息", description = "获取当前用户的伴侣信息")
    public ResultVO<UserVO> getPartnerInfo(@RequestAttribute("userId") Long userId) {
        UserVO partner = userService.getPartnerInfo(userId);
        return ResultVO.success(partner);
    }
}
