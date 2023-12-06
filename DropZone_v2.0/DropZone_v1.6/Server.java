import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.*;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

import static java.lang.System.out;

// Import statements remain the same as provided earlier
public class Server {
    private static final int PORT = 1809;
    private static final int MAX_CLIENTS = 8;
    private static int clients = 0;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private static List<ClientHandler> clientHandlers = new ArrayList<>();
    private static ServerGUI serverGUI;

    public static void main(String[] args) {

        DB.init();

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
        private boolean isReceivingFile = false;
        private static final String FILE_STORAGE_PATH = "upload/";


        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);

                clientName = in.readLine();
                broadcastMessage(clientName + " has joined the chat.");

                while (true) {
                    String line = in.readLine();

                    if ("/EXIT/".equalsIgnoreCase(line)) {
                        broadcastMessage(clientName + " has left the chat");

                    } else if ("/FILE/".equalsIgnoreCase(line)) {
                        handleFileUpload(in);
                    } else if ("/DOWNLOAD/".equalsIgnoreCase(line)) {
                        handleFileDownload(clientWriter, in);
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

        private void handleFileUpload(BufferedReader in) {
            try {
                String fileName = in.readLine();
                String fileData = in.readLine();

                byte[] decodedData = Base64.getDecoder().decode(fileData);
                File saveFile = new File(FILE_STORAGE_PATH + fileName);

                try (FileOutputStream fileOutputStream = new FileOutputStream(saveFile)) {
                    fileOutputStream.write(decodedData);
                }

                broadcastMessage(clientName + " has uploaded file: " + fileName);

                DB.insertUpload(clientName, fileName);
                DB.printUploadList();



            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void handleFileDownloadRequest(PrintWriter out, String fileName) {
            try {
                File fileToSend = new File(FILE_STORAGE_PATH + fileName);

                if (fileToSend.exists()) {
                    try (FileInputStream fileInputStream = new FileInputStream(fileToSend);
                         BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

                        out.println("/FILE/");  // Notify the client that a file is being sent
                        out.println(fileToSend.getName());  // Send the file name to the client

                        byte[] fileData = new byte[(int) fileToSend.length()];
                        bufferedInputStream.read(fileData, 0, fileData.length);

                        out.println(Base64.getEncoder().encodeToString(fileData));  // Send the file data to the client
                    }
                } else {
                    out.println("File not found: " + fileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendFile(File file) {
            try (FileInputStream fileInputStream = new FileInputStream(file);
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

                clientWriter.println("/FILE/");  // Notify the client that a file is being sent

                clientWriter.println(file.getName());  // Send the file name to the client

                byte[] fileData = new byte[(int) file.length()];
                bufferedInputStream.read(fileData, 0, fileData.length);

                clientWriter.println(Base64.getEncoder().encodeToString(fileData));  // Send the file data to the client

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void handleFileDownload(PrintWriter out, BufferedReader in) {
            try {
                String fileName = in.readLine();
                File fileToSend = new File(FILE_STORAGE_PATH + fileName);

                if (fileToSend.exists()) {
                    try (FileInputStream fileInputStream = new FileInputStream(fileToSend);
                         BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

                        out.println("/FILE/");  // Notify the client that a file is being sent
                        out.println(fileToSend.getName());  // Send the file name to the client

                        byte[] fileData = new byte[(int) fileToSend.length()];
                        bufferedInputStream.read(fileData, 0, fileData.length);

                        out.println(Base64.getEncoder().encodeToString(fileData));  // Send the file data to the client

                        DB.insertDownload(clientName, fileName);


                        DB.printDownloadList();

                    }
                } else {
                    out.println("File not found: " + fileName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void broadcastMessage(String message) {
            for (ClientHandler handler : clientHandlers) {
                if (!handler.equals(this) || !handler.isReceivingFile) {
                    handler.clientWriter.println(message);
                }
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

        connectedClientsLabel = new JLabel("Connected Clients:");

        add(connectedClientsLabel, BorderLayout.CENTER);

        setVisible(true);
    }

    public void displayConnectedClients(int numberOfClients) {
        connectedClientsLabel.setText("Connected Clients: " + numberOfClients);
    }
}