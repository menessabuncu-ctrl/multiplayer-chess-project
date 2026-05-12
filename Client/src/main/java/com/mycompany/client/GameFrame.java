
package com.mycompany.client;

import com.mycompany.GameLogic.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// GameFrame ana oyun penceresidir.
// Tahta paneli, sıra bilgisi, oyuncu rengi ve oyun kontrol butonları burada bulunur.
public class GameFrame extends JFrame {

    // Server ile haberleşen client nesnesi.
    private final Client client;

    // Satranç tahtasını çizen panel.
    private BoardPanel boardPanel;

    // Durum mesajını gösteren label.
    private JLabel statusLabel;

    // Oyuncunun rengini gösteren label.
    private JLabel colorLabel;

    // Sıranın kimde olduğunu gösteren label.
    private JLabel turnLabel;

    // Pes etme butonu.
    private JButton resignButton;

    // Beraberlik teklif butonu.
    private JButton drawButton;

    // Yeniden başlatma butonu.
    private JButton restartButton;

    // Oyuncu rengi.
    private PieceColor playerColor;

    // Güncel sıra bilgisi.
    private PieceColor currentTurn;

    public GameFrame(Client client) {
        this.client = client;
        initComponents();
    }

    // GUI bileşenlerini oluşturur.
    private void initComponents() {
        setTitle("Network Chess");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(760, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Pencere kapatılırken server'a QUIT mesajı gönderilir.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeGame();
            }
        });

        // Üst bilgi paneli.
        JPanel topPanel = new JPanel(new GridLayout(3, 1));

        colorLabel = new JLabel("Color: Waiting...", SwingConstants.CENTER);
        colorLabel.setFont(new Font("Arial", Font.BOLD, 16));

        turnLabel = new JLabel("Turn: Waiting...", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Arial", Font.PLAIN, 15));

        statusLabel = new JLabel("Connecting to server...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 15));

        topPanel.add(colorLabel);
        topPanel.add(turnLabel);
        topPanel.add(statusLabel);

        add(topPanel, BorderLayout.NORTH);

        // Tahta paneli.
        boardPanel = new BoardPanel(this);
        add(boardPanel, BorderLayout.CENTER);

        // Alt kontrol paneli.
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));

        resignButton = new JButton("Resign");
        drawButton = new JButton("Offer Draw");
        restartButton = new JButton("Restart");

        resignButton.addActionListener(e -> resignGame());
        drawButton.addActionListener(e -> offerDraw());
        restartButton.addActionListener(e -> restartGame());

        bottomPanel.add(resignButton);
        bottomPanel.add(drawButton);
        bottomPanel.add(restartButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // BoardPanel bir hamle seçtiğinde buraya gelir.
    // Hamle client üzerinden server'a gönderilir.
    public void sendMove(Move move) {
        if (playerColor == null) {
            updateStatus("Player color is not assigned yet.");
            return;
        }

        if (currentTurn != null && currentTurn != playerColor) {
            updateStatus("It is not your turn.");
            return;
        }

        client.sendMove(move);
    }

    // Server'dan gelen güncel board bilgisini tahtaya aktarır.
    public void updateBoard(Piece[][] board) {
        boardPanel.setBoard(board);
    }

    // Server'dan gelen sıra bilgisini günceller.
    public void updateTurn(PieceColor turn) {
        this.currentTurn = turn;
        turnLabel.setText("Turn: " + turn);

        if (playerColor != null && turn == playerColor) {
            statusLabel.setText("Your turn.");
        }
    }

    // Durum mesajını günceller.
    public void updateStatus(String message) {
        statusLabel.setText(message);
    }

    // Oyuncu rengini GUI'de gösterir.
    public void setPlayerColor(PieceColor color) {
        this.playerColor = color;
        colorLabel.setText("You are: " + color);
        boardPanel.setPlayerColor(color);
    }

    // Oyuncu pes etmek istediğinde çalışır.
    private void resignGame() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to resign?",
                "Resign",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            client.sendResign();
        }
    }

    // Beraberlik teklif eder.
    private void offerDraw() {
        client.sendDrawOffer();
        updateStatus("Draw offer sent.");
    }

    // Oyunu yeniden başlatma isteği gönderir.
    private void restartGame() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Restart the game?",
                "Restart",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            client.sendRestart();
            setEnabled(true);
        }
    }

    // Pencere kapatılırken client bağlantısını düzgün kapatır.
    private void closeGame() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Do you want to quit the game?",
                "Quit",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            client.sendQuit();
            client.disconnect();
            dispose();
            System.exit(0);
        }
    }

    // EndScreen tarafından çağrılabilir.
    // Oyuncuyu menüye döndürür.
    public void backToMenu() {
        client.disconnect();
        dispose();

        SwingUtilities.invokeLater(() -> new StartScreen().setVisible(true));
    }

    // EndScreen tarafından çağrılabilir.
    // Oyunu tamamen kapatır.
    public void exitApplication() {
        client.disconnect();
        dispose();
        System.exit(0);
    }

    public PieceColor getPlayerColor() {
        return playerColor;
    }

    public PieceColor getCurrentTurn() {
        return currentTurn;
    }
}