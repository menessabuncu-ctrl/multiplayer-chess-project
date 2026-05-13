package com.mycompany.GameLogic;

// Oyunun aktif, bitmiş veya özel sonuç durumlarını belirtir.
public enum GameStatus {
    WAITING,
    ACTIVE,
    CHECK,
    CHECKMATE,
    STALEMATE,
    DRAW,
    RESIGNED,
    DISCONNECTED
}
