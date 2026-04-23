package com.prodforge.game.application.round;

import com.prodforge.game.application.gameevent.GameEventLogService;
import com.prodforge.game.application.port.GameNotifierPort;
import com.prodforge.game.domain.barrel.Barrel;
import com.prodforge.game.domain.barrel.BarrelQuery;
import com.prodforge.game.domain.barrel.BarrelRepository;
import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.exception.GameErrorMessages;
import com.prodforge.game.domain.gameroom.GameRoomConfigQuery;
import com.prodforge.game.domain.gameroom.GameRoomConfigRepository;
import com.prodforge.game.domain.gameroom.GameRoomQuery;
import com.prodforge.game.domain.gameroom.GameRoomRepository;
import com.prodforge.game.domain.gameroom.GameRoomStatus;
import com.prodforge.game.domain.participant.GameParticipantQuery;
import com.prodforge.game.domain.participant.GameParticipantRepository;
import com.prodforge.game.domain.round.ParticipantBarrelSelection;
import com.prodforge.game.domain.round.ParticipantBarrelSelectionQuery;
import com.prodforge.game.domain.round.ParticipantBarrelSelectionRepository;
import com.prodforge.game.domain.round.ParticipantRoundEntry;
import com.prodforge.game.domain.round.ParticipantRoundEntryPatch;
import com.prodforge.game.domain.round.ParticipantRoundEntryQuery;
import com.prodforge.game.domain.round.ParticipantRoundEntryRepository;
import com.prodforge.game.domain.round.RoundResultQuery;
import com.prodforge.game.domain.round.RoundResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SelectionService {

    private final GameRoomRepository gameRoomRepository;
    private final GameRoomConfigRepository gameRoomConfigRepository;
    private final GameParticipantRepository participantRepository;
    private final BarrelRepository barrelRepository;
    private final RoundResultRepository roundResultRepository;
    private final ParticipantRoundEntryRepository entryRepository;
    private final ParticipantBarrelSelectionRepository selectionRepository;
    private final GameNotifierPort notifierPort;
    private final GameEventLogService gameEventLogService;

    @Transactional(readOnly = true)
    public List<Barrel> getShuffledBarrels(UUID roomId, UUID userId, int roundNumber) {
        List<Barrel> barrels = new ArrayList<>(
                barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber)));
        long seed = userId.getMostSignificantBits() ^ userId.getLeastSignificantBits();
        seed ^= ((long) roundNumber << 32);
        Collections.shuffle(barrels, new Random(seed));
        return barrels;
    }

    @Transactional
    public void submitSelection(UUID roomId, UUID userId, int roundNumber,
                                List<UUID> barrelIds, Instant timestamp) {
        var config = gameRoomConfigRepository.get(GameRoomConfigQuery.byRoom(roomId));
        if (barrelIds == null || barrelIds.isEmpty() || barrelIds.size() > config.getMaxBarrelSelection()) {
            throw ApiException.badRequest(GameErrorMessages.selectionOutOfRange(config.getMaxBarrelSelection()));
        }

        var room = gameRoomRepository.get(GameRoomQuery.byId(roomId));
        GameRoomStatus expectedStatus = roundNumber == 1 ? GameRoomStatus.ROUND_1 : GameRoomStatus.ROUND_2;
        if (room.getStatus() != expectedStatus) {
            throw ApiException.badRequest(GameErrorMessages.roundNotInProgress(roundNumber));
        }

        List<Barrel> validBarrels = barrelRepository.list(BarrelQuery.byRoomAndRound(roomId, roundNumber));
        Set<UUID> validIds = validBarrels.stream().map(Barrel::getId).collect(Collectors.toSet());
        for (UUID bid : barrelIds) {
            if (!validIds.contains(bid)) {
                throw ApiException.badRequest(GameErrorMessages.barrelNotInRound(bid));
            }
        }

        var participant = participantRepository.get(GameParticipantQuery.byRoomAndUser(roomId, userId));
        var roundResult = roundResultRepository.get(RoundResultQuery.byRoomAndRound(roomId, roundNumber));

        Optional<ParticipantRoundEntry> existing = entryRepository.find(
                ParticipantRoundEntryQuery.byRoundResultAndParticipant(roundResult.getId(), participant.getId()));

        UUID entryId;
        if (existing.isPresent()) {
            ParticipantRoundEntry entry = existing.get();
            entryId = entry.getId();
            selectionRepository.delete(ParticipantBarrelSelectionQuery.byEntry(entryId));
            entryRepository.update(
                    ParticipantRoundEntryQuery.byId(entryId),
                    ParticipantRoundEntryPatch.selection(timestamp, barrelIds.size()));
        } else {
            ParticipantRoundEntry newEntry = new ParticipantRoundEntry(roundResult.getId(), participant.getId());
            newEntry.setSelectionTimestamp(timestamp);
            newEntry.setSelectionCount(barrelIds.size());
            entryId = entryRepository.create(newEntry).getId();
        }

        List<ParticipantBarrelSelection> selections = barrelIds.stream()
                .map(barrelId -> new ParticipantBarrelSelection(entryId, barrelId))
                .toList();
        selectionRepository.createAll(selections);

        int selectedCount = entryRepository.countByRoundResult(roundResult.getId());
        int totalPlayers = participantRepository.count(GameParticipantQuery.byRoom(roomId));
        notifierPort.publishRoundEvent(roomId, Map.of(
                "type", "PLAYER_SELECTED",
                "roundNumber", roundNumber,
                "selectedCount", selectedCount,
                "totalPlayers", totalPlayers
        ));

        gameEventLogService.log(roomId, "BARREL_SELECTED", Map.of(
                "userId", userId.toString(),
                "round", roundNumber,
                "barrels", barrelIds.stream().map(UUID::toString).toList(),
                "count", barrelIds.size()
        ));
    }
}
