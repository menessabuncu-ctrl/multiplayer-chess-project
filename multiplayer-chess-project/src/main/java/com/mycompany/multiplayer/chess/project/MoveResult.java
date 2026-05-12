
package com.mycompany.multiplayer.chess.project;

// Bir hamle denemesinin sonucunu temsil eder. Hamlenin başarılı ya da başarısız olması ve oyun durumu bilgisini taşır. 
public class MoveResult {

    // Hamlenin geçerli olup olmadığını belirtmek için.
    public final boolean success;

    // Kullanıcıya veya client'a gösterilecek açıklama mesajı.
    public final String message;

    // Hamleden sonraki oyun durumu.(gamestatus ile bağlı bu)
    public final GameStatus status;

    // Constructor private tutulur.Böylece MoveResult sadece ok() veya fail() methodlarıyla kontrollü oluşur.
    private MoveResult(boolean success, String message, GameStatus status) {
        this.success = success;
        this.message = message;
        this.status = status;
    }

    // Başarılı hamle için sonuç.
    public static MoveResult ok(String message, GameStatus status) {
        return new MoveResult(true, message, status);
    }

    // Başarısız hamle için sonuç ancak başarısız hamlede oyun aktif kalmaya devam eder.
    public static MoveResult fail(String message) {
        return new MoveResult(false, message, GameStatus.ACTIVE);
    }
}
