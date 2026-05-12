package com.mycompany.GameLogic;

import java.util.*;

public class GameState {
    private Piece[][] board;
    private PieceColor turn;
    private GameStatus status;
    private String statusMessage;
    private PieceColor winner;
    private MoveRecord lastMove;
    private int halfMoveClock;
    private int fullMoveNumber;
    private final List<MoveRecord> history = new ArrayList<>();
    private final Map<String, Integer> positionCounts = new HashMap<>();
    private PieceColor drawOfferBy;

    public GameState() {
        reset();
    }

    public final void reset() {
        board = initialBoard();
        turn = PieceColor.WHITE;
        status = GameStatus.ACTIVE;
        statusMessage = "White to move";
        winner = null;
        lastMove = null;
        halfMoveClock = 0;
        fullMoveNumber = 1;
        history.clear();
        positionCounts.clear();
        drawOfferBy = null;
        rememberPosition();
    }

    public static Piece[][] initialBoard() {
        Piece[][] b = new Piece[8][8];
        for (int i = 0; i < 8; i++) {
            b[1][i] = new Piece(PieceType.PAWN, PieceColor.BLACK);
            b[6][i] = new Piece(PieceType.PAWN, PieceColor.WHITE);
        }
        b[0][0] = new Piece(PieceType.ROOK, PieceColor.BLACK);
        b[0][7] = new Piece(PieceType.ROOK, PieceColor.BLACK);
        b[7][0] = new Piece(PieceType.ROOK, PieceColor.WHITE);
        b[7][7] = new Piece(PieceType.ROOK, PieceColor.WHITE);
        b[0][1] = new Piece(PieceType.KNIGHT, PieceColor.BLACK);
        b[0][6] = new Piece(PieceType.KNIGHT, PieceColor.BLACK);
        b[7][1] = new Piece(PieceType.KNIGHT, PieceColor.WHITE);
        b[7][6] = new Piece(PieceType.KNIGHT, PieceColor.WHITE);
        b[0][2] = new Piece(PieceType.BISHOP, PieceColor.BLACK);
        b[0][5] = new Piece(PieceType.BISHOP, PieceColor.BLACK);
        b[7][2] = new Piece(PieceType.BISHOP, PieceColor.WHITE);
        b[7][5] = new Piece(PieceType.BISHOP, PieceColor.WHITE);
        b[0][3] = new Piece(PieceType.QUEEN, PieceColor.BLACK);
        b[7][3] = new Piece(PieceType.QUEEN, PieceColor.WHITE);
        b[0][4] = new Piece(PieceType.KING, PieceColor.BLACK);
        b[7][4] = new Piece(PieceType.KING, PieceColor.WHITE);
        return b;
    }

    public synchronized MoveResult playMove(Move move, PieceColor player) {
        if (isGameOver()) return MoveResult.fail("Game is already over");
        if (player != turn) return MoveResult.fail("It is not your turn");
        if (!move.isInsideBoard()) return MoveResult.fail("Move is outside board bounds");
        Piece piece = board[move.sy][move.sx];
        if (piece == null) return MoveResult.fail("No piece on selected square");
        if (piece.color != player) return MoveResult.fail("Selected piece belongs to opponent");
        if (!isLegalMove(board, move, player, lastMove)) return MoveResult.fail("Illegal move");

        MoveRecord record = applyMoveInternal(board, move, true);
        lastMove = record;
        history.add(record);
        drawOfferBy = null;

        if (record.movedPiece.type == PieceType.PAWN || record.capturedPiece != null) halfMoveClock = 0;
        else halfMoveClock++;
        if (player == PieceColor.BLACK) fullMoveNumber++;

        turn = turn.opposite();
        rememberPosition();
        updateGameStatusAfterMove(player, turn);
        return MoveResult.ok(statusMessage, status);
    }

    public synchronized void resign(PieceColor player) {
        status = GameStatus.RESIGNED;
        winner = player.opposite();
        statusMessage = player + " resigned. " + winner + " wins.";
    }

    public synchronized String offerDraw(PieceColor player) {
        if (isGameOver()) return "Game is already over";
        drawOfferBy = player;
        statusMessage = player + " offered a draw.";
        return statusMessage;
    }

    public synchronized String respondDraw(PieceColor player, boolean accepted) {
        if (drawOfferBy == null) return "There is no active draw offer.";
        if (drawOfferBy == player) return "You cannot answer your own draw offer.";
        if (accepted) {
            status = GameStatus.DRAW;
            winner = null;
            statusMessage = "Draw agreed by both players.";
        } else {
            statusMessage = player + " declined the draw offer. " + turn + " to move.";
        }
        drawOfferBy = null;
        return statusMessage;
    }

