import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;


public class MainFrame {
    public static void main(String[] args) {
        JFrame frame = new JFrame("File Upload and Download");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);


        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));


        JButton uploadButton = new JButton("Upload File");
        uploadButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        panel.add(uploadButton);


        ArrayList<File> uploadedFiles = new ArrayList<>();


        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    uploadedFiles.add(selectedFile);
                    JLabel fileLabel = new JLabel(selectedFile.getName());
                    fileLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
                    fileLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                        public void mouseClicked(java.awt.event.MouseEvent evt) {
                            if (evt.getClickCount() == 1) {
                                downloadFile(selectedFile);
                            }
                        }
                    });
                    panel.add(fileLabel);
                    frame.revalidate();
                    frame.repaint();
                }
            }
        });


        frame.add(panel);
        frame.setVisible(true);
    }


    public static void downloadFile(File file) {
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
}

