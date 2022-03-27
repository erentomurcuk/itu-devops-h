import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlDatabase {

    private Connection connection = null;

    public Connection connect() throws SQLException {
        String password = System.getenv("MINITWIT_DB_PASS");
        String username = System.getenv("MINITWIT_DB_USER");
        String url = System.getenv("MINITWIT_DB_URL");

        if (password == null) {
            throw new IllegalStateException("Password for the database is required");
        } else if (username == null) {
            throw new IllegalStateException("Username for the database is required");
        } else if (url == null) {
            throw new IllegalStateException("URL for the database is required");
        }

        connection = DriverManager.getConnection(url, username, password);

        return connection;
    }

    public Connection getConnection() throws SQLException {
        if (connection != null && !connection.isValid(1)) {
            return connection;
        } else {
            return connect();
        }
    }

    public void init() throws SQLException, URISyntaxException, IOException {
        Connection connection = getConnection();

        String schema;
        InputStream in = getClass().getResourceAsStream("schema.sql");
        schema = new String(in.readAllBytes());

        String[] schemaStatements = schema.split(";");

        // Run the sql in the schema file on the database
        Statement statement = connection.createStatement();

        for (String statementLine: schemaStatements) {
            System.out.println(statementLine.trim());
            statement.executeUpdate(statementLine.trim());
        }
    }

    public static void main(String[] args) {
        SqlDatabase db = new SqlDatabase();
        try {
            db.init();
            System.out.println("Done!");
        } catch (SQLException e) {
            System.out.println("SQL Error:");
            e.printStackTrace();
            System.exit(1);
        } catch (URISyntaxException e) {
            System.out.println("Bad file URI:");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error reading file:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
