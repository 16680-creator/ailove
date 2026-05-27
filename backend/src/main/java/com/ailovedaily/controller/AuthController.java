package com.ailovedaily.controller;

import com.ailovedaily.dto.AccountLoginDTO;
import com.ailovedaily.dto.LoginDTO;
import com.ailovedaily.dto.RegisterDTO;
import com.ailovedaily.service.UserService;
import com.ailovedaily.vo.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录相关接口")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "微信登录", description = "微信小程序登录")
    public ResultVO<Map<String, Object>> wxLogin(@RequestBody LoginDTO loginDTO) {
        Map<String, Object> result = userService.wxLogin(loginDTO);
        return ResultVO.success(result);
    }

    @PostMapping("/web-login")
    @Operation(summary = "Web 账号登录", description = "使用账号密码登录")
    public ResultVO<Map<String, Object>> webLogin(@Valid @RequestBody AccountLoginDTO loginDTO) {
        Map<String, Object> result = userService.accountLogin(loginDTO);
        return ResultVO.success(result);
    }

    @PostMapping("/register")
    @Operation(summary = "Web 注册", description = "使用账号密码注册并自动登录")
    public ResultVO<Map<String, Object>> register(@Valid @RequestBody RegisterDTO registerDTO) {
        Map<String, Object> result = userService.register(registerDTO);
        return ResultVO.success(result);
    }

    @PostMapping("/dev-login")
    @Operation(summary = "开发测试登录", description = "仅用于本地开发调试，跳过微信鉴权")
    public ResultVO<Map<String, Object>> devLogin(@RequestParam(defaultValue = "1") Long userId) {
        Map<String, Object> result = userService.devLogin(userId);
        return ResultVO.success(result);
    }
}
