package com.mycompany.client;

import com.mycompany.GameLogic.*;

import javax.swing.*;
import java.awt.*;

public class BoardPanel extends JPanel {
    private Piece[][] board = GameState.initialBoard();
    private final Client client;
    private final PieceColor role;
    private PieceColor turn = PieceColor.WHITE;
    private GameStatus status = GameStatus.ACTIVE;
    private int selectedX = -1;
    private int selectedY = -1;
    private boolean interactionEnabled = true;
    private final JButton[][] buttons = new JButton[8][8];

    public BoardPanel(Client client, PieceColor role) {
        this.client = client;
        this.role = role;
        setLayout(new GridLayout(8, 8));
        createBoard();
        refresh();
    }

    private void createBoard() {
        boolean whiteBottom = role == PieceColor.WHITE;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int boardX = whiteBottom ? col : 7 - col;
                int boardY = whiteBottom ? row : 7 - row;

                JButton button = new JButton();
                button.setFont(new Font("SansSerif", Font.PLAIN, 40));
                button.setFocusPainted(false);
                button.setOpaque(true);
                button.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70), 1));

                buttons[boardY][boardX] = button;

                int finalX = boardX;
                int finalY = boardY;

                button.addActionListener(e -> handleClick(finalX, finalY));

                add(button);
            }
        }
    }

    public void setState(Piece[][] newBoard, PieceColor newTurn, GameStatus newStatus, String message) {
        this.board = newBoard;
        this.turn = newTurn;
        this.status = newStatus;
        this.selectedX = -1;
        this.selectedY = -1;
        this.interactionEnabled = !isGameOver(newStatus);
        refresh();
    }

    public void setInteractionEnabled(boolean enabled) {
        this.interactionEnabled = enabled;

        for (JButton[] row : buttons) {
            for (JButton b : row) {
                b.setEnabled(enabled);
            }
        }
    }

    private void handleClick(int x, int y) {
        if (!interactionEnabled || isGameOver(status)) return;
        if (!Move.insideBoard(x, y)) return;

        if (turn != role) {
            JOptionPane.showMessageDialog(
                    this,
                    "It is not your turn",
                    "Invalid move",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Piece clicked = board[y][x];

        if (selectedX == -1) {
            if (clicked != null && clicked.color == role) {
                selectedX = x;
                selectedY = y;
                refresh();
                showSelectionAndPseudoLegalMoves(x, y);
            }
            return;
        }

        if (clicked != null && clicked.color == role) {
            selectedX = x;
            selectedY = y;
            refresh();
            showSelectionAndPseudoLegalMoves(x, y);
            return;
        }

        Move move = buildMoveWithPromotionIfNeeded(selectedX, selectedY, x, y);

        selectedX = -1;
        selectedY = -1;

        refresh();

        if (move != null) {
            client.sendMove(move);
        }
    }

    private Move buildMoveWithPromotionIfNeeded(int sx, int sy, int dx, int dy) {
        Piece piece = board[sy][sx];
        PieceType promotion = null;

        if (piece != null && piece.type == PieceType.PAWN && (dy == 0 || dy == 7)) {
            String[] options = {"QUEEN", "ROOK", "BISHOP", "KNIGHT"};

            String selected = (String) JOptionPane.showInputDialog(
                    this,
                    "Choose promotion piece",
                    "Pawn Promotion",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    "QUEEN"
            );

            if (selected == null) return null;

            promotion = PieceType.valueOf(selected);
        }

        return new Move(sx, sy, dx, dy, promotion);
    }

    private void refresh() {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                updateButton(buttons[y][x], x, y);
            }
        }
    }

    private void updateButton(JButton button, int x, int y) {
        Color light = new Color(240, 217, 181);
        Color dark = new Color(181, 136, 99);

        button.setBackground((x + y) % 2 == 0 ? light : dark);
        button.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70), 1));

        Piece piece = board[y][x];

        button.setText(piece == null ? "" : getPieceSymbol(piece));
        button.setEnabled(interactionEnabled);
    }

    private String getPieceSymbol(Piece piece) {
        boolean white = piece.color == PieceColor.WHITE;

        return switch (piece.type) {
            case KING -> white ? "♔" : "♚";
            case QUEEN -> white ? "♕" : "♛";
            case ROOK -> white ? "♖" : "♜";
            case BISHOP -> white ? "♗" : "♝";
            case KNIGHT -> white ? "♘" : "♞";
            case PAWN -> white ? "♙" : "♟";
        };
    }

    private void showSelectionAndPseudoLegalMoves(int x, int y) {
        buttons[y][x].setBackground(new Color(255, 220, 80));
        buttons[y][x].setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));

        for (int dy = 0; dy < 8; dy++) {
            for (int dx = 0; dx < 8; dx++) {
                if (dx == x && dy == y) continue;

                Move move = new Move(x, y, dx, dy, PieceType.QUEEN);

                if (GameState.isBasicMoveValid(board, move, role, null)) {
                    if (board[dy][dx] == null) {
                        buttons[dy][dx].setBackground(new Color(120, 200, 120));
                    } else {
                        buttons[dy][dx].setBackground(new Color(220, 110, 110));
                    }
                }
            }
        }
    }

    private boolean isGameOver(GameStatus s) {
        return s == GameStatus.CHECKMATE
                || s == GameStatus.STALEMATE
                || s == GameStatus.DRAW
                || s == GameStatus.RESIGNED
                || s == GameStatus.DISCONNECTED;
    }
}