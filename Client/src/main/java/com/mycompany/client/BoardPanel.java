
package com.mycompany.client;

import com.mycompany.GameLogic.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// BoardPanel satranç tahtasını çizen ve mouse tıklamalarını işleyen paneldir.
// Kullanıcı taş seçer, hedef kareye tıklar ve hamle GameFrame üzerinden server'a gönderilir.
public class BoardPanel extends JPanel {

    // Tahtanın bağlı olduğu ana oyun penceresi.
    private final GameFrame gameFrame;

    // Güncel tahta bilgisi.
    private Piece[][] board;

    // Oyuncunun rengi.
    // Siyah oyuncu için tahta ters gösterilir.
    private PieceColor playerColor;

    // Seçili karenin koordinatları.
    private int selectedX = -1;
    private int selectedY = -1;

    public BoardPanel(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
        this.board = GameState.initialBoard();

        setPreferredSize(new Dimension(640, 640));

        // Mouse tıklamalarını dinler.
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    // Server'dan gelen güncel board bilgisini alır.
    public void setBoard(Piece[][] board) {
        this.board = board;
        repaint();
    }

    // Oyuncu rengini ayarlar.
    public void setPlayerColor(PieceColor playerColor) {
        this.playerColor = playerColor;
        repaint();
    }

    // Tahtayı ve taşları çizer.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int size = Math.min(getWidth(), getHeight());
        int cell = size / 8;

        drawBoard(g, cell);
        drawPieces(g, cell);
    }

