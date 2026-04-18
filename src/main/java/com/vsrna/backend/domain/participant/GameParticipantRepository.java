package com.vsrna.backend.domain.participant;

import java.util.List;
import java.util.Optional;

public interface GameParticipantRepository {
    GameParticipant create(GameParticipant participant);
    Optional<GameParticipant> find(GameParticipantQuery query);
    GameParticipant get(GameParticipantQuery query);
    List<GameParticipant> list(GameParticipantQuery query);
    int count(GameParticipantQuery query);
    GameParticipant update(GameParticipantQuery query, GameParticipantPatch patch);
}
