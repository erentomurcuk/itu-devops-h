import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLite {
    private Connection connection = null;

    public Connection connect() throws SQLException {
        // create a database connection
        connection = DriverManager.getConnection("jdbc:sqlite:minitwit.db");
        return connection;
    }

    public Connection getConnection() throws SQLException {
        if (connection != null) {
            return connection;
        } else {
            return connect();
        }
    }

    public void init() throws SQLException, URISyntaxException, IOException {
        Connection connection = getConnection();

        // Read the schema file from resources
        URL schemaResource = getClass()
                .getClassLoader()
                .getResource("schema.sql");
        Path schemaPath = Path.of(schemaResource.toURI());
        String schema = Files.readString(schemaPath);
        String[] schemaStatements = schema.split(";");

        // Run the sql in the schema file on the database
        Statement statement = connection.createStatement();
        for (String statementLine:
                schemaStatements) {
            System.out.println(statementLine.trim());
            try {
                ResultSet rs = statement.executeQuery(statementLine.trim());
                rs.next();
            } catch (Exception e) {
                // Someone more clever than me please change above to
                // statement.execute(statementLine.trim());
                // and remove rs part.
                // Exceptions are expected because executeQuery expects the
                // query to return a ResultSet but some that we use doesn't.
            }
        }
        //statement.close();
    }


    public static void main(String[] args) {
        SQLite db = new SQLite();
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