    // 8x8 tahta karelerini çizer.
    private void drawBoard(Graphics g, int cell) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {

                int boardX = screenToBoardX(col);
                int boardY = screenToBoardY(row);

                boolean light = (boardX + boardY) % 2 == 0;

                if (light) {
                    g.setColor(new Color(240, 217, 181));
                } else {
                    g.setColor(new Color(181, 136, 99));
                }

                g.fillRect(col * cell, row * cell, cell, cell);

                // Seçili kare farklı renkte gösterilir.
                if (boardX == selectedX && boardY == selectedY) {
                    g.setColor(new Color(255, 255, 0, 120));
                    g.fillRect(col * cell, row * cell, cell, cell);
                }
            }
        }
    }

    // Taşları board bilgisine göre çizer.
    private void drawPieces(Graphics g, int cell) {
        if (board == null) {
            return;
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, cell - 12));
        FontMetrics fm = g.getFontMetrics();

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece piece = board[y][x];

                if (piece == null) {
                    continue;
                }

                int screenX = boardToScreenX(x);
                int screenY = boardToScreenY(y);

                String symbol = getPieceSymbol(piece);

                int textWidth = fm.stringWidth(symbol);
                int textHeight = fm.getAscent();

                int drawX = screenX * cell + (cell - textWidth) / 2;
                int drawY = screenY * cell + (cell + textHeight) / 2 - 8;

                // Beyaz taşlar beyaz, siyah taşlar siyah çizilir.
                if (piece.color == PieceColor.WHITE) {
                    g.setColor(Color.WHITE);
                } else {
                    g.setColor(Color.BLACK);
                }

                g.drawString(symbol, drawX, drawY);
            }
        }
    }

    // Mouse tıklamasını işler.
    private void handleClick(int mouseX, int mouseY) {
        if (board == null) {
            return;
        }

        int size = Math.min(getWidth(), getHeight());
        int cell = size / 8;

        int screenCol = mouseX / cell;
        int screenRow = mouseY / cell;

        if (screenCol < 0 || screenCol >= 8 || screenRow < 0 || screenRow >= 8) {
            return;
        }

        int boardX = screenToBoardX(screenCol);
        int boardY = screenToBoardY(screenRow);

        Piece clickedPiece = board[boardY][boardX];

        // Henüz taş seçilmemişse, tıklanan kareden taş seçilir.
        if (selectedX == -1 || selectedY == -1) {
            selectPiece(boardX, boardY, clickedPiece);
            return;
        }

        // Aynı kareye tekrar tıklanırsa seçim iptal edilir.
        if (selectedX == boardX && selectedY == boardY) {
            clearSelection();
            return;
        }

        // Kendi başka taşına tıklanırsa seçim o taşa geçer.
        if (clickedPiece != null && clickedPiece.color == gameFrame.getPlayerColor()) {
            selectPiece(boardX, boardY, clickedPiece);
            return;
        }

        // Seçili taş hedef kareye oynanmak istenir.
        Move move = createMoveWithPromotionIfNeeded(selectedX, selectedY, boardX, boardY);

        clearSelection();

        gameFrame.sendMove(move);
    }

    // Oyuncunun kendi taşını seçmesini sağlar.
    private void selectPiece(int x, int y, Piece piece) {
        if (piece == null) {
            return;
        }

        if (gameFrame.getPlayerColor() == null) {
            return;
        }

        // Sadece oyuncunun kendi taşı seçilebilir.
        if (piece.color != gameFrame.getPlayerColor()) {
            return;
        }

        selectedX = x;
        selectedY = y;

        repaint();
    }

    // Seçili kare bilgisini temizler.
    private void clearSelection() {
        selectedX = -1;
        selectedY = -1;
        repaint();
    }

    // Eğer piyon son sıraya gidiyorsa terfi seçimi yaptırır.
    private Move createMoveWithPromotionIfNeeded(int sx, int sy, int dx, int dy) {
        Piece piece = board[sy][sx];

        if (piece != null && piece.type == PieceType.PAWN && (dy == 0 || dy == 7)) {
            PieceType promotion = askPromotionPiece();
            return new Move(sx, sy, dx, dy, promotion);
        }

        return new Move(sx, sy, dx, dy);
    }

    // Piyon terfisi için kullanıcıdan taş seçmesini ister.
    private PieceType askPromotionPiece() {
        String[] options = {"QUEEN", "ROOK", "BISHOP", "KNIGHT"};

        String choice = (String) JOptionPane.showInputDialog(
                this,
                "Choose promotion piece:",
                "Pawn Promotion",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == null) {
            return PieceType.QUEEN;
        }

        return PieceType.valueOf(choice);
    }

    // Piece nesnesini Unicode satranç sembolüne çevirir.
    private String getPieceSymbol(Piece piece) {
        return switch (piece.type) {
            case KING -> piece.color == PieceColor.WHITE ? "♔" : "♚";
            case QUEEN -> piece.color == PieceColor.WHITE ? "♕" : "♛";
            case ROOK -> piece.color == PieceColor.WHITE ? "♖" : "♜";
            case BISHOP -> piece.color == PieceColor.WHITE ? "♗" : "♝";
            case KNIGHT -> piece.color == PieceColor.WHITE ? "♘" : "♞";
            case PAWN -> piece.color == PieceColor.WHITE ? "♙" : "♟";
        };
    }

    // Board x koordinatını ekrandaki sütuna çevirir.
    private int boardToScreenX(int boardX) {
        if (playerColor == PieceColor.BLACK) {
            return 7 - boardX;
        }

        return boardX;
    }

    // Board y koordinatını ekrandaki satıra çevirir.
    private int boardToScreenY(int boardY) {
        if (playerColor == PieceColor.BLACK) {
            return 7 - boardY;
        }

        return boardY;
    }

    // Ekrandaki sütunu board x koordinatına çevirir.
    private int screenToBoardX(int screenX) {
        if (playerColor == PieceColor.BLACK) {
            return 7 - screenX;
        }

        return screenX;
    }

    // Ekrandaki satırı board y koordinatına çevirir.
    private int screenToBoardY(int screenY) {
        if (playerColor == PieceColor.BLACK) {
            return 7 - screenY;
        }

        return screenY;
    }
}