    public synchronized void opponentDisconnected(PieceColor disconnected) {
        if (!isGameOver()) {
            status = GameStatus.DISCONNECTED;
            winner = disconnected.opposite();
            statusMessage = disconnected + " disconnected. " + winner + " wins.";
        }
    }

    private void updateGameStatusAfterMove(PieceColor mover, PieceColor next) {
        boolean check = isKingInCheck(board, next);
        boolean anyMove = hasAnyLegalMove(board, next, lastMove);
        if (check && !anyMove) {
            status = GameStatus.CHECKMATE;
            winner = mover;
            statusMessage = mover + " wins by checkmate.";
            return;
        }
        if (!check && !anyMove) {
            status = GameStatus.STALEMATE;
            winner = null;
            statusMessage = "Stalemate. Game is drawn.";
            return;
        }
        if (halfMoveClock >= 100) {
            status = GameStatus.DRAW;
            winner = null;
            statusMessage = "Draw by fifty-move rule.";
            return;
        }
        if (positionCounts.getOrDefault(positionKey(), 0) >= 3) {
            status = GameStatus.DRAW;
            winner = null;
            statusMessage = "Draw by threefold repetition.";
            return;
        }
        if (hasInsufficientMaterial()) {
            status = GameStatus.DRAW;
            winner = null;
            statusMessage = "Draw by insufficient material.";
            return;
        }
        status = check ? GameStatus.CHECK : GameStatus.ACTIVE;
        statusMessage = check ? next + " is in check." : next + " to move.";
    }

    public synchronized boolean isGameOver() {
        return status == GameStatus.CHECKMATE || status == GameStatus.STALEMATE || status == GameStatus.DRAW
                || status == GameStatus.RESIGNED || status == GameStatus.DISCONNECTED;
    }

    public synchronized Piece[][] getBoardCopy() {
        return cloneBoard(board);
    }

    public synchronized PieceColor getTurn() { return turn; }
    public synchronized GameStatus getStatus() { return status; }
    public synchronized String getStatusMessage() { return statusMessage; }
    public synchronized PieceColor getWinner() { return winner; }
    public synchronized List<MoveRecord> getHistoryCopy() { return new ArrayList<>(history); }

    public synchronized String toStateMessage() {
        return "STATE|" + turn + "|" + status + "|" + (winner == null ? "NONE" : winner)
                + "|" + escape(statusMessage) + "|" + halfMoveClock + "|" + fullMoveNumber + "|" + serializeBoard();
    }

    public static String escape(String s) { return s == null ? "" : s.replace("|", "/"); }

