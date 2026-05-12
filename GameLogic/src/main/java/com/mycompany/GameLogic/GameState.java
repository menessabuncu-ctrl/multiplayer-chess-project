
package com.mycompany.GameLogic;


import java.util.*;

// Oyunun ana mantığını tutan class.
// Tahta durumu, sıra, hamleler, Checkmate, pat, Draw, Castling, en passant ve piyon terfi etme burada yönetilir.
public class GameState {

    // 8x8 satranç tahtası.
    // board[y][x] şeklinde kullanılır y satırı, x sütunu temsil eder.
    private Piece[][] board;

    // renk sırasını tutar.
    private PieceColor turn;

    // Oyunun mevcut durumunu tutar.
    private GameStatus status;

    // Oyunculara gösterilecek durum mesajı.
    private String statusMessage;

    // Oyun bittiyse kazananı tutar.
    // Beraberlik varsa null olur.
    private PieceColor winner;

    // Son yapılan hamlenin kaydı.
    // En passant ve tekrar için kullanılır.
    private MoveRecord lastMove;

    // 50 hamle kuralı için kullanılır.
    private int halfMoveClock;

    // Tam hamle sayısı, siyahtan sonra artar.
    private int fullMoveNumber;

    // Oyun boyunca yapılan hamlelerin geçmişi.
    private final List<MoveRecord> history = new ArrayList<>();

    // Aynı pozisyon kaç kez oluştu, onu tutar(Üç tekrar beraberliği için).
    private final Map<String, Integer> positionCounts = new HashMap<>();

    // Beraberlik teklifinin kim tarafından yapıldığını tutar.
    private PieceColor drawOfferBy;

    // GameState oluşturulunca oyun başlangıç durumuna geçer.
    public GameState() {
        reset();
    }

    // Oyunu tamamen baştan başlatır.
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

