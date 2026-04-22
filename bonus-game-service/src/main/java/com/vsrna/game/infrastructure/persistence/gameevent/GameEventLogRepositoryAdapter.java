package com.vsrna.game.infrastructure.persistence.gameevent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsrna.game.domain.gameevent.GameEventLog;
import com.vsrna.game.domain.gameevent.GameEventLogQuery;
import com.vsrna.game.domain.gameevent.GameEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GameEventLogRepositoryAdapter implements GameEventLogRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final GameEventLogJpaRepository jpa;
    private final ObjectMapper objectMapper;

    @Override
    public GameEventLog save(GameEventLog event) {
        return toDomain(jpa.save(toJpa(event)));
    }

    @Override
    public List<GameEventLog> list(GameEventLogQuery query) {
        int page = Math.max(query.page(), 0);
        int size = query.size() > 0 ? query.size() : 200;
        return jpa.findByRoomIdOrderByOccurredAtAsc(query.roomId(), PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    private GameEventLogJpa toJpa(GameEventLog event) {
        GameEventLogJpa jpaEntity = new GameEventLogJpa();
        jpaEntity.setId(event.getId());
        jpaEntity.setRoomId(event.getRoomId());
        jpaEntity.setEventType(event.getEventType());
        jpaEntity.setDetails(serializeDetails(event.getDetails()));
        jpaEntity.setOccurredAt(event.getOccurredAt());
        return jpaEntity;
    }

    private GameEventLog toDomain(GameEventLogJpa jpaEntity) {
        GameEventLog event = new GameEventLog();
        event.setId(jpaEntity.getId());
        event.setRoomId(jpaEntity.getRoomId());
        event.setEventType(jpaEntity.getEventType());
        event.setDetails(deserializeDetails(jpaEntity.getDetails()));
        event.setOccurredAt(jpaEntity.getOccurredAt());
        return event;
    }

    private String serializeDetails(Map<String, Object> details) {
        if (details == null) return null;
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            log.warn("Failed to serialize event details", e);
            return null;
        }
    }

    private Map<String, Object> deserializeDetails(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            log.warn("Failed to deserialize event details: {}", json, e);
            return Map.of();
        }
    }
}
