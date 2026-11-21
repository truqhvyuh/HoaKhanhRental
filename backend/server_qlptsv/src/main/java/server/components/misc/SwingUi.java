package server.components.misc;

import server.components.session.SmtpSettings;

import javax.swing.*;
import java.awt.*;

public final class SwingUi {
    private SwingUi() {}

    public static void show(SmtpSettings smtpSettings, String serverState) {
        if (GraphicsEnvironment.isHeadless()) return;
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Room Rental Server");
            frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel fields = new JPanel(new GridLayout(8, 2, 8, 6));
            JLabel state = new JLabel("Status: " + serverState);
            JLabel emailState = new JLabel(smtpSettings.state());
            JTextField host = new JTextField(smtpSettings.host());
            JTextField port = new JTextField(smtpSettings.port() == 0 ? "" : Integer.toString(smtpSettings.port()));
            JTextField username = new JTextField(smtpSettings.username());
            JPasswordField password = new JPasswordField();
            JTextField from = new JTextField(smtpSettings.from());
            JTextField fromName = new JTextField(smtpSettings.fromName());

            for (Component component : new Component[] {
                    host, port, username, password, from, fromName}) {
                component.setEnabled(serverState.equals("Stopped"));
            }
            fields.add(new JLabel("Server")); fields.add(state);
            fields.add(new JLabel("Email")); fields.add(emailState);
            fields.add(new JLabel("SMTP host")); fields.add(host);
            fields.add(new JLabel("SMTP port")); fields.add(port);
            fields.add(new JLabel("SMTP username")); fields.add(username);
            fields.add(new JLabel("SMTP password")); fields.add(password);
            fields.add(new JLabel("Sender address")); fields.add(from);
            fields.add(new JLabel("Sender name")); fields.add(fromName);

            JButton start = new JButton("Start");
            JButton stop = new JButton("Stop");
            JPanel buttons = new JPanel();
            buttons.add(start);
            buttons.add(stop);
            start.addActionListener(event -> password.setText(""));
            stop.addActionListener(event -> password.setText(""));

            frame.add(fields, BorderLayout.CENTER);
            frame.add(buttons, BorderLayout.SOUTH);
            frame.setSize(520, 300);
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        });
    }
}
