
package com.mycompany.multiplayer.chess.project;

public class Move {
     public final int sx, sy, dx, dy; //Başlangıç ve hedef kare koordinatları.
     public final PieceType promotion; //Piyon terfisi varsa hangi taşa dönüşeceğini tutar.
     
    public Move(int sx, int sy, int dx, int dy) {
        this(sx, sy, dx, dy, null);
    }   //Terfisiz hamle oluşumu.
    
    
    public Move(int sx, int sy, int dx, int dy, PieceType promotion) {
        this.sx = sx;
        this.sy = sy;
        this.dx = dx;
        this.dy = dy;
        this.promotion = promotion;
    }  //Terfi ihtimalli hamle oluşumu.
    
     
    public static boolean insideBoard(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }   //Verilen x,y'nin 8x8 satranç tahtasının içinde olma durumunu kontrol eder.
    
       
    public boolean isInsideBoard() {
        return insideBoard(sx, sy) && insideBoard(dx, dy);
    }  //Hamle başlangıcı ve hedefin boardda olup olmadığını kontrol etme.
    
    
    @Override
    public String toString() {
        return sx + "," + sy + "," + dx + "," + dy + (promotion == null ? "" : "," + promotion.name());
    }  //Hamleyi protokolde veya logda kullanılabilecek metin formatına çevirir.
    
     
    public static Move fromProtocol(String[] parts) {  // Server ve client arasında geçen MOVE mesajını Move nesnesine çevirir.
        if (parts.length < 5 || !"MOVE".equals(parts[0])) {
            throw new IllegalArgumentException("Invalid MOVE message format");
        }

        // Koordinatlar metinden sayıya çevrilir.
        int sx = Integer.parseInt(parts[1]);
        int sy = Integer.parseInt(parts[2]);
        int dx = Integer.parseInt(parts[3]);
        int dy = Integer.parseInt(parts[4]);

        PieceType promotion = null;

        // Eğer mesajda terfi bilgisi varsa PieceType enum değerine çevrilir.
        if (parts.length >= 6 && !parts[5].isBlank()) {
            promotion = PieceType.valueOf(parts[5]);

            // Satrançta piyon şah veya piyona terfi edemez.
            if (promotion == PieceType.KING || promotion == PieceType.PAWN) {
                throw new IllegalArgumentException("Invalid promotion piece");
            }
        }

        Move move = new Move(sx, sy, dx, dy, promotion); //Beklenen format: MOVE,sx,sy,dx,dy veya MOVE,sx,sy,dx,dy,promotion

        // Tahta dışına çıkan hamleler geçersiz sayılır.
        if (!move.isInsideBoard()) {
            throw new IllegalArgumentException("Move is outside board bounds");
        }

        return move;
    }
}
