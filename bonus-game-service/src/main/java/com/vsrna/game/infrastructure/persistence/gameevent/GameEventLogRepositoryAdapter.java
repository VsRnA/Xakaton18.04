package com.vsrna.game.infrastructure.persistence.gameevent;

import com.vsrna.game.domain.gameevent.GameEventLog;
import com.vsrna.game.domain.gameevent.GameEventLogQuery;
import com.vsrna.game.domain.gameevent.GameEventLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GameEventLogRepositoryAdapter implements GameEventLogRepository {

    private final GameEventLogJpaRepository jpa;

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
        jpaEntity.setDetails(event.getDetails());
        jpaEntity.setOccurredAt(event.getOccurredAt());
        return jpaEntity;
    }

    private GameEventLog toDomain(GameEventLogJpa jpaEntity) {
        GameEventLog event = new GameEventLog();
        event.setId(jpaEntity.getId());
        event.setRoomId(jpaEntity.getRoomId());
        event.setEventType(jpaEntity.getEventType());
        event.setDetails(jpaEntity.getDetails());
        event.setOccurredAt(jpaEntity.getOccurredAt());
        return event;
    }
}
