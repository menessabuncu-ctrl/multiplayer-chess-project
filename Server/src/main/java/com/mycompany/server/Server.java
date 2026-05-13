package com.mycompany.server;

import com.mycompany.GameLogic.*;

import java.io.*;
import java.net.*;

public class Server {
    private static final int PORT = 5000;
    private static int nextSessionId = 1;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT + "...");

            while (true) {
                System.out.println("Waiting for players...");

                Socket player1 = serverSocket.accept();
                Socket player2 = serverSocket.accept();

                int sessionId = nextSessionId++;

                System.out.println("[Session " + sessionId + "] White connected: " + player1.getRemoteSocketAddress());
                System.out.println("[Session " + sessionId + "] Black connected: " + player2.getRemoteSocketAddress());

                Thread session = new Thread(
                        new GameSession(sessionId, player1, player2),
                        "game-session-" + sessionId
                );

                System.out.println("[Session " + sessionId + "] Game session started.");

                session.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class GameSession implements Runnable {

        private final int sessionId;
        private final PlayerConnection white;
        private final PlayerConnection black;
        private final GameState game = new GameState();

        private volatile boolean running = true;
        private PieceColor replayRequestedBy = null;

        GameSession(int sessionId, Socket whiteSocket, Socket blackSocket) throws IOException {
            this.sessionId = sessionId;
            white = new PlayerConnection(whiteSocket, PieceColor.WHITE);
            black = new PlayerConnection(blackSocket, PieceColor.BLACK);
        }

        @Override
        public void run() {
            try {
                white.send("ROLE|WHITE");
                black.send("ROLE|BLACK");

                broadcastState();

                Thread t1 = new Thread(() -> listen(white), "white-listener");
                Thread t2 = new Thread(() -> listen(black), "black-listener");

                t1.start();
                t2.start();

                t1.join();
                t2.join();
            } catch (Exception e) {
                System.err.println("[Session " + sessionId + "] Game session error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                closeQuietly(white);
                closeQuietly(black);
                System.out.println("[Session " + sessionId + "] Game session closed.");
            }
        }

        private void listen(PlayerConnection player) {
            try {
                while (running) {
                    String message = player.in.readUTF();
                    handleMessage(player, message);
                }
            } catch (EOFException | SocketException e) {
                handleDisconnect(player);
            } catch (Exception e) {
                System.err.println("[Session " + sessionId + "] " + player.color + " listener error: " + e.getMessage());
                e.printStackTrace();
                handleDisconnect(player);
            }
        }

        private synchronized void handleMessage(PlayerConnection player, String message) {
            try {
                String[] parts = message.split("\\|");

                switch (parts[0]) {
                    case "MOVE" -> {
                        if (game.isGameOver()) {
                            player.send("ERROR|Game is over. Use Replay or Back to Menu.");
                            return;
                        }

                        Move move = Move.fromProtocol(parts);
                        MoveResult result = game.playMove(move, player.color);

                        if (!result.success) {
                            player.send("ERROR|" + GameState.escape(result.message));
                            return;
                        }

                        replayRequestedBy = null;
                        broadcastState();
                    }

                    case "RESIGN" -> {
                        if (game.isGameOver()) {
                            player.send("ERROR|Game is already over");
                            return;
                        }

                        game.resign(player.color);
                        replayRequestedBy = null;
                        broadcastState();
                    }

                    case "DRAW_OFFER" -> {
                        if (game.isGameOver()) {
                            player.send("ERROR|Game is already over");
                            return;
                        }

                        game.offerDraw(player.color);
                        broadcastState();
                    }

                    case "DRAW_ACCEPT" -> {
                        if (game.isGameOver()) {
                            player.send("ERROR|Game is already over");
                            return;
                        }

                        game.respondDraw(player.color, true);
                        replayRequestedBy = null;
                        broadcastState();
                    }

                    case "DRAW_DECLINE" -> {
                        if (game.isGameOver()) {
                            player.send("ERROR|Game is already over");
                            return;
                        }

                        game.respondDraw(player.color, false);
                        broadcastState();
                    }

                    case "REPLAY_REQUEST" -> handleReplayRequest(player);

                    case "REPLAY_ACCEPT" -> handleReplayAccept(player);

                    case "REPLAY_DECLINE" -> handleReplayDecline(player);

                    default -> player.send("ERROR|Unknown command: " + GameState.escape(parts[0]));
                }

            } catch (Exception e) {
                player.send("ERROR|Invalid message: " + GameState.escape(e.getMessage()));

                System.err.println("[Session " + sessionId + "] Invalid client message from " + player.color + ": " + message);
                e.printStackTrace();
            }
        }

        private void handleReplayRequest(PlayerConnection player) {
            if (!game.isGameOver()) {
                player.send("ERROR|Replay can only be requested after the game is over");
                return;
            }

            if (replayRequestedBy == null) {
                replayRequestedBy = player.color;
                player.send("REPLAY_WAITING");
                opponentOf(player).send("REPLAY_OFFER|" + player.color);
                return;
            }

            if (replayRequestedBy == player.color) {
                player.send("REPLAY_WAITING");
                return;
            }

            startReplay();
        }

        private void handleReplayAccept(PlayerConnection player) {
            if (!game.isGameOver()) {
                player.send("ERROR|Replay can only be accepted after the game is over");
                return;
            }

            if (replayRequestedBy == null) {
                replayRequestedBy = player.color;
                player.send("REPLAY_WAITING");
                opponentOf(player).send("REPLAY_OFFER|" + player.color);
                return;
            }

            if (replayRequestedBy == player.color) {
                player.send("REPLAY_WAITING");
                return;
            }

            startReplay();
        }

        private void handleReplayDecline(PlayerConnection player) {
            if (replayRequestedBy != null && replayRequestedBy != player.color) {
                player.send("REPLAY_DECLINED");
                opponentOf(player).send("REPLAY_DECLINED");
                replayRequestedBy = null;
            }
        }

        private void startReplay() {
            replayRequestedBy = null;

            game.reset();

            white.send("REPLAY_START");
            black.send("REPLAY_START");

            broadcastState();
        }

        private synchronized void handleDisconnect(PlayerConnection disconnected) {
            if (!running) {
                return;
            }

            System.out.println("[Session " + sessionId + "] " + disconnected.color + " disconnected.");

            PlayerConnection opponent = opponentOf(disconnected);

            if (game.isGameOver()) {
                opponent.send("REPLAY_UNAVAILABLE|Opponent left the session. You can go back to menu and find a new opponent.");
            } else {
                game.opponentDisconnected(disconnected.color);
                broadcastState();
            }

            finishSession();
        }

        private PlayerConnection opponentOf(PlayerConnection player) {
            return player == white ? black : white;
        }

        private synchronized void finishSession() {
            running = false;
            closeQuietly(white);
            closeQuietly(black);
        }

        private void broadcastState() {
            String state = game.toStateMessage();

            white.send(state);
            black.send(state);
        }

        private void closeQuietly(PlayerConnection p) {
            try {
                p.socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static class PlayerConnection {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        final PieceColor color;

        PlayerConnection(Socket socket, PieceColor color) throws IOException {
            this.socket = socket;
            this.color = color;
            this.socket.setKeepAlive(true);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        synchronized void send(String message) {
            try {
                out.writeUTF(message);
                out.flush();
            } catch (IOException e) {
                System.err.println("Send failed to " + color + ": " + e.getMessage());
            }
        }
    }
}