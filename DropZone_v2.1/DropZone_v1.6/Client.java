import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Client {
    private PrintWriter out;
    private String clientName;
    private JTextArea chatArea;
    private JTextField messageField;
    private List<String> clientNames = new ArrayList<>();
    private JButton uploadFileButton;
    private boolean isReceivingFile = false;
    private JFrame frame;

    // Declare downloadButton
    private JButton downloadButton;

    public Client() {
        while (true) {
            clientName = JOptionPane.showInputDialog("Enter your username:");
            if (clientName != null && !clientName.trim().isEmpty()) {
                break;
            } else {
                JOptionPane.showMessageDialog(null, "Username cannot be blank. Please enter a valid username.");
            }
        }

        frame = new JFrame("P2P Chat - " + clientName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        chatArea = new JTextArea();
        chatArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        messageField = new JTextField();
        JButton sendButton = new JButton("Send");
        JButton exitButton = new JButton("Exit");

        sendButton.addActionListener(e -> {
            sendMessage(messageField.getText());
            messageField.setText("");
        });

        exitButton.addActionListener(e -> {
            sendExitMessage();
            System.exit(0); // Terminate the client application
        });

        messageField.addActionListener(e -> {
            sendMessage(messageField.getText());
            messageField.setText("");
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(messageField, BorderLayout.CENTER);
        buttonPanel.add(sendButton, BorderLayout.EAST);
        buttonPanel.add(exitButton, BorderLayout.WEST);

        uploadFileButton = createStyledButton("Upload File");
        downloadButton = createStyledButton("Download File"); // Initialize download button

        JPanel fileButtonPanel = new JPanel(new GridLayout(1, 2)); // Panel to hold upload and download buttons
        fileButtonPanel.add(uploadFileButton);
        fileButtonPanel.add(downloadButton);

        uploadFileButton.addActionListener(e -> performUpload());
        downloadButton.addActionListener(e -> handleFileDownloadRequest());

        buttonPanel.add(fileButtonPanel, BorderLayout.NORTH); // Add the file buttons panel to the main button panel

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
                        } else if ("/FILE/".equalsIgnoreCase(message)) {
                            handleFileDownload(in);
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

    private void handleFileDownload(BufferedReader in) {
        try {
            String fileName = in.readLine();
            String fileData = in.readLine();
            byte[] decodedData = Base64.getDecoder().decode(fileData);

            File saveFile = new File("download/" + fileName);

            try (FileOutputStream fileOutputStream = new FileOutputStream(saveFile)) {
                fileOutputStream.write(decodedData);
            }

            chatArea.append("File received: " + saveFile.getAbsolutePath() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestFileDownload(String fileName) {
        out.println("/DOWNLOAD/");
        out.println(fileName);
    }

    class FileUploadDialog extends JDialog {
        private File selectedFile;
        private JFileChooser fileChooser;
        private Client client;

        public FileUploadDialog(Client client) {
            super(client.frame, "Select File", true);
            this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            this.setSize(400, 300);
            this.setLayout(new BorderLayout());
            this.client = client;

            fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Choose File to Upload");

            this.add(fileChooser, BorderLayout.CENTER);

            JButton uploadButton = new JButton("Upload");
            uploadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedFile = fileChooser.getSelectedFile();
                    client.sendSelectedFile(selectedFile);
                    // Dispose the dialog after the file transfer is complete
                    dispose();
                }
            });

            this.add(uploadButton, BorderLayout.SOUTH);
        }

        public void showDialog() {
            this.setVisible(true);
        }
    }

    private void performUpload() {
        String targetClientName = JOptionPane.showInputDialog("Enter the target client's name:");
        if (targetClientName != null && !targetClientName.trim().isEmpty()) {
            FileUploadDialog uploadDialog = new FileUploadDialog(this);
            uploadDialog.showDialog();
        } else {
            JOptionPane.showMessageDialog(null, "Invalid target client name. Please enter a valid name.");
        }
    }

    private void sendMessage(String message) {
        out.println(message);
    }

    private void sendExitMessage() {
        out.println("/EXIT/");  // Notify the server that the client is exiting
    }

    private void updateClientList(String clients) {
        String[] names = clients.split(",");
        clientNames.clear();
        for (String name : names) {
            clientNames.add(name.trim());
        }
        // Do something with the updated client names list if needed
    }

    public void sendSelectedFile(File selectedFile) {
        if (selectedFile != null) {
            sendFile(selectedFile);
        }
    }

    private void sendFile(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {

            out.println("/FILE/");  // Notify the server that a file is being sent

            out.println(file.getName());  // Send the file name to the server

            byte[] fileData = new byte[(int) file.length()];
            bufferedInputStream.read(fileData, 0, fileData.length);

            out.println(Base64.getEncoder().encodeToString(fileData));  // Send the file data to the server

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleFileDownloadRequest() {
        String fileName = JOptionPane.showInputDialog("Enter the file name to download:");
        if (fileName != null && !fileName.trim().isEmpty()) {
            requestFileDownload(fileName);
        } else {
            JOptionPane.showMessageDialog(null, "Invalid file name. Please enter a valid file name.");
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
