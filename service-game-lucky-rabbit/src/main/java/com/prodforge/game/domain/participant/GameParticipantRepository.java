package com.prodforge.game.domain.participant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameParticipantRepository {
    GameParticipant create(GameParticipant participant);
    Optional<GameParticipant> find(GameParticipantQuery query);
    GameParticipant get(GameParticipantQuery query);
    List<GameParticipant> list(GameParticipantQuery query);
    List<GameParticipant> listByRoomIds(List<UUID> roomIds);
    int count(GameParticipantQuery query);
    GameParticipant update(GameParticipantQuery query, GameParticipantPatch patch);
}
