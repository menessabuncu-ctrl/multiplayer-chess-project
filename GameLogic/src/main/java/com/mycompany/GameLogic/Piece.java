
package com.mycompany.GameLogic;

public class Piece {
    public final PieceType type;
    public final PieceColor color;
    public boolean moved;
    
    public Piece(PieceType type, PieceColor color){
            this(type, color, false);
    }
    public Piece(PieceType type, PieceColor color, boolean moved){
        this.color = color;
        this.type = type;
        this.moved = moved;
    }
    public Piece copy(){
        return new Piece(type, color, moved);
    }
   
}
