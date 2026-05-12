
package com.mycompany.client;

import com.mycompany.GameLogic.*;
import javax.swing.*;
import java.awt.*;

// EndScreen oyun bittiğinde açılan sonuç ekranıdır.
// Kazananı, oyun bitiş sebebini ve kullanıcı seçeneklerini gösterir.
public class EndScreen extends JFrame {

    // Client nesnesi üzerinden restart, quit gibi mesajlar gönderilebilir.
    private final Client client;

    // Oyunun bitiş durumu.
    private final GameStatus finalStatus;

    // Kazanan oyuncu. Beraberlikte null olur.
    private final PieceColor winner;

    // Gösterilecek sonuç mesajı.
    private final String message;

    public EndScreen(Client client, GameStatus finalStatus, PieceColor winner, String message) {
        this.client = client;
        this.finalStatus = finalStatus;
        this.winner = winner;
        this.message = message;

        initComponents();
    }

    // Sonuç ekranının bileşenlerini oluşturur.
    private void initComponents() {
        setTitle("Game Over");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(420, 280);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Başlık.
        JLabel titleLabel = new JLabel("Game Over", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Sonuç bilgileri.
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));

        JLabel statusLabel = new JLabel("Status: " + finalStatus, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        JLabel winnerLabel = new JLabel(getWinnerText(), SwingConstants.CENTER);
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        infoPanel.add(statusLabel);
        infoPanel.add(winnerLabel);
        infoPanel.add(messageLabel);

        mainPanel.add(infoPanel, BorderLayout.CENTER);

        // Butonlar.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        JButton restartButton = new JButton("Restart");
        JButton menuButton = new JButton("Back to Menu");
        JButton closeButton = new JButton("Close");

        restartButton.addActionListener(e -> restartGame());
        menuButton.addActionListener(e -> backToMenu());
        closeButton.addActionListener(e -> closeApplication());

        buttonPanel.add(restartButton);
        buttonPanel.add(menuButton);
        buttonPanel.add(closeButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // Kazanan bilgisini okunabilir metne çevirir.
    private String getWinnerText() {
        if (winner == null) {
            return "Result: Draw";
        }

        return "Winner: " + winner;
    }

    // Server'a restart mesajı gönderir.
    private void restartGame() {
        client.sendRestart();
        dispose();
    }

    // Oyuncuyu bağlantıyı kapatıp başlangıç ekranına döndürür.
    private void backToMenu() {
        client.sendQuit();
        client.disconnect();

        dispose();

        SwingUtilities.invokeLater(() -> new StartScreen().setVisible(true));
    }

    // Uygulamayı tamamen kapatır.
    private void closeApplication() {
        client.sendQuit();
        client.disconnect();

        dispose();
        System.exit(0);
    }
}