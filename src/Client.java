import javax.swing.*;
import java.io.PrintWriter;


public class Client {
    private static PrintWriter out;


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ClientGUI());
    }
}
