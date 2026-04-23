package com.prodforge.game.application.gameroom;

public record NextGameOption(String type, GameRoomDetails room) {
    // type: "SAME" | "SAFER" | "RISKIER"
}
