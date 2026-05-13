package com.mycompany.client;

import com.mycompany.GameLogic.*;

import javax.swing.*;
import java.awt.*;

// Oyuna başlamadan önce server IP adresini alan giriş ekranı.
public class StartScreen extends JFrame {

    // IP alanı ve start butonu ile başlangıç arayüzünü hazırlar.
    public StartScreen() {

        setTitle("Multiplayer Chess");
        setSize(500, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(40, 40, 40));

        JLabel title = new JLabel("MULTIPLAYER CHESS", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 32));

        JLabel subtitle = new JLabel("Socket Programming Chess Game", SwingConstants.CENTER);
        subtitle.setForeground(new Color(210, 210, 210));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 16));

        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBackground(new Color(40, 40, 40));
        topPanel.add(title);
        topPanel.add(subtitle);

        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(new Color(40, 40, 40));
        centerPanel.setLayout(new GridLayout(3, 1, 10, 10));

        JLabel ipLabel = new JLabel("Server IP Address", SwingConstants.CENTER);
        ipLabel.setForeground(Color.WHITE);
        ipLabel.setFont(new Font("Arial", Font.BOLD, 18));

        JTextField ipField = new JTextField("98.82.187.117");
        ipField.setHorizontalAlignment(SwingConstants.CENTER);
        ipField.setFont(new Font("Arial", Font.PLAIN, 18));

        JButton startButton = new JButton("START GAME");
        startButton.setFont(new Font("Arial", Font.BOLD, 20));
        startButton.setBackground(new Color(70, 130, 180));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);

        startButton.addActionListener(e -> {
            try {
                new Client(ipField.getText());
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Connection Failed");
            }
        });

        centerPanel.add(ipLabel);
        centerPanel.add(ipField);
        centerPanel.add(startButton);

        JLabel footer = new JLabel("Software Engineering Chess Project", SwingConstants.CENTER);
        footer.setForeground(new Color(170, 170, 170));
        footer.setFont(new Font("Arial", Font.PLAIN, 14));

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(footer, BorderLayout.SOUTH);

        add(mainPanel);

        setVisible(true);
    }
}