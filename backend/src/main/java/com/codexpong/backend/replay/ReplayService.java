package com.codexpong.backend.replay;

import com.codexpong.backend.game.GameResult;
import com.codexpong.backend.game.domain.GameRoom;
import com.codexpong.backend.game.engine.model.GameSnapshot;
import com.codexpong.backend.user.domain.User;
import com.codexpong.backend.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * [서비스] backend/src/main/java/com/codexpong/backend/replay/ReplayService.java
 * 설명:
 *   - v0.11.0 리플레이 녹화/저장/조회 흐름을 담당한다.
 *   - 게임 방 생성 시 버퍼를 준비해 틱마다 스냅샷을 누적하고, 종료 시 파일을 생성한 뒤 메타데이터를 저장한다.
 *   - 소유자별 조회/다운로드 권한을 검증하고, 보존 정책에 따라 오래된 리플레이를 정리한다.
 * 버전: v0.11.0
 * 관련 설계문서:
 *   - design/backend/v0.11.0-replay-recording-and-storage.md
 */
@Service
public class ReplayService {

    private static final String EVENT_FORMAT = "JSONL_V1";

    private final Map<String, RecordingBuffer> buffers = new ConcurrentHashMap<>();

    private final ReplayRepository replayRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Path storageDir;
    private final int retentionLimit;

    public ReplayService(ReplayRepository replayRepository,
            UserRepository userRepository, ObjectMapper objectMapper,
            @Value("${replay.storage.path}") String storagePath,
            @Value("${replay.retention.max-per-user:20}") int retentionLimit) {
        this.replayRepository = replayRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.storageDir = Paths.get(storagePath);
        this.retentionLimit = retentionLimit;
        try {
            Files.createDirectories(storageDir);
        } catch (IOException ignored) {
        }
    }

    /**
     * 설명:
     *   - 경기 방이 생성될 때 초기 스냅샷을 기록하기 위해 버퍼를 준비한다.
     */
    public void startRecording(GameRoom room) {
        RecordingBuffer buffer = new RecordingBuffer(System.currentTimeMillis());
        buffer.append(new ReplayEventRecord(0, room.currentSnapshot()));
        buffers.put(room.getRoomId(), buffer);
    }

    /**
     * 설명:
     *   - 틱마다 전달된 스냅샷을 시간 오프셋과 함께 버퍼에 누적한다.
     */
    public void appendSnapshot(String roomId, GameSnapshot snapshot) {
        RecordingBuffer buffer = buffers.get(roomId);
        if (buffer == null) {
            return;
        }
        long offset = System.currentTimeMillis() - buffer.startedAtMs();
        buffer.append(new ReplayEventRecord(Math.max(offset, 0), snapshot));
    }

    /**
     * 설명:
     *   - 경기 종료 시 녹화 버퍼를 파일로 직렬화하고 두 플레이어 소유 리플레이를 생성한다.
     */
    @Transactional
    public List<Replay> completeRecording(GameRoom room, GameResult result) {
        RecordingBuffer buffer = buffers.remove(room.getRoomId());
        if (buffer == null || buffer.events().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Path filePath = writeFile(room.getRoomId(), result.getId(), buffer.events());
            String checksum = checksum(filePath);
            long durationMs = calculateDuration(buffer, result);
            Replay replayA = new Replay(result, result.getPlayerA(), durationMs, EVENT_FORMAT,
                    filePath.toString(), checksum);
            Replay replayB = new Replay(result, result.getPlayerB(), durationMs, EVENT_FORMAT,
                    filePath.toString(), checksum);
            List<Replay> saved = replayRepository.saveAll(List.of(replayA, replayB));
            enforceRetention(result.getPlayerA());
            enforceRetention(result.getPlayerB());
            return saved;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "리플레이 저장에 실패했습니다.", ex);
        }
    }

    @Transactional(readOnly = true)
    public ReplayPageResponse listMyReplays(Long ownerId, int page, int size) {
        User owner = findOwner(ownerId);
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var result = replayRepository.findByOwnerOrderByCreatedAtDesc(owner, pageable)
                .map(entity -> ReplaySummaryResponse.from(entity, owner));
        return new ReplayPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ReplayDetailResponse getReplay(Long ownerId, Long replayId) {
        User owner = findOwner(ownerId);
        Replay replay = replayRepository.findByIdAndOwner(replayId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리플레이를 찾을 수 없습니다."));
        return ReplayDetailResponse.from(replay, owner);
    }

    @Transactional(readOnly = true)
    public ReplayDetailResponse getReplayByMatch(Long ownerId, Long matchId) {
        User owner = findOwner(ownerId);
        Replay replay = replayRepository.findByMatchIdAndOwner(matchId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리플레이를 찾을 수 없습니다."));
        return ReplayDetailResponse.from(replay, owner);
    }

    @Transactional(readOnly = true)
    public InputStreamResource streamEvents(Long ownerId, Long replayId) {
        User owner = findOwner(ownerId);
        Replay replay = replayRepository.findByIdAndOwner(replayId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리플레이를 찾을 수 없습니다."));
        Path path = Paths.get(replay.getStorageUri());
        try {
            return new InputStreamResource(Files.newInputStream(path));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "리플레이 파일을 읽을 수 없습니다.");
        }
    }

    private User findOwner(Long ownerId) {
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private Path writeFile(String roomId, Long matchId, List<ReplayEventRecord> events) throws IOException {
        String filename = roomId + "-" + matchId + ".jsonl";
        Path filePath = storageDir.resolve(filename).toAbsolutePath().normalize();
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            for (ReplayEventRecord event : events) {
                writer.write(objectMapper.writeValueAsString(event));
                writer.newLine();
            }
        }
        return filePath;
    }

    private long calculateDuration(RecordingBuffer buffer, GameResult result) {
        if (result.getFinishedAt() != null && result.getStartedAt() != null) {
            return Math.max(0, ChronoUnit.MILLIS.between(result.getStartedAt(), result.getFinishedAt()));
        }
        return buffer.events().isEmpty() ? 0 : buffer.events().get(buffer.events().size() - 1).offsetMs();
    }

    private String checksum(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(path);
            byte[] hashed = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("해시 알고리즘을 찾을 수 없습니다.", ex);
        }
    }

    private void enforceRetention(User owner) {
        List<Replay> all = replayRepository.findByOwnerOrderByCreatedAtDesc(owner);
        if (all.size() <= retentionLimit) {
            return;
        }
        List<Replay> toRemove = new ArrayList<>(all.subList(retentionLimit, all.size()));
        for (Replay replay : toRemove) {
            replayRepository.delete(replay);
            cleanupStorageIfOrphan(replay.getStorageUri());
        }
    }

    private void cleanupStorageIfOrphan(String storageUri) {
        long count = replayRepository.countByStorageUri(storageUri);
        if (count > 0) {
            return;
        }
        Path path = Paths.get(storageUri);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }

    private record RecordingBuffer(long startedAtMs, List<ReplayEventRecord> events) {

        RecordingBuffer(long startedAtMs) {
            this(startedAtMs, Collections.synchronizedList(new ArrayList<>()));
        }

        void append(ReplayEventRecord event) {
            events.add(event);
        }
    }
}
