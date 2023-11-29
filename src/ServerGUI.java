import javax.swing.*;
import java.awt.*;


public class ServerGUI extends JFrame {
    private JLabel connectedClientsLabel;


    public ServerGUI() {
        setTitle("P2P Chat Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 100);


        connectedClientsLabel = new JLabel("Connected Clients: 0");


        add(connectedClientsLabel, BorderLayout.CENTER);


        setVisible(true);
    }


    public void displayConnectedClients(int numberOfClients) {
        connectedClientsLabel.setText("Connected Clients: " + numberOfClients);
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerGUI());
    }
}
