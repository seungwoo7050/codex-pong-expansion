package com.codexpong.backend.tournament.service;

import com.codexpong.backend.game.domain.GameRoom;
import com.codexpong.backend.game.domain.MatchType;
import com.codexpong.backend.game.service.GameRoomService;
import com.codexpong.backend.tournament.domain.Tournament;
import com.codexpong.backend.tournament.domain.TournamentMatch;
import com.codexpong.backend.tournament.domain.TournamentParticipant;
import com.codexpong.backend.tournament.domain.TournamentStatus;
import com.codexpong.backend.tournament.dto.TournamentCreateRequest;
import com.codexpong.backend.tournament.dto.TournamentDetailResponse;
import com.codexpong.backend.tournament.dto.TournamentMatchResponse;
import com.codexpong.backend.tournament.dto.TournamentParticipantResponse;
import com.codexpong.backend.tournament.dto.TournamentSummaryResponse;
import com.codexpong.backend.tournament.repository.TournamentMatchRepository;
import com.codexpong.backend.tournament.repository.TournamentParticipantRepository;
import com.codexpong.backend.tournament.repository.TournamentRepository;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/tournament/service/TournamentService.java
 * 설명:
 *   - 토너먼트 생성/참여/시작과 브래킷 초기화를 처리한다.
 *   - GameRoomService와 연동해 라운드별 roomId를 발급하고 실시간 알림을 발행한다.
 * 버전: v0.7.0
 * 관련 설계문서:
 *   - design/backend/v0.7.0-tournaments.md
 */
