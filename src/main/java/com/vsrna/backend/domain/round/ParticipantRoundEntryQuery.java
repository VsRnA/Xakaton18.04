package com.vsrna.backend.domain.round;

import java.util.UUID;

public record ParticipantRoundEntryQuery(UUID id, UUID roundResultId, UUID participantId, Integer rankInRound) {
    public static ParticipantRoundEntryQuery byId(UUID id) {
        return new ParticipantRoundEntryQuery(id, null, null, null);
    }

    public static ParticipantRoundEntryQuery byRoundResult(UUID roundResultId) {
        return new ParticipantRoundEntryQuery(null, roundResultId, null, null);
    }

    public static ParticipantRoundEntryQuery byRoundResultAndParticipant(UUID roundResultId, UUID participantId) {
        return new ParticipantRoundEntryQuery(null, roundResultId, participantId, null);
    }
}
