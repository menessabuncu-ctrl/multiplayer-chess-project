package com.mycompany.GameLogic;

// Satrançtaki iki oyuncu rengini temsil eder.
public enum PieceColor {
    WHITE, BLACK;

    // Sırayı karşı renge geçirmek için diğer rengi döndürür.
    public PieceColor opposite() {
        return this == WHITE ? BLACK : WHITE;
    }
}
