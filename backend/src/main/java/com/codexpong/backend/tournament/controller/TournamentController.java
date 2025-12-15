package com.codexpong.backend.tournament.controller;

import com.codexpong.backend.auth.model.AuthenticatedUser;
import com.codexpong.backend.tournament.dto.TournamentCreateRequest;
import com.codexpong.backend.tournament.dto.TournamentDetailResponse;
import com.codexpong.backend.tournament.dto.TournamentSummaryResponse;
import com.codexpong.backend.tournament.service.TournamentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * [컨트롤러] backend/src/main/java/com/codexpong/backend/tournament/controller/TournamentController.java
 * 설명:
 *   - 토너먼트 생성/참가/시작 및 조회 REST 엔드포인트를 제공한다.
 *   - v0.7.0 단일 제거 브래킷 규칙에 맞춰 단순한 흐름으로 노출한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping
    public List<TournamentSummaryResponse> list() {
        return tournamentService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentDetailResponse create(@AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody TournamentCreateRequest request) {
        return tournamentService.create(user.id(), request);
    }

    @PostMapping("/{tournamentId}/join")
    public TournamentDetailResponse join(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long tournamentId) {
        return tournamentService.join(tournamentId, user.id());
    }

    @PostMapping("/{tournamentId}/start")
    public TournamentDetailResponse start(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long tournamentId) {
        return tournamentService.start(tournamentId, user.id());
    }

    @GetMapping("/{tournamentId}")
    public TournamentDetailResponse detail(@PathVariable Long tournamentId) {
        return tournamentService.detail(tournamentId);
    }
}
