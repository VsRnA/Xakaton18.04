package com.vsrna.game.application.gameroom;

import com.vsrna.game.domain.gameroom.GameRoom;
import com.vsrna.game.domain.gameroom.GameRoomConfig;

public record GameRoomDetails(GameRoom room, GameRoomConfig config) {}
