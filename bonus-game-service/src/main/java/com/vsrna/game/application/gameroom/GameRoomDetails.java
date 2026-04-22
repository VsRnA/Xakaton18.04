package com.vsrna.game.application.gameroom;

import com.vsrna.game.domain.gameroom.GameRoom;
import com.vsrna.game.domain.gameroom.GameRoomConfig;
import com.vsrna.game.domain.participant.GameParticipant;

import java.util.List;

public record GameRoomDetails(GameRoom room, GameRoomConfig config, List<GameParticipant> participants) {}
