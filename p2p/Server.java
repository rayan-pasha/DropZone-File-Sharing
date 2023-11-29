import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Server {
    private static final int PORT = 1809;
    private static final int MAX_CLIENTS = 2;
    private static int clients = 0;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static List<ClientHandler> clientHandlers = new ArrayList<>();
    private static ServerGUI serverGUI;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            serverGUI = new ServerGUI();
            serverGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            serverGUI.setSize(300, 100);
            serverGUI.setVisible(true);
        });

        try (ServerSocket server = new ServerSocket(PORT)) {
            server.setReuseAddress(true);

            while (true) {
                Socket client = server.accept();
                logger.info("New client connected: ");

                if (clients >= MAX_CLIENTS) {
                    String error = "Server is busy. Maximum number of clients reached.";
                    sendResponse(client, error);
                    logger.info("Server is busy. Maximum number of clients reached.");
                    break;
                }

                ClientHandler clientHandler = new ClientHandler(client);
                clientHandlers.add(clientHandler);

                new Thread(clientHandler).start();
                clients++;
                serverGUI.displayConnectedClients(clients);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error in server: " + e.getMessage(), e);
        }
    }

    private static void sendResponse(Socket client, String message) {
        try (PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {
            out.println(message);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error sending response: " + e.getMessage(), e);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter clientWriter;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);

                // Prompt the client to enter a username
                clientName = in.readLine();
                broadcastMessage(clientName + " has joined the chat.");

                while (true) {
                    String line = in.readLine();

                    if ("/EXIT/".equalsIgnoreCase(line)) {
                        broadcastMessage(clientName + " has left the chat");
                        break;
                    } else {
                        broadcastMessage(clientName + ": " + line);
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error handling client: " + e.getMessage(), e);
            } finally {
                try {
                    clientSocket.close();
                    clientHandlers.remove(this);
                    clients--;
                    serverGUI.displayConnectedClients(clients);
                    logger.info("Client " + clientName + " has disconnected");
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error closing client socket: " + e.getMessage(), e);
                }
            }
        }

        private void broadcastMessage(String message) {
            for (ClientHandler handler : clientHandlers) {
                handler.clientWriter.println(message);
            }
        }
    }
}

class ServerGUI extends JFrame {
    private JLabel connectedClientsLabel;

    public ServerGUI() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("P2P Chat Server");
        setSize(300, 100);

        connectedClientsLabel = new JLabel("Connected Clients: 0");

        add(connectedClientsLabel, BorderLayout.CENTER);

        setVisible(true);
    }

    public void displayConnectedClients(int numberOfClients) {
        connectedClientsLabel.setText("Connected Clients: " + numberOfClients);
    }
}
