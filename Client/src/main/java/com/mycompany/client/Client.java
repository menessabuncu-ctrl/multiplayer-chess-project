
package com.mycompany.client;

import com.mycompany.GameLogic.*;
import javax.swing.*;
import java.io.*;
import java.net.*;

// Client class'ı oyuncunun server'a bağlanmasını sağlar.
// Server'dan gelen mesajları dinler ve GUI tarafına aktarır.
// Oyuncunun yaptığı hamleleri de server'a gönderir.
public class Client {

    // Server bağlantısı için socket.
    private Socket socket;

    // Server'dan mesaj okumak için kullanılır.
    private BufferedReader in;

    // Server'a mesaj göndermek için kullanılır.
    private PrintWriter out;

    // Client bağlantısı açık mı bilgisini tutar.
    private volatile boolean connected = false;

    // Oyuncunun rengi.
    // Server bağlantıdan sonra COLOR mesajı ile belirler.
    private PieceColor playerColor;

    // Client tarafında görünen güncel tahta.
    // Asıl oyun durumu server'dadır, bu sadece görüntüleme için kullanılır.
    private Piece[][] board;

    // Server'dan gelen güncel sıra bilgisi.
    private PieceColor turn;

    // Server'dan gelen güncel oyun durumu.
    private GameStatus status;

    // Server'dan gelen durum mesajı.
    private String statusMessage;

    // Oyun bittiyse kazanan oyuncu.
    private PieceColor winner;

    // GUI ekranı.
    private GameFrame gameFrame;

