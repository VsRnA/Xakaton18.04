package com.vsrna.game.application.round;

import com.vsrna.game.domain.history.GameHistory;
import com.vsrna.game.domain.history.GameHistoryQuery;
import com.vsrna.game.domain.history.GameHistoryRepository;
import com.vsrna.game.domain.participant.GameParticipant;
import com.vsrna.game.domain.participant.GameParticipantQuery;
import com.vsrna.game.domain.participant.GameParticipantRepository;
import com.vsrna.game.domain.round.ParticipantRoundEntry;
import com.vsrna.game.domain.round.ParticipantRoundEntryQuery;
import com.vsrna.game.domain.round.ParticipantRoundEntryRepository;
import com.vsrna.game.domain.round.RoundResultQuery;
import com.vsrna.game.domain.round.RoundResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoundResultService {

    private final RoundResultRepository roundResultRepository;
    private final ParticipantRoundEntryRepository entryRepository;
    private final GameParticipantRepository participantRepository;
    private final GameHistoryRepository gameHistoryRepository;

    @Transactional(readOnly = true)
    public RoundResultDetails getRoundResult(UUID roomId, int roundNumber) {
        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));
        List<ParticipantRoundEntry> entries = entryRepository.list(
                ParticipantRoundEntryQuery.byRoundResult(roundResult.getId()));

        Map<UUID, GameParticipant> participantMap = participantRepository
                .list(GameParticipantQuery.byRoom(roomId)).stream()
                .collect(Collectors.toMap(GameParticipant::getId, p -> p));

        List<RoundResultDetails.ParticipantScore> scores = entries.stream()
                .map(entry -> {
                    GameParticipant participant = participantMap.get(entry.getParticipantId());
                    return new RoundResultDetails.ParticipantScore(
                            entry.getParticipantId(),
                            participant != null && participant.isBot(),
                            entry.getTotalScore(),
                            entry.getSelectionCount(),
                            entry.getRankInRound()
                    );
                })
                .toList();

        UUID winnerId = entries.stream()
                .filter(entry -> entry.getRankInRound() != null && entry.getRankInRound() == 1)
                .map(ParticipantRoundEntry::getParticipantId)
                .findFirst().orElse(null);

        return new RoundResultDetails(roundResult, scores, winnerId);
    }

    @Transactional(readOnly = true)
    public GameHistory getGameHistory(UUID roomId) {
        return gameHistoryRepository.get(GameHistoryQuery.byRoom(roomId));
    }
}
