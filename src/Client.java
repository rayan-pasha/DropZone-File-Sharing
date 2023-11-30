import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
    private PrintWriter out;
    private String clientName;
    private JTextArea chatArea;
    private JTextField messageField;
    private List<String> clientNames = new ArrayList<>();

    public Client() {
        // Loop until a non-blank username is entered
        while (true) {
            clientName = JOptionPane.showInputDialog("Enter your username:");
            if (clientName != null && !clientName.trim().isEmpty()) {
                break;
            } else {
                JOptionPane.showMessageDialog(null, "Username cannot be blank. Please enter a valid username.");
            }
        }

        JFrame frame = new JFrame("P2P Chat - " + clientName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        JButton exitButton = new JButton("Exit");

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
                messageField.setText("");
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendExitMessage();
                System.exit(0);  // Terminate the client application
            }
        });

        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage(messageField.getText());
                messageField.setText("");
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(messageField, BorderLayout.CENTER);
        buttonPanel.add(sendButton, BorderLayout.EAST);
        buttonPanel.add(exitButton, BorderLayout.WEST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setVisible(true);

        try {
            Socket socket = new Socket("localhost", 1809);
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send the username to the server
            out.println(clientName);

            // Add the client's name to the list of client names
            clientNames.add(clientName);

            // Handle incoming messages in a separate thread
            Thread receiverThread = new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("/CLIENTLIST/")) {
                            updateClientList(message.substring(12)); // Assuming "/CLIENTLIST/" is used to send client names from the server
                        } else {
                            chatArea.append(message + "\n");
                        }
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            receiverThread.start();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error connecting to the server: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void updateClientList(String clients) {
        String[] names = clients.split(",");
        clientNames.clear();
        for (String name : names) {
            clientNames.add(name.trim());
        }
        // Do something with the updated client names list if needed
    }

    private void sendMessage(String message) {
        out.println(message);
    }

    private void sendExitMessage() {
        out.println("/EXIT/");  // Notify the server that the client is exiting
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
