package com.prodforge.game.application.gameroom;

import com.prodforge.game.domain.gameroom.GameRoom;
import com.prodforge.game.domain.gameroom.GameRoomConfig;
import com.prodforge.game.domain.participant.GameParticipant;

import java.util.List;

public record GameRoomDetails(GameRoom room, GameRoomConfig config, List<GameParticipant> participants) {}
