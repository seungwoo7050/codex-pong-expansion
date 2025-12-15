package com.codexpong.backend.admin;

import com.codexpong.backend.admin.dto.AdminStatsResponse;
import com.codexpong.backend.admin.dto.AdminUserResponse;
import com.codexpong.backend.admin.dto.ModerationRequest;
import com.codexpong.backend.game.GameResultResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/admin/AdminController.java
 * 설명:
 *   - 관리자용 조회/제재 API를 묶어 제공한다.
 *   - v0.9.0에서는 인증된 사용자만 접근 가능하도록 SecurityConfig 기본 정책을 활용한다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/backend/v0.9.0-admin-and-ops.md
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return adminService.listUsers();
    }

    @GetMapping("/users/{userId}/matches")
    public List<GameResultResponse> userMatches(@PathVariable Long userId) {
        return adminService.userMatches(userId);
    }

    @PostMapping("/users/{userId}/moderations")
    public AdminUserResponse moderate(@PathVariable Long userId, @Valid @RequestBody ModerationRequest request) {
        return adminService.moderate(userId, request);
    }

    @GetMapping("/matches")
    public List<GameResultResponse> matches() {
        return adminService.recentMatches();
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.stats();
    }
}
