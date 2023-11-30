import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;

public class DropZone {
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel lobbyPanel;
    private JPanel chatRoomPanel;
    private JPanel currentPanel;
    private List<ChatRoom> chatRooms;
    private ServerGUI serverGUI;
    private Client client;
    private List<File> uploadedFiles;
    private int fileCounter;
    private JPanel chatRoomsPanel;
    private JTextField searchBar;


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

        // Create the Peer to Peer button
        JButton p2pButton = P2PButtonStyle("Peer to Peer");
        p2pButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align the button to the left


        JLabel titleLabel = createStyledTitleLabel("DropZone");

        mainPanel = createMainPanel();
        lobbyPanel = createLobbyPanel();

        // Create the container panel with CardLayout
        currentPanel = new JPanel(new CardLayout());
        currentPanel.add(mainPanel, "main");
        currentPanel.add(lobbyPanel, "lobby");

        // Add the button and title label to the container panel
        containerPanel.add(p2pButton, BorderLayout.WEST);
        containerPanel.add(titleLabel, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(currentPanel);
        frame.add(containerPanel, BorderLayout.NORTH);
        frame.add(scrollPane);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Helper method to create a styled button
    private JButton P2PButtonStyle(String text) {
        JButton button = new JButton(text);
        button.setAlignmentY(Component.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        button.setFont(new Font("DialogInput", Font.BOLD, 16));
        button.setForeground(new Color(46, 134, 193));
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add the ActionListener to switch to the lobby panel
        button.addActionListener(e -> switchToLobbyPanel());

        return button;
    }

    public DropZone() {
        // Initialize the chatRooms list
        chatRooms = new ArrayList<>();
    }


    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK),  // Add a black border
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        panel.setBackground(new Color(173, 216, 230)); // Light Blue Background

        JLabel titleLabel = createStyledTitleLabel("File Explorer");
        panel.add(titleLabel);

        searchBar = new JTextField();
        searchBar.setAlignmentX(JTextField.CENTER_ALIGNMENT);
        searchBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, searchBar.getPreferredSize().height));
        panel.add(searchBar);

        JButton uploadButton = createStyledButton("Upload File");
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(uploadButton);
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
                // Not needed for plain text fields
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
            addFileButton(selectedFile, fileCounter++, mainPanel);
            frame.revalidate();
            frame.repaint();
        }
    }

    private void addFileButton(File file, int fileNumber, JPanel targetPanel) {
        JButton fileButton = new JButton(file.getName(), getFileIcon(file));  // Set the file name and icon
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
                // The action is handled by the downloadFile method in this case
            }
        });

        downloadButton.setText("↓");  // Set the down arrow symbol
        downloadButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return downloadButton;
    }

    private Icon getFileIcon(File file) {
        // Use JFileChooser to get the file icon
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

        // Create a sub-panel for the chat rooms
        chatRoomsPanel = new JPanel();
        chatRoomsPanel.setLayout(new BoxLayout(chatRoomsPanel, BoxLayout.Y_AXIS));

        JScrollPane chatRoomsScrollPane = new JScrollPane(chatRoomsPanel);
        chatRoomsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        JButton createChatRoomButton = createStyledButton("Create Chat Room");
        JButton goBackButton = createStyledButton("Go Back");

        createChatRoomButton.addActionListener(e -> createChatRoom());
        goBackButton.addActionListener(e -> switchToMainPanel());

        panel.add(lobbyLabel);
        panel.add(chatRoomsScrollPane);  // Add scroll pane for chat rooms
        panel.add(createChatRoomButton);
        panel.add(goBackButton);

        return panel;
    }

    // private JPanel createChatRoomPanel() {
    //     JPanel panel = new JPanel();
    //     panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    //     panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    //     panel.setBackground(new Color(135, 206, 250));

    //     JLabel chatLabel = createStyledTitleLabel("P2P Chat Room");
    //     JTextArea chatTextArea = new JTextArea();
    //     chatTextArea.setEditable(false);
    //     JScrollPane chatScrollPane = new JScrollPane(chatTextArea);

    //     panel.add(chatLabel);
    //     panel.add(chatScrollPane);

    //     JButton backButton = createStyledButton("Go Back");
    //     backButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
    //     backButton.addActionListener(e -> switchToLobbyPanel());
    //     panel.add(backButton);

    //     return panel;
    // }

    private void createChatRoom() {
        String roomName = JOptionPane.showInputDialog("Enter Chat Room Name:");
        if (roomName != null && !roomName.trim().isEmpty()) {
            ChatRoom chatRoom = new ChatRoom(roomName);
            chatRooms.add(chatRoom);
            JButton roomButton = createStyledButton(roomName);
            roomButton.addActionListener(e -> switchToChatRoom(chatRoom));

            // Create a numbered label for the chat room
            JLabel chatRoomLabel = new JLabel(chatRooms.size() + ". " + roomName);
            chatRoomLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            chatRoomLabel.setForeground(Color.BLACK);

            // Add the chat room label to the chat rooms panel
            JPanel chatRoomPanel = new JPanel();
            chatRoomPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            chatRoomPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
            chatRoomPanel.add(chatRoomLabel);

            // Add the chat room button and label to the chat rooms panel
            chatRoomsPanel.add(chatRoomPanel);
            chatRoomsPanel.add(roomButton);

            switchToLobbyPanel();  // Automatically switch to the lobby to show the new chat room
            frame.revalidate();
            frame.repaint();
        } else {
            JOptionPane.showMessageDialog(frame, "Chat Room Name cannot be blank. Please enter a valid name.");
        }
    }


    private void switchToLobbyPanel() {
        CardLayout cardLayout = (CardLayout) currentPanel.getLayout();
        cardLayout.show(currentPanel, "lobby");
    }

    private void switchToChatRoom(ChatRoom chatRoom) {
        serverGUI = new ServerGUI();
        new Thread(() -> Server.main(new String[]{chatRoom.getRoomName(), String.valueOf(serverGUI)})).start();

        Client client = new Client();
        new Thread(() -> client.main(new String[]{chatRoom.getRoomName()})).start();

        CardLayout cardLayout = (CardLayout) currentPanel.getLayout();
        cardLayout.show(currentPanel, "chat");
    }

    // private void switchToDropBox() {
    //     // Implement your DropBox logic here
    //     // For simplicity, I'll just display a message for demonstration purposes
    //     JOptionPane.showMessageDialog(frame, "Switching to DropBox!");
    // }

    // private void showFileUploadPanel() {
    //     // Create a file chooser for file uploads
    //     JFileChooser fileChooser = new JFileChooser();
    //     fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    //     int returnValue = fileChooser.showOpenDialog(null);
    //     if (returnValue == JFileChooser.APPROVE_OPTION) {
    //         File selectedFile = fileChooser.getSelectedFile();
    //         uploadedFiles.add(selectedFile);
    //         addFileButton(selectedFile, fileCounter++, mainPanel);
    //         frame.revalidate();
    //         frame.repaint();
    //     }
    // }

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

    // private JButton createGoBackButton() {
    //     JButton goBackButton = new JButton("Go Back");
    //     goBackButton.addActionListener(e -> switchToMainPanel());
    //     return goBackButton;
    // }

    private void switchToMainPanel() {
        CardLayout cardLayout = (CardLayout) currentPanel.getLayout();
        cardLayout.show(currentPanel, "main");
    }

    // private void showMessage(String title, String message) {
    //     JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
    // }

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

    class ChatRoom {
        private final String roomName;

        public ChatRoom(String roomName) {
            this.roomName = roomName;
        }

        public String getRoomName() {
            return roomName;
        }
    }
}
