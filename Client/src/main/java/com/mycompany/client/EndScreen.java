package com.mycompany.client;

import com.mycompany.GameLogic.*;

import javax.swing.*;
import java.awt.*;

// Oyun sonucu, replay ve çıkış seçeneklerini gösteren bitiş ekranı.
public class EndScreen extends JFrame {
    private final JButton replay = new JButton("Replay");
    private final JButton back = new JButton("Back to Menu");
    private final JButton close = new JButton("Close");
    private final JLabel replayStatus = new JLabel(" ", SwingConstants.CENTER);

    // Oyun bitince sonuç metni ve aksiyon butonlarıyla pencereyi hazırlar.
    public EndScreen(String result, Client client, GameFrame frame) {
        setTitle("Game Over");
        setSize(470, 260);
        setLocationRelativeTo(frame);
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JLabel label = new JLabel("<html><div style='text-align:center;'>" + result + "</div></html>", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));

        replayStatus.setFont(new Font("Arial", Font.PLAIN, 13));
        replayStatus.setForeground(new Color(80, 80, 80));

        replay.addActionListener(e -> {
            replay.setEnabled(false);
            replayStatus.setText("Replay request sent. Waiting for opponent...");
            client.requestReplay();
        });

        back.addActionListener(e -> {
            client.closeConnection();
            frame.closeFrame();
            new StartScreen();
            dispose();
        });

        close.addActionListener(e -> {
            dispose();
            frame.closeFrame();
            client.exitApplication();
        });

        JPanel center = new JPanel(new BorderLayout(5, 5));
        center.add(label, BorderLayout.CENTER);
        center.add(replayStatus, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(replay);
        buttons.add(back);
        buttons.add(close);

        add(center, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        setVisible(true);
    }

    // Replay isteği gönderildiğinde butonu bekleme durumuna alır.
    public void setReplayWaiting() {
        replay.setEnabled(false);
        replayStatus.setText("Replay request sent. Waiting for opponent...");
    }

    // Karşı taraftan replay teklifi geldiğinde kullanıcıyı bilgilendirir.
    public void setReplayOfferReceived() {
        replay.setEnabled(false);
        replayStatus.setText("Opponent wants replay. Choose Yes or No in the dialog.");
    }

    public void setReplayDeclined() {
        replay.setEnabled(true);
        replayStatus.setText("Opponent declined replay. You can request again or go back to menu.");
    }

    // Rakip ayrıldığında replay seçeneğini pasif hale getirir.
    public void setReplayUnavailable() {
        replay.setEnabled(false);
        replayStatus.setText("Replay unavailable because opponent left the session.");
    }

    public void closeForReplay() {
        dispose();
    }
}