import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class DropZoneServer {
    private static final int PORT = 8888;
    private String clientName;
    private List<ClientHandler> clientHandlers = new ArrayList<>();
    private List<ChatRoom> chatRooms = new ArrayList<>();
    private static final String FILE_STORAGE_PATH = "upload/";
    private static final int MAX_CLIENTS = 8;
    private static final List<ClientHandler> clientHandler = new ArrayList<>();
    private static List<ClientHandler> connectedClients = new ArrayList<>();
    private static ServerGUI serverGUI;

    public static void main(String[] args) {
        DB.init();
        serverGUI = new ServerGUI(connectedClients);
        new DropZoneServer().startServer();
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("DropZone Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // Create a new client handler for each connected client in the default lobby
                ChatRoom defaultLobby = getOrCreateLobby("DefaultLobby");
                ClientHandler clientHandler = new ClientHandler(clientSocket, defaultLobby);
                defaultLobby.addClient(clientHandler);

                // Start a new thread to handle the client
                new Thread(clientHandler).start();

                // Update connected clients count in the GUI
                serverGUI.updateConnectedClients(connectedClients.size());

                // Send the list of available chat rooms to the newly connected client
                clientHandler.sendChatRoomsList();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private ChatRoom getOrCreateLobby(String lobbyName) {
        for (ChatRoom chatRoom : chatRooms) {
            if (chatRoom.getRoomName().equals(lobbyName)) {
                return chatRoom;
            }
        }

        // If the lobby doesn't exist, create a new one
        ChatRoom newLobby = new ChatRoom(lobbyName);
        chatRooms.add(newLobby);
        return newLobby;
    }

    // Inner class to handle each connected client
    private class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private ChatRoom currentLobby;
        private String clientName;

        public ClientHandler(Socket socket, ChatRoom lobby) {
            this.socket = socket;
            this.currentLobby = lobby;
            connectedClients.add(this);

            try {
                // Create a PrintWriter for writing to the client's output stream
                writer = new PrintWriter(socket.getOutputStream(), true);
                // Create a BufferedReader for reading from the client's input stream
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void handleClientCommand(String command, ClientHandler clientHandler) {
            // Split the command into parts
            String[] parts = command.split(":");
            String action = parts[0];

            switch (action) {

                case "REQUEST_FILE":
                    handleFileRequest(parts[1], parts[2], clientHandler);
                    break;

            }
        }
        private void handleFileRequest(String fileName, String downloadPath, ClientHandler clientHandler) {
            // Check if the file exists
            File file = new File("serversaves/" + fileName);
            if (!file.exists()) {
                clientHandler.sendMessage("FILE_NOT_FOUND:" + fileName);
                return;
            }

            try (FileInputStream fileInputStream = new FileInputStream(file);
                 BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

                // Read the file content into a byte array
                byte[] fileData = new byte[(int) file.length()];
                bufferedInputStream.read(fileData, 0, fileData.length);

                // Send the file data to the client
                clientHandler.sendMessage("SEND_FILE:" + fileName + ":" + Base64.getEncoder().encodeToString(fileData));

                DB.insertDownload(clientName, fileName);
                DB.printDownloadList();

            } catch (IOException e) {
                e.printStackTrace();
                clientHandler.sendMessage("FILE_TRANSFER_FAILED:" + fileName);
            }
        }

        private void sendFileList() {
            File serverSaveFolder = new File("serversaves");
            File[] files = serverSaveFolder.listFiles();

            if (files != null) {
                // Send the list of available files to the client
                StringBuilder fileListMessage = new StringBuilder("FILE_LIST:");
                for (File file : files) {
                    fileListMessage.append(file.getName()).append(",");
                }

                sendMessage(fileListMessage.toString());
            }
        }

        private void sendMessage(String message) {
            writer.println(message);
        }

        private void handleRefreshRequest() {
            sendFileList();
        }
        private void sendChatRoomsList() {
            StringBuilder chatRoomsListMessage = new StringBuilder("CHAT_ROOMS_LIST:");
            for (ChatRoom room : chatRooms) {
                chatRoomsListMessage.append(room.getRoomName()).append(",");
            }
            sendMessage(chatRoomsListMessage.toString());
        }

        @Override
        public void run() {
            try {
                // Send the file list to the client upon connection
                sendFileList();
                sendChatRoomsList();

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("CLIENT_INFO:")) {
                        handleClientInfo(line);
                    } else if ("/UPLOAD/".equalsIgnoreCase(line)) {
                        handleFileUpload(reader);
                    } else if ("/SWITCH_LOBBY/".equalsIgnoreCase(line)) {
                        handleLobbySwitch(reader);
                    } else if ("REQUEST_FILE_LIST".equalsIgnoreCase(line)) {
                        handleRefreshRequest();
                    } else {
                        // Handle other messages
                        handleClientCommand(line, this); // Add this line
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Remove the client when they disconnect
                connectedClients.remove(this);
                currentLobby.removeClient(this);
                System.out.println("Client disconnected: " + socket);
            }
        }

        private void handleClientInfo(String clientInfo) {
            // Format: CLIENT_INFO:username
            String[] parts = clientInfo.split(":");
            clientName = parts[1];
            // Update the GUI or perform any other necessary actions
        }
        private void handleClientMessage(String message) {
            currentLobby.broadcastMessage(clientName, message);
        }


        // Handle switching to a different lobby
        private void handleLobbySwitch(BufferedReader reader) {
            try {
                String newLobbyName = reader.readLine();
                ChatRoom newLobby = getOrCreateLobby(newLobbyName);
                currentLobby.removeClient(this);
                newLobby.addClient(this);
                currentLobby = newLobby;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // ... (rest of the ClientHandler class)

        // Inside the ClientHandler class, modify the handleFileUpload method
        private void handleFileUpload(BufferedReader reader) {
            try {
                String fileName = reader.readLine();
                String fileData = reader.readLine();

                // Decode the Base64-encoded file data
                byte[] decodedData = Base64.getDecoder().decode(fileData);

                // Specify the path where the server will save the uploaded files
                String serverSaveFolderPath = "serversaves/";

                // Create the directory if it doesn't exist
                File serverSaveFolder = new File(serverSaveFolderPath);
                if (!serverSaveFolder.exists()) {
                    serverSaveFolder.mkdirs();  // This will create necessary parent directories as well
                }

                // Create a new file and write the data into it
                File saveFile = new File(serverSaveFolder, fileName);
                try (FileOutputStream fileOutputStream = new FileOutputStream(saveFile)) {
                    fileOutputStream.write(decodedData);
                }

                // You can add additional logic here if needed

                System.out.println("File saved on the server: " + saveFile.getAbsolutePath());

                DB.insertUpload(clientName, fileName);
                DB.printUploadList();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ChatRoom {
        private String roomName;
        private List<ClientHandler> clients = new ArrayList<>();

        public ChatRoom(String roomName) {
            this.roomName = roomName;
        }

        public String getRoomName() {
            return roomName;
        }

        public void addClient(ClientHandler client) {
            clients.add(client);
        }

        public void removeClient(ClientHandler client) {
            clients.remove(client);
        }

        public void broadcast(String message) {
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
        public void broadcastMessage(String sender, String message) {
            String formattedMessage = String.format("[%s]: %s", sender, message);
            broadcast(formattedMessage);
        }
    }
    private static class ServerGUI extends JFrame {
        private JLabel connectedClientsLabel;

        public ServerGUI(List<ClientHandler> connectedClients) {
            initializeUI();
            updateConnectedClients(DropZoneServer.connectedClients.size());
        }

        public void updateConnectedClients(int numberOfClients) {
            connectedClientsLabel.setText("Connected Clients: " + numberOfClients);
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
}
