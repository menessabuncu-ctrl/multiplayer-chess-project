package com.mycompany.client;

import com.mycompany.GameLogic.*;

import javax.swing.*;
import java.io.*;
import java.net.*;

// Server ile haberleşen ve gelen mesajları GUI tarafına aktaran client sınıfı.
public class Client {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private GameFrame gameFrame;
    private PieceColor role;
    private volatile boolean closing = false;

    // Servera bağlanır, oyuncu rolünü alır ve dinleme threadini başlatır.
    public Client(String ip) throws IOException {
        socket = new Socket(ip, 5000);
        socket.setKeepAlive(true);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        String roleMessage = in.readUTF();

        if (!roleMessage.startsWith("ROLE|")) {
            throw new IOException("Server did not assign a role");
        }

        role = PieceColor.valueOf(roleMessage.split("\\|")[1]);

        SwingUtilities.invokeLater(() -> gameFrame = new GameFrame(this, role));

        Thread listener = new Thread(this::listen, "client-listener");
        listener.setDaemon(true);
        listener.start();
    }

    // Serverdan gelen mesajları bağlantı açık kaldığı sürece dinler.
    private void listen() {
        try {
            while (!closing && socket != null && !socket.isClosed()) {
                String message = in.readUTF();
                SwingUtilities.invokeLater(() -> handleServerMessage(message));
            }
        } catch (EOFException | SocketException e) {
    if (closing || socket == null || socket.isClosed()) {
        return;
    }

    System.err.println("Connection closed unexpectedly: " + e.getMessage());

            SwingUtilities.invokeLater(() -> {
                if (gameFrame != null) {
                    if (gameFrame.isEndScreenShown()) {
                        gameFrame.disableReplayBecauseOpponentLeft(
                                "Opponent left the session. Replay is no longer available."
                        );
                    } else {
                        gameFrame.showConnectionError("Connection lost: " + e.getMessage());
                    }
                }
            });

        } catch (Exception e) {
            if (closing) {
                return;
            }

            System.err.println("Connection listener failed: " + e.getMessage());
            e.printStackTrace();

            SwingUtilities.invokeLater(() -> {
                if (gameFrame != null) {
                    if (gameFrame.isEndScreenShown()) {
                        gameFrame.disableReplayBecauseOpponentLeft(
                                "Opponent left the session. Replay is no longer available."
                        );
                    } else {
                        gameFrame.showConnectionError("Connection lost: " + e.getMessage());
                    }
                }
            });
        }
    }

    // Protokol mesajının tipine göre GUI üzerinde gerekli işlemi yapar.
    private void handleServerMessage(String message) {
        if (gameFrame == null) {
            return;
        }

        try {
            if (message.startsWith("STATE|")) {
                gameFrame.receiveState(message);

            } else if (message.startsWith("ERROR|")) {
                JOptionPane.showMessageDialog(
                        gameFrame,
                        message.substring("ERROR|".length()),
                        "Server rejected action",
                        JOptionPane.WARNING_MESSAGE
                );

            } else if (message.startsWith("REPLAY_OFFER|")) {
                String requester = message.substring("REPLAY_OFFER|".length());
                gameFrame.receiveReplayOffer(requester);

            } else if (message.equals("REPLAY_WAITING")) {
                gameFrame.showReplayWaiting();

            } else if (message.equals("REPLAY_DECLINED")) {
                gameFrame.showReplayDeclined();

            } else if (message.startsWith("REPLAY_UNAVAILABLE|")) {
                gameFrame.showReplayUnavailable(message.substring("REPLAY_UNAVAILABLE|".length()));

            } else if (message.equals("REPLAY_START")) {
                gameFrame.startReplayFromServer();
            }
        } catch (Exception e) {
            System.err.println("Failed to process server message: " + message);
            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    gameFrame,
                    "Invalid server message: " + e.getMessage(),
                    "Protocol Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public PieceColor getRole() {
        return role;
    }

    // Clienttan servera güvenli şekilde mesaj gönderir.
    public synchronized void send(String message) {
        try {
            if (closing) {
                return;
            }

            if (socket == null || socket.isClosed()) {
                throw new IOException("Socket is closed");
            }

            out.writeUTF(message);
            out.flush();

        } catch (Exception e) {
            System.err.println("Send failed: " + e.getMessage());
            e.printStackTrace();

            if (gameFrame != null) {
                JOptionPane.showMessageDialog(
                        gameFrame,
                        "Send failed: " + e.getMessage(),
                        "Network Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    // Yapılan hamleyi serverın beklediği protokol formatında yollar.
    public void sendMove(Move move) {
        send("MOVE|" + move.sx + "|" + move.sy + "|" + move.dx + "|" + move.dy + "|" + (move.promotion == null ? "" : move.promotion.name()));
    }

    public void resign() {
        send("RESIGN");
    }

    public void offerDraw() {
        send("DRAW_OFFER");
    }

    public void acceptDraw() {
        send("DRAW_ACCEPT");
    }

    public void declineDraw() {
        send("DRAW_DECLINE");
    }

    public void requestReplay() {
        send("REPLAY_REQUEST");
    }

    public void acceptReplay() {
        send("REPLAY_ACCEPT");
    }

    public void declineReplay() {
        send("REPLAY_DECLINE");
    }

    // Pencere kapandığında veya menüye dönüldüğünde socket bağlantısını kapatır.
    public void closeConnection() {
        closing = true;

        try {
            if (in != null) in.close();
        } catch (Exception ignored) {
        }

        try {
            if (out != null) out.close();
        } catch (Exception ignored) {
        }

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {
            System.err.println("Close failed: " + e.getMessage());
        }
    }

    public void exitApplication() {
        closeConnection();
        System.exit(0);
    }

    // Server socketini açar ve gelen oyuncuları ikili oturumlara ayırır.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(StartScreen::new);
    }
}