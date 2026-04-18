package com.vsrna.game.infrastructure.persistence.participant;

import com.vsrna.game.domain.exception.ApiException;
import com.vsrna.game.domain.participant.*;
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
        return toDomain(jpa.save(toJpa(participant)));
    }

    @Override
    public Optional<GameParticipant> find(GameParticipantQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id()).map(this::toDomain);
        }
        if (query.gameRoomId() != null && query.userId() != null) {
            return jpa.findByGameRoomIdAndUserId(query.gameRoomId(), query.userId()).map(this::toDomain);
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
            return jpa.findByGameRoomIdAndStatus(query.gameRoomId(), query.status())
                    .stream().map(this::toDomain).toList();
        }
        if (query.gameRoomId() != null) {
            return jpa.findByGameRoomId(query.gameRoomId()).stream().map(this::toDomain).toList();
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
        GameParticipantJpa entity = findJpa(query);
        if (patch.status() != null) entity.setStatus(patch.status());
        if (patch.advancedToFinal() != null) entity.setAdvancedToFinal(patch.advancedToFinal());
        if (patch.reservedPoints() != null) entity.setReservedPoints(patch.reservedPoints());
        return toDomain(jpa.save(entity));
    }

    private GameParticipantJpa findJpa(GameParticipantQuery query) {
        if (query.id() != null) {
            return jpa.findById(query.id())
                    .orElseThrow(() -> ApiException.notFound("GameParticipant", query.id().toString()));
        }
        if (query.gameRoomId() != null && query.userId() != null) {
            return jpa.findByGameRoomIdAndUserId(query.gameRoomId(), query.userId())
                    .orElseThrow(() -> ApiException.notFound("GameParticipant", buildDetail(query)));
        }
        throw ApiException.notFound("GameParticipant", "unknown");
    }

    private String buildDetail(GameParticipantQuery query) {
        if (query.id() != null) return query.id().toString();
        if (query.gameRoomId() != null && query.userId() != null)
            return "room=" + query.gameRoomId() + ", user=" + query.userId();
        return "unknown";
    }

    private GameParticipant toDomain(GameParticipantJpa e) {
        GameParticipant p = new GameParticipant();
        p.setId(e.getId());
        p.setGameRoomId(e.getGameRoomId());
        p.setUserId(e.getUserId());
        p.setBot(e.isBot());
        p.setReservedPoints(e.getReservedPoints());
        p.setStatus(e.getStatus());
        p.setAdvancedToFinal(e.isAdvancedToFinal());
        p.setJoinedAt(e.getJoinedAt());
        return p;
    }

    private GameParticipantJpa toJpa(GameParticipant p) {
        GameParticipantJpa e = new GameParticipantJpa();
        e.setId(p.getId());
        e.setGameRoomId(p.getGameRoomId());
        e.setUserId(p.getUserId());
        e.setBot(p.isBot());
        e.setReservedPoints(p.getReservedPoints());
        e.setStatus(p.getStatus());
        e.setAdvancedToFinal(p.isAdvancedToFinal());
        return e;
    }
}