        // İlk pozisyon da tekrar kontrolüne dahil edilir.
        rememberPosition();
    }

    // Satranç taşlarını başlangıç dizilimine göre tahtaya yerleştirir.
    public static Piece[][] initialBoard() {
        Piece[][] b = new Piece[8][8];

        // Siyah piyonlar 1. satıra, beyaz piyonlar 6. satıra yerleştirilir.
        for (int i = 0; i < 8; i++) {
            b[1][i] = new Piece(PieceType.PAWN, PieceColor.BLACK);
            b[6][i] = new Piece(PieceType.PAWN, PieceColor.WHITE);
        }

        // Kaleler
        b[0][0] = new Piece(PieceType.ROOK, PieceColor.BLACK);
        b[0][7] = new Piece(PieceType.ROOK, PieceColor.BLACK);
        b[7][0] = new Piece(PieceType.ROOK, PieceColor.WHITE);
        b[7][7] = new Piece(PieceType.ROOK, PieceColor.WHITE);

        // Atlar
        b[0][1] = new Piece(PieceType.KNIGHT, PieceColor.BLACK);
        b[0][6] = new Piece(PieceType.KNIGHT, PieceColor.BLACK);
        b[7][1] = new Piece(PieceType.KNIGHT, PieceColor.WHITE);
        b[7][6] = new Piece(PieceType.KNIGHT, PieceColor.WHITE);

        // Filler
        b[0][2] = new Piece(PieceType.BISHOP, PieceColor.BLACK);
        b[0][5] = new Piece(PieceType.BISHOP, PieceColor.BLACK);
        b[7][2] = new Piece(PieceType.BISHOP, PieceColor.WHITE);
        b[7][5] = new Piece(PieceType.BISHOP, PieceColor.WHITE);

        // Vezirler
        b[0][3] = new Piece(PieceType.QUEEN, PieceColor.BLACK);
        b[7][3] = new Piece(PieceType.QUEEN, PieceColor.WHITE);

        // Şahlar
        b[0][4] = new Piece(PieceType.KING, PieceColor.BLACK);
        b[7][4] = new Piece(PieceType.KING, PieceColor.WHITE);

        return b;
    }

    // Oyuncudan gelen hamleyi işler ve server tarafında asıl hamle doğrulamasını yapar.
    public synchronized MoveResult playMove(Move move, PieceColor player) {
        if (isGameOver()) return MoveResult.fail("Game is already over");

        // Sıra kontrolü.
        if (player != turn) return MoveResult.fail("It is not your turn");

        // Hamlenin tahta sınırları içinde olup olmadığı kontrolü.
        if (!move.isInsideBoard()) return MoveResult.fail("Move is outside board bounds");

        Piece piece = board[move.sy][move.sx];

        // Seçilen karede taş yoksa hamle geçersizdir.
        if (piece == null) return MoveResult.fail("No piece on selected square");

        // Oyuncu sadece kendi taşını oynayabilir.
        if (piece.color != player) return MoveResult.fail("Selected piece belongs to opponent");

        // Satranç kurallarına göre hamle geçerli mi kontrol edilir.
        if (!isLegalMove(board, move, player, lastMove)) return MoveResult.fail("Illegal move");

        // Hamle tahtaya uygulanır.
        MoveRecord record = applyMoveInternal(board, move, true);

        // Son hamle ve hamle geçmişi güncellenir.
        lastMove = record;
        history.add(record);

        // Bir hamle yapıldığı için aktif beraberlik teklifi temizlenir.
        drawOfferBy = null;

        // Piyon hareketi veya taş yeme varsa 50 hamle sayacı sıfırlanır.
        if (record.movedPiece.type == PieceType.PAWN || record.capturedPiece != null) halfMoveClock = 0;
        else halfMoveClock++;

        // Satrançta tam hamle sayısı siyah oynadıktan sonra artar.
        if (player == PieceColor.BLACK) fullMoveNumber++;

        // Sıra rakibe geçer.
        turn = turn.opposite();

        // Yeni pozisyon tekrar kontrolü için kaydedilir.
        rememberPosition();

        // Hamleden sonra şah, mat, pat veya beraberlik var mı kontrol edilir.
        updateGameStatusAfterMove(player, turn);

        return MoveResult.ok(statusMessage, status);
    }

    // Oyuncunun pes etmesini işler.
    public synchronized void resign(PieceColor player) {
        status = GameStatus.RESIGNED;
        winner = player.opposite();
        statusMessage = player + " resigned. " + winner + " wins.";
    }

    // Oyuncu beraberlik teklif eder.
    public synchronized String offerDraw(PieceColor player) {
        if (isGameOver()) return "Game is already over";

        drawOfferBy = player;
        statusMessage = player + " offered a draw.";
        return statusMessage;
    }

    // Karşı oyuncu beraberlik teklifine cevap verir.
    public synchronized String respondDraw(PieceColor player, boolean accepted) {
        if (drawOfferBy == null) return "There is no active draw offer.";

        // Oyuncu kendi teklifini kabul veya reddedemez.
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

    // Rakibin bağlantısı koparsa oyun bitirilir.
    public synchronized void opponentDisconnected(PieceColor disconnected) {
        if (!isGameOver()) {
            status = GameStatus.DISCONNECTED;
            winner = disconnected.opposite();
            statusMessage = disconnected + " disconnected. " + winner + " wins.";
        }
    }

    // Her hamleden sonra oyunun yeni durumunu belirler.
    private void updateGameStatusAfterMove(PieceColor mover, PieceColor next) {
        boolean check = isKingInCheck(board, next);
        boolean anyMove = hasAnyLegalMove(board, next, lastMove);

        // Şah tehdit altındaysa ve oynanacak hiçbir legal hamle yoksa mat olur.
        if (check && !anyMove) {
            status = GameStatus.CHECKMATE;
            winner = mover;
            statusMessage = mover + " wins by checkmate.";
            return;
        }

        // Şah tehdit altında değil ama legal hamle yoksa pat olur.
        if (!check && !anyMove) {
            status = GameStatus.STALEMATE;
            winner = null;
            statusMessage = "Stalemate. Game is drawn.";
            return;
        }

        // 50 hamle kuralı.
        // 100 half-move = iki taraf toplam 50 hamle.
        if (halfMoveClock >= 100) {
            status = GameStatus.DRAW;
            winner = null;
            statusMessage = "Draw by fifty-move rule.";
            return;
        }

        // Aynı pozisyon üç kez oluşursa beraberlik.
        if (positionCounts.getOrDefault(positionKey(), 0) >= 3) {
            status = GameStatus.DRAW;
            winner = null;
            statusMessage = "Draw by threefold repetition.";
            return;
        }

        // Mat edecek yeterli taş kalmadıysa beraberlik.
        if (hasInsufficientMaterial()) {
            status = GameStatus.DRAW;
            winner = null;
            statusMessage = "Draw by insufficient material.";
            return;
        }

        // Oyun devam ediyorsa status CHECK veya ACTIVE yapılır.
        status = check ? GameStatus.CHECK : GameStatus.ACTIVE;
        statusMessage = check ? next + " is in check." : next + " to move.";
    }

    // Oyunun bitip bitmediğini kontrol eder.
    public synchronized boolean isGameOver() {
        return status == GameStatus.CHECKMATE || status == GameStatus.STALEMATE || status == GameStatus.DRAW
                || status == GameStatus.RESIGNED || status == GameStatus.DISCONNECTED;
    }

    // Dışarıya gerçek board verilmez.
    // Kopya verilir ki client veya GUI asıl oyun durumunu bozamasın.
    public synchronized Piece[][] getBoardCopy() {
        return cloneBoard(board);
    }

    // Getter methodları.
    public synchronized PieceColor getTurn() { return turn; }
    public synchronized GameStatus getStatus() { return status; }
    public synchronized String getStatusMessage() { return statusMessage; }
    public synchronized PieceColor getWinner() { return winner; }
    public synchronized List<MoveRecord> getHistoryCopy() { return new ArrayList<>(history); }

    // Server'ın clientlara göndereceği oyun durum mesajını oluşturur.
    public synchronized String toStateMessage() {
        return "STATE|" + turn + "|" + status + "|" + (winner == null ? "NONE" : winner)
                + "|" + escape(statusMessage) + "|" + halfMoveClock + "|" + fullMoveNumber + "|" + serializeBoard();
    }

    // Protokolde | karakteri ayırıcı olduğu için mesaj içinde kullanılmasını engeller.
    public static String escape(String s) { return s == null ? "" : s.replace("|", "/"); }

    // Board'u 64 karakterlik stringe çevirir.
    // Bu string server-client arasında oyun durumu taşımak için kullanılır.
    public synchronized String serializeBoard() {
        StringBuilder sb = new StringBuilder(64);

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) sb.append(pieceToChar(board[y][x]));
        }

        return sb.toString();
    }

    // 64 karakterlik board bilgisini tekrar Piece[][] yapısına çevirir.
    public static Piece[][] deserializeBoard(String encoded) {
        if (encoded == null || encoded.length() != 64) throw new IllegalArgumentException("Invalid board state");

        Piece[][] b = new Piece[8][8];

        for (int i = 0; i < 64; i++) b[i / 8][i % 8] = charToPiece(encoded.charAt(i));

        return b;
    }

    // Piece nesnesini tek karakterle temsil eder.
    // Büyük harf beyaz, küçük harf siyah taşları ifade eder.
    private static char pieceToChar(Piece p) {
        if (p == null) return '.';

        char c = switch (p.type) {
            case KING -> 'k'; case QUEEN -> 'q'; case ROOK -> 'r'; case BISHOP -> 'b'; case KNIGHT -> 'n'; case PAWN -> 'p';
        };

        return p.color == PieceColor.WHITE ? Character.toUpperCase(c) : c;
    }

    // Tek karakterden Piece nesnesi üretir.
    private static Piece charToPiece(char c) {
        if (c == '.') return null;

        PieceColor color = Character.isUpperCase(c) ? PieceColor.WHITE : PieceColor.BLACK;

        return new Piece(switch (Character.toLowerCase(c)) {
            case 'k' -> PieceType.KING; case 'q' -> PieceType.QUEEN; case 'r' -> PieceType.ROOK;
            case 'b' -> PieceType.BISHOP; case 'n' -> PieceType.KNIGHT; case 'p' -> PieceType.PAWN;
            default -> throw new IllegalArgumentException("Unknown piece char");
        }, color, true);
    }

    // Hamlenin hem temel olarak geçerli olup olmadığını hem de şahı açıkta bırakıp bırakmadığını kontrol eder.
    public static boolean isLegalMove(Piece[][] b, Move m, PieceColor player, MoveRecord last) {
        if (!isBasicMoveValid(b, m, player, last)) return false;

        // Hamle kopya tahta üzerinde denenir.
        // Böylece gerçek oyun tahtası bozulmaz.
        Piece[][] test = cloneBoard(b);
        applyMoveInternal(test, m, false);

        // Hamleden sonra kendi şahı tehdit altında kalıyorsa hamle legal değildir.
        return !isKingInCheck(test, player);
    }

    // Taşın hareket kuralına göre hamlenin geçerli olup olmadığını kontrol eder.
    public static boolean isBasicMoveValid(Piece[][] b, Move m, PieceColor player, MoveRecord last) {
        if (b == null || !m.isInsideBoard()) return false;
        if (m.sx == m.dx && m.sy == m.dy) return false;

        Piece p = b[m.sy][m.sx];

        // Başlangıç karesinde oyuncuya ait taş olmalı.
        if (p == null || p.color != player) return false;

        Piece target = b[m.dy][m.dx];

        // Hedef karede kendi taşı varsa oraya gidilemez.
        if (target != null && target.color == player) return false;

        int dx = m.dx - m.sx;
        int dy = m.dy - m.sy;
        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);

        // Beyaz yukarı, siyah aşağı hareket eder.
        int dir = player == PieceColor.WHITE ? -1 : 1;

        // Taş türüne göre hareket kuralı seçilir.
        return switch (p.type) {
            case PAWN -> pawnMoveValid(b, m, player, last, dx, dy, dir, target);
            case ROOK -> (dx == 0 || dy == 0) && isPathClear(b, m);
            case BISHOP -> absDx == absDy && isPathClear(b, m);
            case QUEEN -> (dx == 0 || dy == 0 || absDx == absDy) && isPathClear(b, m);
            case KNIGHT -> absDx * absDy == 2;
            case KING -> (absDx <= 1 && absDy <= 1) || castlingValid(b, m, player);
        };
    }

    // Piyon hareketlerini kontrol eder.
    private static boolean pawnMoveValid(Piece[][] b, Move m, PieceColor player, MoveRecord last, int dx, int dy, int dir, Piece target) {

        // Piyon bir kare düz gidebilir.
        if (dx == 0 && dy == dir && target == null) return true;

        int startRank = player == PieceColor.WHITE ? 6 : 1;

        // Piyon ilk hamlesinde iki kare ilerleyebilir.
        if (dx == 0 && dy == 2 * dir && m.sy == startRank && target == null && b[m.sy + dir][m.sx] == null) return true;

        // Piyon çapraz taş yiyebilir.
        if (Math.abs(dx) == 1 && dy == dir && target != null && target.color != player) return true;

        // En passant özel hamlesi kontrol edilir.
        return isEnPassantValid(b, m, player, last, dx, dy, dir);
    }

    // En passant hamlesinin geçerli olup olmadığını kontrol eder.
    private static boolean isEnPassantValid(Piece[][] b, Move m, PieceColor player, MoveRecord last, int dx, int dy, int dir) {
        if (Math.abs(dx) != 1 || dy != dir || b[m.dy][m.dx] != null || last == null) return false;

        Piece adjacent = b[m.sy][m.dx];

        return adjacent != null && adjacent.type == PieceType.PAWN && adjacent.color == player.opposite()
                && last.movedPiece.type == PieceType.PAWN
                && last.move.dx == m.dx && last.move.dy == m.sy
                && Math.abs(last.move.dy - last.move.sy) == 2;
    }

    // castling hamlesinin geçerli olup olmadığını kontrol eder.
    private static boolean castlingValid(Piece[][] b, Move m, PieceColor player) {
        Piece king = b[m.sy][m.sx];

        // Şah daha önce hareket etmişse castling yapılamaz.
        if (king == null || king.type != PieceType.KING || king.moved || m.sy != m.dy || Math.abs(m.dx - m.sx) != 2) return false;

        // Şah tehdit altındayken castling yapılamaz.
        if (isKingInCheck(b, player)) return false;

        int rookX = m.dx > m.sx ? 7 : 0;
        int step = m.dx > m.sx ? 1 : -1;

        Piece rook = b[m.sy][rookX];

        // castling yapılacak kalenin de daha önce hareket etmemiş olması gerekir.
        if (rook == null || rook.type != PieceType.ROOK || rook.color != player || rook.moved) return false;

        // Şah ile kale arasındaki kareler boş olmalı.
        for (int x = m.sx + step; x != rookX; x += step) if (b[m.sy][x] != null) return false;

        // Şahın geçeceği kare tehdit altında olamaz.
        if (isSquareAttacked(b, m.sx + step, m.sy, player.opposite())) return false;

        // Şahın varacağı kare de tehdit altında olamaz.
        return !isSquareAttacked(b, m.sx + 2 * step, m.sy, player.opposite());
    }

    // Hamleyi tahtaya uygular.
    // mutateMovedFlag true ise gerçek oyun tahtası güncelleniyor demektir.
    // false ise sadece simülasyon yapılıyordur.
    private static MoveRecord applyMoveInternal(Piece[][] b, Move m, boolean mutateMovedFlag) {
        Piece moving = b[m.sy][m.sx];
        Piece captured = b[m.dy][m.dx];

        boolean castling = moving.type == PieceType.KING && Math.abs(m.dx - m.sx) == 2;
        boolean enPassant = moving.type == PieceType.PAWN && m.sx != m.dx && captured == null;

        // En passant durumunda hedef kare boş görünür.
        // Asıl yenilen piyon yan kareden kaldırılır.
        if (enPassant) {
            captured = b[m.sy][m.dx];
            b[m.sy][m.dx] = null;
        }

        // Taş eski konumundan kaldırılır.
        b[m.sy][m.sx] = null;

        Piece placed = moving;
        PieceType promotion = null;

        // Piyon son sıraya ulaşırsa terfi eder.
        if (moving.type == PieceType.PAWN && (m.dy == 0 || m.dy == 7)) {
            promotion = m.promotion == null ? PieceType.QUEEN : m.promotion;
            placed = new Piece(promotion, moving.color, true);
        } else if (mutateMovedFlag) {
            // Gerçek hamlede taşın hareket ettiği işaretlenir.
            moving.moved = true;
        } else {
            // Simülasyonda asıl taş bozulmasın diye kopya kullanılır.
            placed = moving.copy();
            placed.moved = true;
        }

        // Taş hedef kareye yerleştirilir.
        b[m.dy][m.dx] = placed;

        // castling hamlesinde kale de hareket ettirilir.
        if (castling) {
            int rookFromX = m.dx > m.sx ? 7 : 0;
            int rookToX = m.dx > m.sx ? m.dx - 1 : m.dx + 1;

            Piece rook = b[m.sy][rookFromX];

            b[m.sy][rookFromX] = null;

            if (rook != null) rook.moved = true;

            b[m.sy][rookToX] = rook;
        }

        // Hamle notasyonu oluşturulur.
        String note = coord(m.sx, m.sy) + "-" + coord(m.dx, m.dy) + (promotion == null ? "" : "=" + promotion);

        // Hamle geçmişi için MoveRecord döndürülür.
        return new MoveRecord(m, moving.copy(), captured == null ? null : captured.copy(), castling, enPassant, promotion, note);
    }

    // x,y koordinatını satranç gösterimine çevirir.
    // Örneğin 0,7 -> a1
    private static String coord(int x, int y) { return "abcdefgh".charAt(x) + String.valueOf(8 - y); }

    // Kale, fil ve vezir gibi çizgisel hareket eden taşlar için yolun boş olup olmadığını kontrol eder.
    private static boolean isPathClear(Piece[][] b, Move m) {
        int xStep = Integer.compare(m.dx, m.sx);
        int yStep = Integer.compare(m.dy, m.sy);

        int x = m.sx + xStep, y = m.sy + yStep;

        while (x != m.dx || y != m.dy) {
            if (!Move.insideBoard(x, y) || b[y][x] != null) return false;

            x += xStep;
            y += yStep;
        }

        return true;
    }

    // Belirli renkteki şahın tehdit altında olup olmadığını kontrol eder.
    public static boolean isKingInCheck(Piece[][] b, PieceColor color) {
        int[] king = findKing(b, color);

        // Şah bulunamazsa güvenlik için check kabul edilir.
        return king == null || isSquareAttacked(b, king[0], king[1], color.opposite());
    }

    // Belirli bir karenin rakip tarafından saldırı altında olup olmadığını kontrol eder.
    private static boolean isSquareAttacked(Piece[][] b, int tx, int ty, PieceColor byColor) {
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            Piece p = b[y][x];

            if (p != null && p.color == byColor && attacksSquare(b, x, y, tx, ty)) return true;
        }

        return false;
    }

    // Bir taşın belirli hedef kareye saldırıp saldıramayacağını kontrol eder.
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

    // Tahta üzerinde belirtilen renkteki şahı bulur.
    public static int[] findKing(Piece[][] b, PieceColor color) {
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            Piece p = b[y][x];

            if (p != null && p.type == PieceType.KING && p.color == color) return new int[]{x, y};
        }

        return null;
    }

    // Tahtanın derin kopyasını oluşturur.
    // Her Piece de ayrıca kopyalanır.
    public static Piece[][] cloneBoard(Piece[][] b) {
        Piece[][] copy = new Piece[8][8];

        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) if (b[y][x] != null) copy[y][x] = b[y][x].copy();

        return copy;
    }

    // Belirli oyuncunun oynayabileceği herhangi bir legal hamle var mı kontrol eder.
    // Mat ve pat kontrolünde kullanılır.
    public static boolean hasAnyLegalMove(Piece[][] b, PieceColor player, MoveRecord last) {
        for (int sy = 0; sy < 8; sy++) for (int sx = 0; sx < 8; sx++) {
            Piece p = b[sy][sx];

            if (p != null && p.color == player) {
                for (int dy = 0; dy < 8; dy++) for (int dx = 0; dx < 8; dx++) {
                    // Piyon terfisi ihtimali için varsayılan QUEEN verilir.
                    Move m = new Move(sx, sy, dx, dy, PieceType.QUEEN);

                    if (isLegalMove(b, m, player, last)) return true;
                }
            }
        }

        return false;
    }

    // Tahta üzerinde mat yapmaya yeterli taş kalıp kalmadığını kontrol eder.
    private boolean hasInsufficientMaterial() {
        List<Piece> pieces = new ArrayList<>();

        for (Piece[] row : board) for (Piece p : row) if (p != null) pieces.add(p);

        // Sadece iki şah kaldıysa beraberliktir.
        if (pieces.size() == 2) return true;

        // Şah + tek fil veya şah + tek at durumunda da mat materyali yetersiz kabul edilir.
        if (pieces.size() == 3) return pieces.stream().anyMatch(p -> p.type == PieceType.BISHOP || p.type == PieceType.KNIGHT);

        return false;
    }

    // Mevcut pozisyonu tekrar sayacı içine kaydeder.
    private void rememberPosition() { positionCounts.merge(positionKey(), 1, Integer::sum); }

    // Pozisyonu tekrar kontrolü için benzersiz metin anahtarına çevirir.
    private String positionKey() {
        StringBuilder sb = new StringBuilder(80);

        sb.append(serializeBoard()).append('|').append(turn).append('|');

        // Castling hakları pozisyon anahtarına eklenir.
        sb.append(canCastle(PieceColor.WHITE, true)).append(canCastle(PieceColor.WHITE, false));
        sb.append(canCastle(PieceColor.BLACK, true)).append(canCastle(PieceColor.BLACK, false));

        // En passant ihtimali varsa pozisyon anahtarına eklenir.
        if (lastMove != null && lastMove.movedPiece.type == PieceType.PAWN && Math.abs(lastMove.move.dy - lastMove.move.sy) == 2) {
            sb.append("|ep").append(lastMove.move.dx).append(lastMove.move.dy);
        }

        return sb.toString();
    }

    // Belirli tarafın kısa veya uzun castling hakkı olup olmadığını kontrol eder.
    private boolean canCastle(PieceColor color, boolean kingSide) {
        int y = color == PieceColor.WHITE ? 7 : 0;
        int rookX = kingSide ? 7 : 0;

        Piece k = board[y][4], r = board[y][rookX];

        return k != null && r != null && k.type == PieceType.KING && r.type == PieceType.ROOK && !k.moved && !r.moved;
    }
}
