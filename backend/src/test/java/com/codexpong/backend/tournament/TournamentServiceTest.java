package com.codexpong.backend.tournament;

import static org.assertj.core.api.Assertions.assertThat;

import com.codexpong.backend.game.GameResultService;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.tournament.domain.TournamentMatch;
import com.codexpong.backend.tournament.domain.TournamentMatchStatus;
import com.codexpong.backend.tournament.dto.TournamentCreateRequest;
import com.codexpong.backend.tournament.dto.TournamentDetailResponse;
import com.codexpong.backend.tournament.repository.TournamentMatchRepository;
import com.codexpong.backend.tournament.repository.TournamentRepository;
import com.codexpong.backend.tournament.service.TournamentService;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

/**
 * [통합 테스트] backend/src/test/java/com/codexpong/backend/tournament/TournamentServiceTest.java
 * 설명:
 *   - 토너먼트 생성→참여→시작→진행→완료 흐름을 검증한다.
 *   - GameResultService와 연계해 매치 결과가 다음 라운드에 반영되는지 확인한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TournamentServiceTest {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private TournamentMatchRepository matchRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameResultService gameResultService;

    private User host;
    private User user2;
    private User user3;
    private User user4;

    @BeforeEach
    void setUp() {
        host = userRepository.save(new User("host", "pw", "호스트", null));
        user2 = userRepository.save(new User("user2", "pw", "사용자2", null));
        user3 = userRepository.save(new User("user3", "pw", "사용자3", null));
        user4 = userRepository.save(new User("user4", "pw", "사용자4", null));
    }

    @Test
    void 토너먼트_매치가_진행되면_다음_라운드로_진행된다() {
        TournamentDetailResponse created = tournamentService.create(host.getId(),
                new TournamentCreateRequest("주말 토너먼트", 4));
        tournamentService.join(created.id(), user2.getId());
        tournamentService.join(created.id(), user3.getId());
        tournamentService.join(created.id(), user4.getId());

        TournamentDetailResponse started = tournamentService.start(created.id(), host.getId());
        List<TournamentMatch> firstRound = matchRepository.findByTournamentOrderByRoundNumberAscPositionAsc(
                tournamentRepository.getReferenceById(started.id()))
                .stream()
                .filter(match -> match.getRoundNumber() == 1)
                .toList();

        assertThat(firstRound).hasSize(2);
        assertThat(firstRound).allSatisfy(match -> {
            assertThat(match.getStatus()).isEqualTo(TournamentMatchStatus.READY);
            assertThat(match.getRoomId()).isNotBlank();
        });

        TournamentMatch match1 = firstRound.get(0);
        TournamentMatch match2 = firstRound.get(1);

        gameResultService.recordResult(match1.getRoomId(), match1.getParticipantA().getUser(),
                match1.getParticipantB().getUser(), 5, 2, MatchType.NORMAL, LocalDateTime.now(), LocalDateTime.now());
        gameResultService.recordResult(match2.getRoomId(), match2.getParticipantA().getUser(),
                match2.getParticipantB().getUser(), 3, 5, MatchType.NORMAL, LocalDateTime.now(), LocalDateTime.now());

        TournamentMatch finalMatch = matchRepository.findByTournamentOrderByRoundNumberAscPositionAsc(
                tournamentRepository.getReferenceById(started.id()))
                .stream()
                .filter(match -> match.getRoundNumber() == 2)
                .findFirst()
                .orElseThrow();

        assertThat(finalMatch.getParticipantA()).isNotNull();
        assertThat(finalMatch.getParticipantB()).isNotNull();
        assertThat(finalMatch.getStatus()).isEqualTo(TournamentMatchStatus.READY);
        assertThat(finalMatch.getRoomId()).isNotBlank();

        gameResultService.recordResult(finalMatch.getRoomId(), finalMatch.getParticipantA().getUser(),
                finalMatch.getParticipantB().getUser(), 5, 1, MatchType.NORMAL, LocalDateTime.now(),
                LocalDateTime.now());

        TournamentMatch completedFinal = matchRepository.findById(finalMatch.getId()).orElseThrow();
        assertThat(completedFinal.getStatus()).isEqualTo(TournamentMatchStatus.COMPLETED);
        assertThat(completedFinal.getWinner()).isNotNull();
        assertThat(tournamentRepository.findById(started.id()).orElseThrow().getStatus().name())
                .isEqualTo("COMPLETED");
    }
}
