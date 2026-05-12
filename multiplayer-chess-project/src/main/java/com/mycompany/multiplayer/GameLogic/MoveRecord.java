
package com.mycompany.multiplayer.GameLogic;

// Yapılmış bir hamlenin geçmiş kaydını tutar.
// Hamle geçmişi; en passant, beraberlik kuralları veya oyun analizi için kullanılabilir.
public class MoveRecord {

    // Yapılan hamle bilgisi.
    public final Move move;

    // Hamleyi yapan taş.
    public final Piece movedPiece;

    // Eğer taş yenildiyse, yenilen taş burada tutulur.
    public final Piece capturedPiece;

    // Bu hamlenin rok olup olmadığını belirtir.
    public final boolean castling;

    // Bu hamlenin en passant olup olmadığını belirtir.
    public final boolean enPassant;

    // Piyon terfisi varsa hangi taşa terfi edildiğini tutar.
    public final PieceType promotion;

    // Hamlenin okunabilir gösterimi veya kısa açıklaması.
    public final String notation;

    
    public MoveRecord(  // Hamle kaydını tüm detaylarıyla oluşturmak için.
            Move move,
            Piece movedPiece,
            Piece capturedPiece,
            boolean castling,
            boolean enPassant,
            PieceType promotion,
            String notation
    ) {
        this.move = move;
        this.movedPiece = movedPiece;
        this.capturedPiece = capturedPiece;
        this.castling = castling;
        this.enPassant = enPassant;
        this.promotion = promotion;
        this.notation = notation;
    }
}
