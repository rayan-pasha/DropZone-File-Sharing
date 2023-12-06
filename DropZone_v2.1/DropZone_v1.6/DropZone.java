import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileView;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.text.DefaultCaret;

public class DropZone {
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel lobbyPanel;
    private JPanel chatRoomPanel;
    private JPanel currentPanel;
    private ChatRoom currentChatRoom;
    private List<ChatRoom> chatRooms;
    private List<File> uploadedFiles;
    private int fileCounter;
    private JTextField searchBar;
    private String clientName;
    private Socket clientSocket;
    private PrintWriter serverWriter;
    private BufferedReader serverReader;
    private JPanel chatRoomsPanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                     UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }

            new DropZone().createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        frame = new JFrame("DropZone - File Upload and Download");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        JPanel containerPanel = new JPanel(new BorderLayout());

        JButton p2pButton = P2PButtonStyle("Peer to Peer");
        p2pButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = createStyledTitleLabel("DropZone");

        mainPanel = createMainPanel();
        lobbyPanel = createLobbyPanel();

        currentPanel = new JPanel(new CardLayout());
        currentPanel.add(mainPanel, "main");
        currentPanel.add(lobbyPanel, "lobby");

        JButton refreshButton = createRefreshButton();
        containerPanel.add(refreshButton, BorderLayout.EAST);

        containerPanel.add(p2pButton, BorderLayout.WEST);
        containerPanel.add(titleLabel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(currentPanel);
        frame.add(containerPanel, BorderLayout.NORTH);
        frame.add(scrollPane);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        connectToServer();
    }

    private JButton createRefreshButton() {
        JButton refreshButton = createStyledButton("Refresh");
        refreshButton.addActionListener(e -> requestFileListFromServer());
        return refreshButton;
    }

    private void requestFileListFromServer() {
        serverWriter.println("REQUEST_FILE_LIST");
    }


    private void connectToServer() {
        try {
            clientSocket = new Socket("localhost", 8888);
            serverWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            serverReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientName = promptForUsername();

            serverWriter.println("CLIENT_INFO:" + clientName);

            serverWriter.println("CLIENT_INFO:DropZoneClient");

            new Thread(this::listenToServer).start();
        } catch (SocketException se) {
            JOptionPane.showMessageDialog(frame, "Server connection failed. Make sure the server is running.");
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenToServer() {
        try {
            String line;
            while ((line = serverReader.readLine()) != null) {
                System.out.println("Received from server: " + line);

                if (line.startsWith("CLIENT_INFO:")) {
                    handleClientInfo(line);
                } else if (line.startsWith("FILE_LIST:")) {
                    handleFileList(line);
                } else if (line.startsWith("CHAT:")) {
                    handleChatMessage(line);
                } else if (line.startsWith("CHAT_ROOMS:")) {
                    handleChatRooms(line);
                } else {
                    handleServerCommand(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleChatRooms(String chatRoomsMessage) {
        // Remove the "CHAT_ROOMS:" prefix
        String[] chatRooms = chatRoomsMessage.substring(12).split(",");

        // Clear existing components from the chatRoomPanel
        chatRoomPanel.removeAll();

        for (String roomName : chatRooms) {
            if (!roomName.isEmpty()) {
                JButton roomButton = createStyledButton(roomName);
                roomButton.addActionListener(e -> switchToChatRoom(findChatRoomByName(roomName)));

                JLabel chatRoomLabel = new JLabel(roomName);
                chatRoomLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                chatRoomLabel.setForeground(Color.BLACK);

                chatRoomPanel.add(chatRoomLabel);
                chatRoomPanel.add(roomButton);
            }
        }

        // Repaint and revalidate the chatRoomPanel
        chatRoomPanel.revalidate();
        chatRoomPanel.repaint();
    }


    private void handleClientInfo(String clientInfo) {
        // Format: CLIENT_INFO:username
        String[] parts = clientInfo.split(":");
        clientName = parts[1];
        // Update the GUI or perform any other necessary actions
    }

    private void sendExitMessage() {
        // Notify the server that the client is exiting the chat room
        serverWriter.println("/LEAVE_CHAT/");
    }

    private void handleChatMessage(String message) {
        // Format: CHAT:roomName:sender:message
        String[] parts = message.split(":");
        String roomName = parts[1];
        String sender = parts[2];
        String chatMessage = parts[3];

        // Find the ChatRoom based on the room name
        ChatRoom chatRoom = findChatRoomByName(roomName);

        if (chatRoom != null) {
            // Display the chat message in the JTextArea
            JTextArea chatTextArea = chatRoom.getChatTextArea();
            chatTextArea.append(sender + ": " + chatMessage + "\n");

            // Call displayChatMessage to update the UI
            displayChatMessage(roomName, sender, chatMessage);
        }
    }

    private ChatRoom findChatRoomByName(String roomName) {
        for (ChatRoom room : chatRooms) {
            if (room.getRoomName().equals(roomName)) {
                return room;
            }
        }
        return null;
    }

    private void displayChatMessage(String roomName, String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            // Find the chat room panel based on the room name
            for (Component component : currentPanel.getComponents()) {
                if (component instanceof JPanel && component.getName() != null && component.getName().equals(roomName)) {
                    // Assuming you have a JTextArea component in your chat room panel
                    JTextArea chatArea = findChatAreaInPanel((JPanel) component);

                    if (chatArea != null) {
                        // Append the message to the chat area
                        chatArea.append(sender + ": " + message + "\n");

                        // Scroll to the bottom to display the latest message
                        DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
                        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
                    }

                    break;
                }
            }
        });
    }

    private JTextArea findChatAreaInPanel(JPanel panel) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JTextArea) {
                return (JTextArea) component;
            }
        }
        return null;
    }

    private void handleServerCommand(String command) {
        // Split the command into parts
        String[] parts = command.split(":");
        String action = parts[0];

        switch (action) {
            case "FILE_LIST":
                // Handle file list from server, if needed
                break;
            case "DOWNLOAD_FILE":
                // Handle file download request
                String fileName = parts[1];
                downloadFileFromServer(fileName);
                break;
            case "CHAT":
                // Handle chat messages
                handleChatMessage(command);
                break;
            case "SEND_FILE":
                handleFileTransfer(parts[1], parts[2]);
                break;
            // Add other cases for different server commands
        }
    }
    private void handleFileTransfer(String fileName, String fileData) {
        // Decode the Base64-encoded file data
        byte[] decodedFileData = Base64.getDecoder().decode(fileData);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose Download Location");
        fileChooser.setSelectedFile(new File(fileName));

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File downloadLocation = fileChooser.getSelectedFile();

            try (FileOutputStream fileOutputStream = new FileOutputStream(downloadLocation)) {
                // Write the file data to the selected location
                fileOutputStream.write(decodedFileData);

                System.out.println("File downloaded to: " + downloadLocation.getAbsolutePath());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleFileList(String fileList) {
        // Remove the "FILE_LIST:" prefix
        String[] files = fileList.substring(10).split(",");

        // Update the user interface with the list of available files
        uploadedFiles.clear();
        mainPanel.removeAll();
        mainPanel.add(createStyledTitleLabel("File Explorer"));
        mainPanel.add(searchBar);

        JButton searchUploadButton = createStyledButton("Upload File");
        searchUploadButton.addActionListener(e -> performUpload());
        mainPanel.add(searchUploadButton);

        for (String fileName : files) {
            if (!fileName.isEmpty()) {
                File file = new File("serversaves/" + fileName);
                uploadedFiles.add(file);
                addFileButton(file, fileCounter++, mainPanel);
            }
        }

        frame.revalidate();
        frame.repaint();
    }

    private void downloadFileFromServer(String fileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose Download Location");
        fileChooser.setSelectedFile(new File(fileName));

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File downloadLocation = fileChooser.getSelectedFile();
            String downloadPath = downloadLocation.getAbsolutePath();

            // Send a command to the server to start the file transfer
            serverWriter.println("REQUEST_FILE:" + fileName + ":" + downloadPath);
        }
    }

    private JButton P2PButtonStyle(String text) {
        JButton button = new JButton(text);
        button.setAlignmentY(Component.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        button.setFont(new Font("DialogInput", Font.BOLD, 16));
        button.setForeground(new Color(46, 134, 193));
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> switchToLobbyPanel());

        return button;
    }

    public DropZone() {
        chatRooms = new ArrayList<>();
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        panel.setBackground(new Color(173, 216, 230));

        JLabel titleLabel = createStyledTitleLabel("File Explorer");
        panel.add(titleLabel);

        searchBar = new JTextField();
        searchBar.setAlignmentX(JTextField.CENTER_ALIGNMENT);
        searchBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, searchBar.getPreferredSize().height));
        panel.add(searchBar);

        JButton uploadButton = createStyledButton("Upload File");
        JButton refreshButton = createRefreshButton();  // Add the refresh button
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(uploadButton);
        panel.add(refreshButton);  // Add the refresh button
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        uploadedFiles = new ArrayList<>();
        fileCounter = 1;

        uploadButton.addActionListener(e -> performUpload());

        searchBar.addActionListener(e -> filterFiles(searchBar.getText()));
        searchBar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterFiles(searchBar.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterFiles(searchBar.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        return panel;
    }

    private void performUpload() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            uploadedFiles.add(selectedFile);

            serverWriter.println("/UPLOAD/");

            // Send the file name to the server
            serverWriter.println(selectedFile.getName());

            // Now, you should send the file content to the server
            sendFile(selectedFile);

            addFileButton(selectedFile, fileCounter++, mainPanel);
            frame.revalidate();
            frame.repaint();
        }
    }

    private void sendFile(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            byte[] fileData = new byte[(int) file.length()];
            bufferedInputStream.read(fileData, 0, fileData.length);

            // Send the file data to the server
            serverWriter.println(Base64.getEncoder().encodeToString(fileData));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addFileButton(File file, int fileNumber, JPanel targetPanel) {
        JButton fileButton = new JButton(file.getName(), getFileIcon(file));
        fileButton.setFont(new Font("Arial", Font.PLAIN, 14));
        fileButton.setForeground(Color.BLACK);
        fileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel fileNumberLabel = new JLabel(fileNumber + ". ");
        fileNumberLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        fileNumberLabel.setForeground(Color.BLACK);

        JButton downloadButton = createDownloadButton();
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        filePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, fileButton.getPreferredSize().height));
        filePanel.add(fileNumberLabel);
        filePanel.add(fileButton);
        filePanel.add(downloadButton);

        fileButton.addActionListener(e -> downloadFile(file));
        downloadButton.addActionListener(e -> downloadFile(file));

        targetPanel.add(filePanel);
    }

    private JButton createDownloadButton() {
        JButton downloadButton = new JButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });

        downloadButton.setText("↓");
        downloadButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return downloadButton;
    }

    private Icon getFileIcon(File file) {
        JFileChooser fileChooser = new JFileChooser();
        FileView fileView = fileChooser.getUI().getFileView(fileChooser);
        return fileView.getIcon(file);
    }

    private JPanel createLobbyPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(135, 206, 250));

        JLabel lobbyLabel = createStyledTitleLabel("P2P Lobby");

        chatRoomPanel = new JPanel();
        chatRoomPanel.setLayout(new BoxLayout(chatRoomPanel, BoxLayout.Y_AXIS));

        JScrollPane chatRoomsScrollPane = new JScrollPane(chatRoomPanel);
        chatRoomsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        JButton createChatRoomButton = createStyledButton("Create Chat Room");
        JButton refreshChatRoomsButton = createStyledButton("Refresh Chat Rooms");  // Add the refresh button
        JButton goBackButton = createStyledButton("Go Back");

        createChatRoomButton.addActionListener(e -> createChatRoom());
        refreshChatRoomsButton.addActionListener(e -> requestChatRoomsFromServer());  // Handle refresh action
        goBackButton.addActionListener(e -> switchToMainPanel());

        panel.add(lobbyLabel);
        panel.add(chatRoomsScrollPane);
        panel.add(createChatRoomButton);
        panel.add(refreshChatRoomsButton);  // Add the refresh button
        panel.add(goBackButton);

        return panel;
    }

    private void requestChatRoomsFromServer() {
        serverWriter.println("REQUEST_CHAT_ROOMS");
    }

    private String promptForUsername() {
        return JOptionPane.showInputDialog(frame, "Enter your username:");
    }

    private void createChatRoom() {
        String roomName = JOptionPane.showInputDialog("Enter Chat Room Name:");
        if (roomName != null && !roomName.trim().isEmpty()) {
            serverWriter.println("CREATE_CHAT_ROOM:" + roomName);
            ChatRoom chatRoom = new ChatRoom(roomName);
            chatRooms.add(chatRoom);

            JButton roomButton = createStyledButton(roomName);
            roomButton.addActionListener(e -> switchToChatRoom(chatRoom));

            JLabel chatRoomLabel = new JLabel(chatRooms.size() + ". " + roomName);
            chatRoomLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            chatRoomLabel.setForeground(Color.BLACK);

            chatRoomPanel.setLayout(new BoxLayout(chatRoomPanel, BoxLayout.Y_AXIS));
            chatRoomPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
            chatRoomPanel.add(chatRoomLabel);
            chatRoomPanel.add(roomButton);

            switchToLobbyPanel();
            frame.revalidate();
            frame.repaint();
        } else {
            JOptionPane.showMessageDialog(frame, "Chat Room Name cannot be blank. Please enter a valid name.");
        }
    }


    private void switchToLobbyPanel() {
        serverWriter.println("JOIN_LOBBY");
        currentChatRoom = null;
        CardLayout cardLayout = (CardLayout) currentPanel.getLayout();
        cardLayout.show(currentPanel, "lobby");
    }

    private void switchToChatRoom(ChatRoom chatRoom) {
        serverWriter.println("JOIN_CHAT_ROOM:" + chatRoom.getRoomName());

        // Create a new chat room panel for the selected chat room
        JPanel chatRoomPanel = createChatRoomPanel(chatRoom);

        // Add the new chat room panel to the current panel
        currentPanel.add(chatRoomPanel, "chat");

        // Update the currentChatRoom variable
        currentChatRoom = chatRoom; // Add this line

        // Switch to the new chat room panel
        CardLayout cardLayout = (CardLayout) currentPanel.getLayout();
        cardLayout.show(currentPanel, "chat");
    }

    private JPanel createChatRoomPanel(ChatRoom chatRoom) {
        JPanel panel = new JPanel(new BorderLayout());

        // Add a JLabel with the room name
        JLabel roomLabel = new JLabel("Room: " + chatRoom.getRoomName());
        panel.add(roomLabel, BorderLayout.NORTH);

        // Add the JTextArea for chat messages
        JTextArea chatTextArea = chatRoom.getChatTextArea();
        chatTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Add a JTextField for typing messages
        JTextField messageField = new JTextField();
        JButton sendButton = new JButton("Send");

        sendButton.addActionListener(e -> {
            String message = messageField.getText();
            sendMessageToServer("CHAT:" + chatRoom.getRoomName() + ":" + message);
            messageField.setText("");
        });

        // Add the input field and send button to a sub-panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }


    private void sendMessageToServer(String message) {
        serverWriter.println("CHAT:" + currentChatRoom.getRoomName() + ":" + clientName + ":" + message);
    }

    private JLabel createStyledTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        label.setFont(new Font("DialogInput", Font.BOLD, 32));
        label.setForeground(new Color(46, 134, 193));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(5, 10, 10, 10)
        ));
        return label;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(JButton.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        button.setFont(new Font("DialogInput", Font.BOLD, 16));
        button.setBackground(new Color(83, 164, 99));
        button.setForeground(Color.BLACK);
        button.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void switchToMainPanel() {
        serverWriter.println("LEAVE_LOBBY");
        CardLayout cardLayout = (CardLayout) currentPanel.getLayout();
        cardLayout.show(currentPanel, "main");
    }

    private void downloadFile(File file) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose Download Location");
        fileChooser.setSelectedFile(new File(file.getName()));

        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File downloadLocation = fileChooser.getSelectedFile();
            String downloadPath = downloadLocation.getAbsolutePath();

            try (InputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = new FileOutputStream(downloadPath)) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                System.out.println("File downloaded to: " + downloadPath);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void filterFiles(String searchTerm) {
        mainPanel.removeAll();
        mainPanel.add(createStyledTitleLabel("File Explorer"));
        mainPanel.add(searchBar);

        JButton searchUploadButton = createStyledButton("Upload File");
        searchUploadButton.addActionListener(e -> performUpload());
        mainPanel.add(searchUploadButton);

        if (searchTerm.isEmpty()) {
            int allFileCounter = 1;
            for (File file : uploadedFiles) {
                addFileButton(file, allFileCounter++, mainPanel);
            }
        } else {
            int filteredFileCounter = 1;
            for (File file : uploadedFiles) {
                if (file.getName().toLowerCase().contains(searchTerm.toLowerCase())) {
                    addFileButton(file, filteredFileCounter++, mainPanel);
                }
            }
        }

        searchBar.requestFocusInWindow();
        frame.revalidate();
        frame.repaint();
    }

    public class ChatRoom {
        private final String roomName;
        private JTextArea chatTextArea;
        private JTextField messageField;  // Add this line

        public ChatRoom(String roomName) {
            this.roomName = roomName;
            this.chatTextArea = new JTextArea();
            this.messageField = new JTextField();  // Initialize the JTextField
        }

        public String getRoomName() {
            return roomName;
        }

        public JTextArea getChatTextArea() {
            return chatTextArea;
        }

        public JTextField getMessageField() {
            return messageField;
        }
    }
}