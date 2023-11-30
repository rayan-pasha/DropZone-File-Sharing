import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.filechooser.FileSystemView;

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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new DropZone().createAndShowGUI());
    }

    private void createAndShowGUI() {
        frame = new JFrame("DropZone - P2P Lobby");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        mainPanel = createMainPanel();
        lobbyPanel = createLobbyPanel();
        chatRoomPanel = createChatRoomPanel();
        chatRooms = new ArrayList<>();

        CardLayout cardLayout = new CardLayout();
        currentPanel = new JPanel(cardLayout);
        currentPanel.add(mainPanel, "main");
        currentPanel.add(lobbyPanel, "lobby");
        currentPanel.add(chatRoomPanel, "chat");

        frame.add(currentPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createMainPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(173, 216, 230)); // Light Blue Background


        JButton p2pButton = createStyledButton("P2P");
        JButton dropBoxButton = createStyledButton("DropBox");
        JButton uploadButton = createStyledButton("Upload");


        // Set text color to black
        p2pButton.setForeground(Color.BLACK);
        dropBoxButton.setForeground(Color.BLACK);
        uploadButton.setForeground(Color.BLACK);


        // Add borders to buttons
        p2pButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        dropBoxButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        uploadButton.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));


        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
        titlePanel.add(p2pButton);
        titlePanel.add(dropBoxButton);
        titlePanel.add(uploadButton);


        panel.add(titlePanel);

        uploadedFiles = new ArrayList<>();
        fileCounter = 1;


        uploadButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                uploadedFiles.add(selectedFile);
                addFileButton(selectedFile, fileCounter++, panel);
                frame.revalidate();
                frame.repaint();
            }
        });


        p2pButton.addActionListener(e -> switchToLobbyPanel());
        dropBoxButton.addActionListener(e -> switchToDropBox());


        return panel;
    }


    private JPanel createLobbyPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(135, 206, 250));


        JLabel lobbyLabel = createStyledTitleLabel("P2P Lobby");


        // Create a sub-panel for the chat rooms
        chatRoomsPanel = new JPanel();  // Use the class-level variable
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


    private JPanel createChatRoomPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(135, 206, 250)); // DodgerBlue Background


        JLabel chatLabel = createStyledTitleLabel("P2P Chat Room");
        JTextArea chatTextArea = new JTextArea();
        chatTextArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatTextArea);


        panel.add(chatLabel);
        panel.add(chatScrollPane);


        JButton backButton = createStyledButton("Go Back");
        backButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        backButton.addActionListener(e -> switchToLobbyPanel());
        panel.add(backButton);




        return panel;
    }


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


    private void switchToDropBox() {
        // Implement your DropBox logic here
        // For simplicity, I'll just display a message for demonstration purposes
        JOptionPane.showMessageDialog(frame, "Switching to DropBox!");
    }


    private void switchToUpload() {
        // Implement your Upload logic here
        // For simplicity, I'll just display a message for demonstration purposes
        JOptionPane.showMessageDialog(frame, "Switching to Upload!");
    }


    private void showFileUploadPanel() {
       

        // Create a file chooser for file uploads
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);


        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            uploadedFiles.add(selectedFile);
        }


     
    }



    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(new Font("DialogInput", Font.BOLD, 16));
        button.setBackground(new Color(173, 216, 230)); // Light Blue Background
        button.setForeground(Color.BLACK);
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }


    private JLabel createStyledTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        label.setFont(new Font("DialogInput", Font.BOLD, 32));
        label.setForeground(new Color(0, 0, 0)); // Black Font
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return label;
    }


    private void addFileButton(File file, int fileNumber, JPanel targetPanel) {
        JButton fileButton = new JButton(file.getName(), getFileIcon(file));
        fileButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        fileButton.setFont(new Font("Arial", Font.PLAIN, 14));
        fileButton.setForeground(Color.BLACK);
        fileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));


        JLabel fileNumberLabel = new JLabel(fileNumber + ". ");
        fileNumberLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        fileNumberLabel.setForeground(Color.BLACK);


        JPanel filePanel = new JPanel();
        filePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        filePanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        filePanel.add(fileNumberLabel);
        filePanel.add(fileButton);


        fileButton.addActionListener(e -> downloadFile(file));


        targetPanel.add(filePanel);
    }


    private JButton createGoBackButton() {
        JButton goBackButton = new JButton("Go Back");
        goBackButton.addActionListener(e -> switchToMainPanel());
        return goBackButton;
    }


    private void switchToMainPanel() {
        CardLayout cardLayout = (CardLayout) currentPanel.getLayout();
        cardLayout.show(currentPanel, "main");
    }


    private void showMessage(String title, String message) {
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.INFORMATION_MESSAGE);
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
    private Icon getFileIcon(File file) {
        FileSystemView view = FileSystemView.getFileSystemView();
        return view.getSystemIcon(file);
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
