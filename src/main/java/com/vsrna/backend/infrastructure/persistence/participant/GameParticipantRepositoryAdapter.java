package com.vsrna.backend.infrastructure.persistence.participant;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.participant.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GameParticipantRepositoryAdapter implements GameParticipantRepository {

    private final GameParticipantJpaRepository jpa;

    @Override
    public GameParticipant create(GameParticipant participant) {
        return jpa.save(participant);
    }

    @Override
    public Optional<GameParticipant> find(GameParticipantQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id());
        }
        if (query.gameRoomId() != null && query.userId() != null) {
            return jpa.findByGameRoomIdAndUserId(query.gameRoomId(), query.userId());
        }
        return Optional.empty();
    }

    @Override
    public GameParticipant get(GameParticipantQuery query) {
        return find(query).orElseThrow(() ->
                ApiException.notFound("GameParticipant", buildDetail(query)));
    }

    @Override
    public List<GameParticipant> list(GameParticipantQuery query) {
        if (query.gameRoomId() != null && query.status() != null) {
            return jpa.findByGameRoomIdAndStatus(query.gameRoomId(), query.status());
        }
        if (query.gameRoomId() != null) {
            return jpa.findByGameRoomId(query.gameRoomId());
        }
        return List.of();
    }

    @Override
    public int count(GameParticipantQuery query) {
        if (query.gameRoomId() != null) {
            return jpa.countByGameRoomId(query.gameRoomId());
        }
        return 0;
    }

    @Override
    public GameParticipant update(GameParticipantQuery query, GameParticipantPatch patch) {
        GameParticipant p = get(query);
        if (patch.status() != null) p.setStatus(patch.status());
        if (patch.advancedToFinal() != null) p.setAdvancedToFinal(patch.advancedToFinal());
        if (patch.reservedPoints() != null) p.setReservedPoints(patch.reservedPoints());
        return jpa.save(p);
    }

    private String buildDetail(GameParticipantQuery query) {
        if (query.id() != null) return query.id().toString();
        if (query.gameRoomId() != null && query.userId() != null)
            return "room=" + query.gameRoomId() + ", user=" + query.userId();
        return "unknown";
    }
}