@Service
@Transactional
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final TournamentParticipantRepository participantRepository;
    private final TournamentMatchRepository matchRepository;
    private final UserRepository userRepository;
    private final GameRoomService gameRoomService;
    private final TournamentEventPublisher eventPublisher;

    public TournamentService(TournamentRepository tournamentRepository,
            TournamentParticipantRepository participantRepository,
            TournamentMatchRepository matchRepository,
            UserRepository userRepository,
            GameRoomService gameRoomService,
            TournamentEventPublisher eventPublisher) {
        this.tournamentRepository = tournamentRepository;
        this.participantRepository = participantRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
        this.gameRoomService = gameRoomService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<TournamentSummaryResponse> list() {
        List<Tournament> tournaments = tournamentRepository.findAll();
        Map<Long, Integer> counts = new HashMap<>();
        tournaments.forEach(tournament -> counts.put(tournament.getId(),
                (int) participantRepository.countByTournament_Id(tournament.getId())));
        return tournaments.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(tournament -> TournamentSummaryResponse.of(tournament, counts.getOrDefault(tournament.getId(), 0)))
                .toList();
    }

    public TournamentDetailResponse create(Long creatorId, TournamentCreateRequest request) {
        User creator = getUser(creatorId);
        validateBracketSize(request.maxParticipants());

        Tournament tournament = tournamentRepository.save(new Tournament(request.name(), request.maxParticipants(), creator));
        participantRepository.save(new TournamentParticipant(tournament, creator, 1));
        return detailResponse(tournament);
    }

    public TournamentDetailResponse join(Long tournamentId, Long userId) {
        Tournament tournament = getTournament(tournamentId);
        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 시작된 토너먼트입니다.");
        }
        if (participantRepository.existsByTournament_IdAndUser_Id(tournamentId, userId)) {
            return detailResponse(tournament);
        }
        long count = participantRepository.countByTournament_Id(tournamentId);
        if (count >= tournament.getMaxParticipants()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정원이 가득 찼습니다.");
        }
        User user = getUser(userId);
        participantRepository.save(new TournamentParticipant(tournament, user, (int) count + 1));
        TournamentDetailResponse response = detailResponse(tournament);
        eventPublisher.publishToUsers(participantUserIds(tournament), "TOURNAMENT_UPDATED", response);
        return response;
    }

    public TournamentDetailResponse start(Long tournamentId, Long requesterId) {
        Tournament tournament = getTournament(tournamentId);
        if (!tournament.getCreator().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "생성자만 시작할 수 있습니다.");
        }
        if (tournament.getStatus() != TournamentStatus.REGISTRATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 시작된 토너먼트입니다.");
        }
        List<TournamentParticipant> participants = participantRepository.findByTournamentOrderBySeedAsc(tournament);
        if (participants.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "두 명 이상이 필요합니다.");
        }
        if (participants.size() != tournament.getMaxParticipants()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정원이 모두 찼을 때만 시작할 수 있습니다.");
        }
        tournament.markInProgress();
        initializeBracket(tournament, participants);
        TournamentDetailResponse response = detailResponse(tournament);
        eventPublisher.publishToUsers(participantUserIds(tournament), "TOURNAMENT_STARTED", response);
        return response;
    }

    @Transactional(readOnly = true)
    public TournamentDetailResponse detail(Long tournamentId) {
        Tournament tournament = getTournament(tournamentId);
        return detailResponse(tournament);
    }

    private void initializeBracket(Tournament tournament, List<TournamentParticipant> participants) {
        int totalPlayers = participants.size();
        int rounds = (int) (Math.log(totalPlayers) / Math.log(2));
        List<TournamentMatch> matches = new ArrayList<>();
        for (int round = 1; round <= rounds; round++) {
            int matchCount = totalPlayers / (int) Math.pow(2, round);
            for (int pos = 0; pos < matchCount; pos++) {
                matches.add(new TournamentMatch(tournament, round, pos));
            }
        }
        matchRepository.saveAll(matches);
        seedFirstRound(matches, participants);
    }

    private void seedFirstRound(List<TournamentMatch> matches, List<TournamentParticipant> participants) {
        List<TournamentMatch> firstRound = matches.stream()
                .filter(match -> match.getRoundNumber() == 1)
                .sorted((a, b) -> Integer.compare(a.getPosition(), b.getPosition()))
                .toList();
        int index = 0;
        for (TournamentMatch match : firstRound) {
            if (index + 1 >= participants.size()) {
                break;
            }
            match.assignParticipants(participants.get(index), participants.get(index + 1));
            index += 2;
            openMatch(match);
        }
    }

    public void openMatch(TournamentMatch match) {
        GameRoom room = gameRoomService.createRoom(match.getParticipantA().getUser(), match.getParticipantB().getUser(),
                MatchType.NORMAL);
        match.markReady(room.getRoomId());
        matchRepository.save(match);
        Map<String, Object> payload = Map.of(
                "tournamentId", match.getTournament().getId(),
                "matchId", match.getId(),
                "roomId", room.getRoomId(),
                "round", match.getRoundNumber()
        );
        eventPublisher.publishToUsers(participantUserIds(match), "TOURNAMENT_MATCH_READY", payload);
    }

    private List<Long> participantUserIds(Tournament tournament) {
        return participantRepository.findByTournamentOrderBySeedAsc(tournament).stream()
                .map(participant -> participant.getUser().getId())
                .toList();
    }

    private List<Long> participantUserIds(TournamentMatch match) {
        return List.of(match.getParticipantA().getUser().getId(), match.getParticipantB().getUser().getId());
    }

    private Tournament getTournament(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "토너먼트를 찾을 수 없습니다."));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    TournamentDetailResponse detailResponse(Tournament tournament) {
        List<TournamentParticipantResponse> participants = participantRepository.findByTournamentOrderBySeedAsc(tournament)
                .stream()
                .map(TournamentParticipantResponse::from)
                .toList();
        List<TournamentMatchResponse> matches = matchRepository.findByTournamentOrderByRoundNumberAscPositionAsc(tournament)
                .stream()
                .map(TournamentMatchResponse::from)
                .toList();
        return TournamentDetailResponse.of(tournament, participants, matches);
    }

    void publishUpdatedDetail(Tournament tournament, String type) {
        TournamentDetailResponse response = detailResponse(tournament);
        eventPublisher.publishToUsers(participantUserIds(tournament), type, response);
    }

    private void validateBracketSize(Integer maxParticipants) {
        if (maxParticipants == null || maxParticipants < 4 || maxParticipants > 16) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "참가 인원은 4~16명이어야 합니다.");
        }
        int value = maxParticipants;
        boolean powerOfTwo = (value & (value - 1)) == 0;
        if (!powerOfTwo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "2의 제곱수 인원만 지원합니다.");
        }
    }
}
