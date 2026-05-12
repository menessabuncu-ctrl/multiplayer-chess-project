
package com.mycompany.client;

import javax.swing.*;
import java.awt.*;

// StartScreen oyunun ilk açılış ekranıdır.
// Kullanıcı buradan server IP ve port bilgisi girerek oyuna bağlanır.
public class StartScreen extends JFrame {

    // Server IP adresinin girileceği alan.
    private JTextField hostField;

    // Server portunun girileceği alan.
    private JTextField portField;

    // Bağlanma butonu.
    private JButton connectButton;

    // Uygulama başlığını gösteren label.
    private JLabel titleLabel;

    public StartScreen() {
        initComponents();
    }

    // Arayüz bileşenlerini oluşturur ve pencereye ekler.
    private void initComponents() {
        setTitle("Network Chess - Start");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        // Başlık alanı.
        titleLabel = new JLabel("Network Chess", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // IP ve port giriş alanlarının bulunduğu panel.
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(4, 1, 8, 8));

        JLabel hostLabel = new JLabel("Server IP:");
        hostField = new JTextField("localhost");

        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField("5000");

        formPanel.add(hostLabel);
        formPanel.add(hostField);
        formPanel.add(portLabel);
        formPanel.add(portField);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Buton paneli.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        connectButton = new JButton("Connect");
        connectButton.setFocusPainted(false);

        // Connect butonuna basıldığında server'a bağlanma denenir.
        connectButton.addActionListener(e -> connectToServer());

        buttonPanel.add(connectButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // Kullanıcının girdiği IP ve port ile server'a bağlanır.
    private void connectToServer() {
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();

        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Server IP cannot be empty.");
            return;
        }

        int port;

        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Port must be a number.");
            return;
        }

        connectButton.setEnabled(false);
        connectButton.setText("Connecting...");

        // Bağlantı denemesi GUI donmasın diye ayrı thread'de yapılır.
        new Thread(() -> {
            Client client = new Client();
            boolean connected = client.connect(host, port);

            SwingUtilities.invokeLater(() -> {
                if (connected) {
                    GameFrame gameFrame = new GameFrame(client);
                    client.setGameFrame(gameFrame);

                    gameFrame.setVisible(true);
                    dispose();
                } else {
                    connectButton.setEnabled(true);
                    connectButton.setText("Connect");
                }
            });
        }).start();
    }
}