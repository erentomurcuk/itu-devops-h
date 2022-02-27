import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLite {
    private Connection connection = null;

    public Connection connect() throws SQLException {
        var file = System.getenv("MINITWIT_DB_PATH");
        if (file == null) {
            file = "minitwit.db";
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + file);
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