    public synchronized String serializeBoard() {
        StringBuilder sb = new StringBuilder(64);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) sb.append(pieceToChar(board[y][x]));
        }
        return sb.toString();
    }

    public static Piece[][] deserializeBoard(String encoded) {
        if (encoded == null || encoded.length() != 64) throw new IllegalArgumentException("Invalid board state");
        Piece[][] b = new Piece[8][8];
        for (int i = 0; i < 64; i++) b[i / 8][i % 8] = charToPiece(encoded.charAt(i));
        return b;
    }

    private static char pieceToChar(Piece p) {
        if (p == null) return '.';
        char c = switch (p.type) {
            case KING -> 'k'; case QUEEN -> 'q'; case ROOK -> 'r'; case BISHOP -> 'b'; case KNIGHT -> 'n'; case PAWN -> 'p';
        };
        return p.color == PieceColor.WHITE ? Character.toUpperCase(c) : c;
    }

    private static Piece charToPiece(char c) {
        if (c == '.') return null;
        PieceColor color = Character.isUpperCase(c) ? PieceColor.WHITE : PieceColor.BLACK;
        return new Piece(switch (Character.toLowerCase(c)) {
            case 'k' -> PieceType.KING; case 'q' -> PieceType.QUEEN; case 'r' -> PieceType.ROOK;
            case 'b' -> PieceType.BISHOP; case 'n' -> PieceType.KNIGHT; case 'p' -> PieceType.PAWN;
            default -> throw new IllegalArgumentException("Unknown piece char");
        }, color, true);
    }

    public static boolean isLegalMove(Piece[][] b, Move m, PieceColor player, MoveRecord last) {
        if (!isBasicMoveValid(b, m, player, last)) return false;
        Piece[][] test = cloneBoard(b);
        applyMoveInternal(test, m, false);
        return !isKingInCheck(test, player);
    }

    public static boolean isBasicMoveValid(Piece[][] b, Move m, PieceColor player, MoveRecord last) {
        if (b == null || !m.isInsideBoard()) return false;
        if (m.sx == m.dx && m.sy == m.dy) return false;
        Piece p = b[m.sy][m.sx];
        if (p == null || p.color != player) return false;
        Piece target = b[m.dy][m.dx];
        if (target != null && target.color == player) return false;

        int dx = m.dx - m.sx;
        int dy = m.dy - m.sy;
        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);
        int dir = player == PieceColor.WHITE ? -1 : 1;

        return switch (p.type) {
            case PAWN -> pawnMoveValid(b, m, player, last, dx, dy, dir, target);
            case ROOK -> (dx == 0 || dy == 0) && isPathClear(b, m);
            case BISHOP -> absDx == absDy && isPathClear(b, m);
            case QUEEN -> (dx == 0 || dy == 0 || absDx == absDy) && isPathClear(b, m);
            case KNIGHT -> absDx * absDy == 2;
            case KING -> (absDx <= 1 && absDy <= 1) || castlingValid(b, m, player);
        };
    }

    private static boolean pawnMoveValid(Piece[][] b, Move m, PieceColor player, MoveRecord last, int dx, int dy, int dir, Piece target) {
        if (dx == 0 && dy == dir && target == null) return true;
        int startRank = player == PieceColor.WHITE ? 6 : 1;
        if (dx == 0 && dy == 2 * dir && m.sy == startRank && target == null && b[m.sy + dir][m.sx] == null) return true;
        if (Math.abs(dx) == 1 && dy == dir && target != null && target.color != player) return true;
        return isEnPassantValid(b, m, player, last, dx, dy, dir);
    }

    private static boolean isEnPassantValid(Piece[][] b, Move m, PieceColor player, MoveRecord last, int dx, int dy, int dir) {
        if (Math.abs(dx) != 1 || dy != dir || b[m.dy][m.dx] != null || last == null) return false;
        Piece adjacent = b[m.sy][m.dx];
        return adjacent != null && adjacent.type == PieceType.PAWN && adjacent.color == player.opposite()
                && last.movedPiece.type == PieceType.PAWN
                && last.move.dx == m.dx && last.move.dy == m.sy
                && Math.abs(last.move.dy - last.move.sy) == 2;
    }

    private static boolean castlingValid(Piece[][] b, Move m, PieceColor player) {
        Piece king = b[m.sy][m.sx];
        if (king == null || king.type != PieceType.KING || king.moved || m.sy != m.dy || Math.abs(m.dx - m.sx) != 2) return false;
        if (isKingInCheck(b, player)) return false;
        int rookX = m.dx > m.sx ? 7 : 0;
        int step = m.dx > m.sx ? 1 : -1;
        Piece rook = b[m.sy][rookX];
        if (rook == null || rook.type != PieceType.ROOK || rook.color != player || rook.moved) return false;
        for (int x = m.sx + step; x != rookX; x += step) if (b[m.sy][x] != null) return false;
        if (isSquareAttacked(b, m.sx + step, m.sy, player.opposite())) return false;
        return !isSquareAttacked(b, m.sx + 2 * step, m.sy, player.opposite());
    }

    private static MoveRecord applyMoveInternal(Piece[][] b, Move m, boolean mutateMovedFlag) {
        Piece moving = b[m.sy][m.sx];
        Piece captured = b[m.dy][m.dx];
        boolean castling = moving.type == PieceType.KING && Math.abs(m.dx - m.sx) == 2;
        boolean enPassant = moving.type == PieceType.PAWN && m.sx != m.dx && captured == null;
        if (enPassant) {
            captured = b[m.sy][m.dx];
            b[m.sy][m.dx] = null;
        }
        b[m.sy][m.sx] = null;
        Piece placed = moving;
        PieceType promotion = null;
        if (moving.type == PieceType.PAWN && (m.dy == 0 || m.dy == 7)) {
            promotion = m.promotion == null ? PieceType.QUEEN : m.promotion;
            placed = new Piece(promotion, moving.color, true);
        } else if (mutateMovedFlag) {
            moving.moved = true;
        } else {
            placed = moving.copy();
            placed.moved = true;
        }
        b[m.dy][m.dx] = placed;
        if (castling) {
            int rookFromX = m.dx > m.sx ? 7 : 0;
            int rookToX = m.dx > m.sx ? m.dx - 1 : m.dx + 1;
            Piece rook = b[m.sy][rookFromX];
            b[m.sy][rookFromX] = null;
            if (rook != null) rook.moved = true;
            b[m.sy][rookToX] = rook;
        }
        String note = coord(m.sx, m.sy) + "-" + coord(m.dx, m.dy) + (promotion == null ? "" : "=" + promotion);
        return new MoveRecord(m, moving.copy(), captured == null ? null : captured.copy(), castling, enPassant, promotion, note);
    }

    private static String coord(int x, int y) { return "abcdefgh".charAt(x) + String.valueOf(8 - y); }

    private static boolean isPathClear(Piece[][] b, Move m) {
        int xStep = Integer.compare(m.dx, m.sx);
        int yStep = Integer.compare(m.dy, m.sy);
        int x = m.sx + xStep, y = m.sy + yStep;
        while (x != m.dx || y != m.dy) {
            if (!Move.insideBoard(x, y) || b[y][x] != null) return false;
            x += xStep; y += yStep;
        }
        return true;
    }

    public static boolean isKingInCheck(Piece[][] b, PieceColor color) {
        int[] king = findKing(b, color);
        return king == null || isSquareAttacked(b, king[0], king[1], color.opposite());
    }

    private static boolean isSquareAttacked(Piece[][] b, int tx, int ty, PieceColor byColor) {
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            Piece p = b[y][x];
            if (p != null && p.color == byColor && attacksSquare(b, x, y, tx, ty)) return true;
        }
        return false;
    }

    private static boolean attacksSquare(Piece[][] b, int sx, int sy, int tx, int ty) {
        Piece p = b[sy][sx];
        int dx = tx - sx, dy = ty - sy, absDx = Math.abs(dx), absDy = Math.abs(dy);
        int dir = p.color == PieceColor.WHITE ? -1 : 1;
        return switch (p.type) {
            case PAWN -> absDx == 1 && dy == dir;
            case KNIGHT -> absDx * absDy == 2;
            case BISHOP -> absDx == absDy && isPathClear(b, new Move(sx, sy, tx, ty));
            case ROOK -> (dx == 0 || dy == 0) && isPathClear(b, new Move(sx, sy, tx, ty));
            case QUEEN -> (dx == 0 || dy == 0 || absDx == absDy) && isPathClear(b, new Move(sx, sy, tx, ty));
            case KING -> absDx <= 1 && absDy <= 1;
        };
    }

    public static int[] findKing(Piece[][] b, PieceColor color) {
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            Piece p = b[y][x];
            if (p != null && p.type == PieceType.KING && p.color == color) return new int[]{x, y};
        }
        return null;
    }

    public static Piece[][] cloneBoard(Piece[][] b) {
        Piece[][] copy = new Piece[8][8];
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) if (b[y][x] != null) copy[y][x] = b[y][x].copy();
        return copy;
    }

    public static boolean hasAnyLegalMove(Piece[][] b, PieceColor player, MoveRecord last) {
        for (int sy = 0; sy < 8; sy++) for (int sx = 0; sx < 8; sx++) {
            Piece p = b[sy][sx];
            if (p != null && p.color == player) {
                for (int dy = 0; dy < 8; dy++) for (int dx = 0; dx < 8; dx++) {
                    Move m = new Move(sx, sy, dx, dy, PieceType.QUEEN);
                    if (isLegalMove(b, m, player, last)) return true;
                }
            }
        }
        return false;
    }

    private boolean hasInsufficientMaterial() {
        List<Piece> pieces = new ArrayList<>();
        for (Piece[] row : board) for (Piece p : row) if (p != null) pieces.add(p);
        if (pieces.size() == 2) return true;
        if (pieces.size() == 3) return pieces.stream().anyMatch(p -> p.type == PieceType.BISHOP || p.type == PieceType.KNIGHT);
        return false;
    }

    private void rememberPosition() { positionCounts.merge(positionKey(), 1, Integer::sum); }

    private String positionKey() {
        StringBuilder sb = new StringBuilder(80);
        sb.append(serializeBoard()).append('|').append(turn).append('|');
        sb.append(canCastle(PieceColor.WHITE, true)).append(canCastle(PieceColor.WHITE, false));
        sb.append(canCastle(PieceColor.BLACK, true)).append(canCastle(PieceColor.BLACK, false));
        if (lastMove != null && lastMove.movedPiece.type == PieceType.PAWN && Math.abs(lastMove.move.dy - lastMove.move.sy) == 2) {
            sb.append("|ep").append(lastMove.move.dx).append(lastMove.move.dy);
        }
        return sb.toString();
    }

    private boolean canCastle(PieceColor color, boolean kingSide) {
        int y = color == PieceColor.WHITE ? 7 : 0;
        int rookX = kingSide ? 7 : 0;
        Piece k = board[y][4], r = board[y][rookX];
        return k != null && r != null && k.type == PieceType.KING && r.type == PieceType.ROOK && !k.moved && !r.moved;
    }
}
