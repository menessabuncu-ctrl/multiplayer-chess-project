/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.multiplayer.GameLogic;

/**
 *
 * @author a
 */
public enum PieceColor {
    WHITE, BLACK;
    
    public PieceColor opposite(){
        return this == WHITE ? BLACK : WHITE;
    }
}
