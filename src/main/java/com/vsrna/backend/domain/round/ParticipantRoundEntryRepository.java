package com.vsrna.backend.domain.round;

import java.util.List;
import java.util.Optional;

public interface ParticipantRoundEntryRepository {
    ParticipantRoundEntry create(ParticipantRoundEntry entry);
    Optional<ParticipantRoundEntry> find(ParticipantRoundEntryQuery query);
    ParticipantRoundEntry get(ParticipantRoundEntryQuery query);
    List<ParticipantRoundEntry> list(ParticipantRoundEntryQuery query);
    ParticipantRoundEntry update(ParticipantRoundEntryQuery query, ParticipantRoundEntryPatch patch);
}
