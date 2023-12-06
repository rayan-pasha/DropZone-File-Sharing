import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DB {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/dropzone";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static void init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection open() {

        Connection connection;
        try {
            connection = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public static void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void insertDownload(String user, String file) {

        Connection connection = open();
        try {
            String query = "INSERT INTO downloads (`key`, `user`, `file`, `time`) VALUES (DEFAULT, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, user);
            preparedStatement.setString(2, file);
            preparedStatement.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));
            preparedStatement.executeUpdate();
            preparedStatement.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close(connection);
        }

    }

    public static void printDownloadList() {

        Connection connection = open();
        try {

            String query = "SELECT * FROM downloads";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("key");
                String user = resultSet.getString("user");
                String file = resultSet.getString("file");
                java.sql.Timestamp time = resultSet.getTimestamp("time");
                System.out.println("Download (ID: " + id + ", User: " + user + ", File: " + file + ", Time: " + time + ")");
            }

            resultSet.close();
            preparedStatement.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close(connection);
        }

    }

    public static void insertUpload(String user, String file) {

        Connection connection = open();
        try {
            String query = "INSERT INTO uploads (`key`, `user`, `file`, `time`) VALUES (DEFAULT, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, user);
            preparedStatement.setString(2, file);
            preparedStatement.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));
            preparedStatement.executeUpdate();
            preparedStatement.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close(connection);
        }

    }

    public static void printUploadList() {

        Connection connection = open();
        try {

            String query = "SELECT * FROM uploads";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt("key");
                String user = resultSet.getString("user");
                String file = resultSet.getString("file");
                java.sql.Timestamp time = resultSet.getTimestamp("time");
                System.out.println("Upload (ID: " + id + ", User: " + user + ", File: " + file + ", Time: " + time + ")");
            }

            resultSet.close();
            preparedStatement.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            close(connection);
        }

    }

    public static void main(String[] args) throws ClassNotFoundException {

        DB.init();

        DB.insertDownload("user1", "file1");
        DB.printDownloadList();

        DB.insertUpload("user2", "file2");
        DB.printUploadList();

    }
}