    // Client uygulamasının başlangıç noktası.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StartScreen().setVisible(true));
    }

    // Server'a bağlanır.
    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            connected = true;

            // Server'dan gelen mesajları ayrı thread üzerinde dinler.
            Thread listenerThread = new Thread(this::listenToServer);
            listenerThread.start();

            return true;

        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Could not connect to server: " + e.getMessage(),
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return false;
        }
    }

    // Server'dan gelen mesajları sürekli dinler.
    private void listenToServer() {
        try {
            String message;

            while (connected && (message = in.readLine()) != null) {
                handleServerMessage(message);
            }

        } catch (IOException e) {
            if (connected) {
                showInfo("Connection lost: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    // Server'dan gelen mesajları komut türüne göre işler.
    private void handleServerMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        System.out.println("SERVER -> " + message);

        String[] parts = message.split("\\|", -1);

        switch (parts[0]) {
            case "COLOR" -> handleColor(parts);
            case "STATE" -> handleState(parts);
            case "INFO" -> handleInfo(parts);
            case "ERROR" -> handleError(parts);
            case "DRAW_OFFER" -> handleDrawOffer(parts);
            case "GAME_OVER" -> handleGameOver(parts);
            default -> System.out.println("Unknown server message: " + message);
        }
    }

    // Server'ın gönderdiği oyuncu rengini kaydeder.
    private void handleColor(String[] parts) {
        if (parts.length < 2) {
            return;
        }

        playerColor = PieceColor.valueOf(parts[1]);

        SwingUtilities.invokeLater(() -> {
            if (gameFrame != null) {
                gameFrame.setPlayerColor(playerColor);
                gameFrame.updateStatus("You are " + playerColor);
            }
        });
    }

    // Server'ın gönderdiği güncel oyun durumunu işler.
    private void handleState(String[] parts) {
        // STATE|turn|status|winner|message|halfMoveClock|fullMoveNumber|board
        if (parts.length < 8) {
            return;
        }

        try {
            turn = PieceColor.valueOf(parts[1]);
            status = GameStatus.valueOf(parts[2]);

            winner = "NONE".equals(parts[3]) ? null : PieceColor.valueOf(parts[3]);
            statusMessage = parts[4];

            String boardString = parts[7];
            board = GameState.deserializeBoard(boardString);

            SwingUtilities.invokeLater(() -> {
                if (gameFrame != null) {
                    gameFrame.updateBoard(board);
                    gameFrame.updateTurn(turn);
                    gameFrame.updateStatus(statusMessage);
                }
            });

        } catch (Exception e) {
            System.out.println("Invalid STATE message: " + e.getMessage());
        }
    }

    // Server'ın gönderdiği bilgi mesajını GUI'de gösterir.
    private void handleInfo(String[] parts) {
        if (parts.length < 2) {
            return;
        }

        String msg = parts[1];

        SwingUtilities.invokeLater(() -> {
            if (gameFrame != null) {
                gameFrame.updateStatus(msg);
            }
        });
    }

    // Server'ın gönderdiği hata mesajını kullanıcıya gösterir.
    private void handleError(String[] parts) {
        if (parts.length < 2) {
            return;
        }

        String msg = parts[1];

        SwingUtilities.invokeLater(() -> {
            if (gameFrame != null) {
                gameFrame.updateStatus(msg);
            }

            JOptionPane.showMessageDialog(
                    gameFrame,
                    msg,
                    "Move Error",
                    JOptionPane.WARNING_MESSAGE
            );
        });
    }

    // Rakip beraberlik teklif ettiğinde kullanıcıya sorar.
    private void handleDrawOffer(String[] parts) {
        // DRAW_OFFER|color|message
        if (parts.length < 3) {
            return;
        }

        PieceColor offerBy = PieceColor.valueOf(parts[1]);
        String msg = parts[2];

        // Oyuncu kendi teklifini tekrar cevaplamaz.
        if (offerBy == playerColor) {
            SwingUtilities.invokeLater(() -> {
                if (gameFrame != null) {
                    gameFrame.updateStatus(msg);
                }
            });
            return;
        }

        SwingUtilities.invokeLater(() -> {
            int result = JOptionPane.showConfirmDialog(
                    gameFrame,
                    offerBy + " offered a draw. Accept?",
                    "Draw Offer",
                    JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION) {
                sendDrawAccept();
            } else {
                sendDrawDecline();
            }
        });
    }

    // Oyun bittiğinde EndScreen ekranını açar.
    private void handleGameOver(String[] parts) {
        // GAME_OVER|status|winner|message
        if (parts.length < 4) {
            return;
        }

        GameStatus finalStatus = GameStatus.valueOf(parts[1]);
        PieceColor finalWinner = "NONE".equals(parts[2]) ? null : PieceColor.valueOf(parts[2]);
        String message = parts[3];

        SwingUtilities.invokeLater(() -> {
            if (gameFrame != null) {
                gameFrame.updateStatus(message);
                gameFrame.setEnabled(false);
            }

            EndScreen endScreen = new EndScreen(this, finalStatus, finalWinner, message);
            endScreen.setVisible(true);
        });
    }

    // Hamleyi server'a gönderir.
    public void sendMove(Move move) {
        if (!connected || out == null) {
            showInfo("Not connected to server.");
            return;
        }

        if (move.promotion == null) {
            send("MOVE|" + move.sx + "|" + move.sy + "|" + move.dx + "|" + move.dy);
        } else {
            send("MOVE|" + move.sx + "|" + move.sy + "|" + move.dx + "|" + move.dy + "|" + move.promotion);
        }
    }

    // Server'a pes etme mesajı gönderir.
    public void sendResign() {
        send("RESIGN");
    }

    // Server'a beraberlik teklifi gönderir.
    public void sendDrawOffer() {
        send("DRAW_OFFER");
    }

    // Server'a beraberlik kabul mesajı gönderir.
    public void sendDrawAccept() {
        send("DRAW_ACCEPT");
    }

    // Server'a beraberlik reddetme mesajı gönderir.
    public void sendDrawDecline() {
        send("DRAW_DECLINE");
    }

    // Server'a oyunu yeniden başlatma mesajı gönderir.
    public void sendRestart() {
        send("RESTART");
    }

    // Server'a çıkış mesajı gönderir.
    public void sendQuit() {
        send("QUIT");
    }

    // Genel mesaj gönderme methodu.
    private void send(String message) {
        if (connected && out != null) {
            out.println(message);
        }
    }

    // Client bağlantısını kapatır.
    public void disconnect() {
        connected = false;

        try {
            if (out != null) {
                out.close();
            }

            if (in != null) {
                in.close();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            System.out.println("Error while disconnecting: " + e.getMessage());
        }
    }

    // Bilgi mesajını GUI'de veya dialog olarak gösterir.
    private void showInfo(String message) {
        SwingUtilities.invokeLater(() -> {
            if (gameFrame != null) {
                gameFrame.updateStatus(message);
            } else {
                JOptionPane.showMessageDialog(null, message);
            }
        });
    }

    // GameFrame bağlantısını Client içine verir.
    public void setGameFrame(GameFrame gameFrame) {
        this.gameFrame = gameFrame;
    }

    // Getter methodları.
    public boolean isConnected() {
        return connected;
    }

    public PieceColor getPlayerColor() {
        return playerColor;
    }

    public Piece[][] getBoard() {
        return board;
    }

    public PieceColor getTurn() {
        return turn;
    }

    public GameStatus getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public PieceColor getWinner() {
        return winner;
    }
}
