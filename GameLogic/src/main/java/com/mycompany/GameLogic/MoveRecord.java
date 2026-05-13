package com.mycompany.GameLogic;

// Uygulanan hamlenin özel durumlarla birlikte geçmiş kaydını tutar.
public class MoveRecord {
    public final Move move;
    public final Piece movedPiece;
    public final Piece capturedPiece;
    public final boolean castling;
    public final boolean enPassant;
    public final PieceType promotion;
    public final String notation;

    public MoveRecord(Move move, Piece movedPiece, Piece capturedPiece, boolean castling, boolean enPassant, PieceType promotion, String notation) {
        this.move = move;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.castling = castling;
        this.enPassant = enPassant;
        this.promotion = promotion;
        this.notation = notation;
    }
}
