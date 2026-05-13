package com.mycompany.GameLogic;

// Taşın türünü, rengini ve daha önce hareket edip etmediğini tutar.
public class Piece {
    public final PieceType type;
    public final PieceColor color;
    public boolean moved;

    public Piece(PieceType type, PieceColor color) {
        this(type, color, false);
    }

    public Piece(PieceType type, PieceColor color, boolean moved) {
        this.type = type;
        this.color = color;
        this.moved = moved;
    }

    // Board kopyalanırken taşın bağımsız bir kopyasını üretir.
    public Piece copy() {
        return new Piece(type, color, moved);
    }
}
