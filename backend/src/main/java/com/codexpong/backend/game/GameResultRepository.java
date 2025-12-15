package com.codexpong.backend.game;

import com.codexpong.backend.user.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * [저장소] backend/src/main/java/com/codexpong/backend/game/GameResultRepository.java
 * 설명:
 *   - v0.3.0 기준 실시간 경기 결과를 조회/저장하기 위한 JPA 리포지토리 인터페이스다.
 * 버전: v0.9.0
 * 관련 설계문서:
 *   - design/backend/v0.3.0-game-and-matchmaking.md
 * 변경 이력:
 *   - v0.1.0: 기본 CRUD 지원 인터페이스 정의
 *   - v0.3.0: 최근 기록 조회 메서드 추가
 *   - v0.9.0: 사용자별 최근 경기 조회 메서드 추가
 */
@Repository
public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    /**
     * 설명:
     *   - 최근 종료된 경기 기록을 최대 20건까지 내림차순으로 조회한다.
     */
    List<GameResult> findTop20ByOrderByFinishedAtDesc();

    /**
     * 설명:
     *   - 특정 사용자가 참여한 최근 경기 기록을 최신 순으로 최대 20건까지 조회한다.
     */
    List<GameResult> findTop20ByPlayerAOrPlayerBOrderByFinishedAtDesc(User playerA, User playerB);
}
