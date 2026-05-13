package com.mycompany.GameLogic;

// Hamle denemesinin başarılı olup olmadığını ve mesajını taşır.
public class MoveResult {
    public final boolean success;
    public final String message;
    public final GameStatus status;

    private MoveResult(boolean success, String message, GameStatus status) {
        this.success = success;
        this.message = message;
        this.status = status;
    }

    public static MoveResult ok(String message, GameStatus status) {
        return new MoveResult(true, message, status);
    }

    public static MoveResult fail(String message) {
        return new MoveResult(false, message, GameStatus.ACTIVE);
    }
}
