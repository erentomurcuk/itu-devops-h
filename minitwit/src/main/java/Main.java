import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello Minitwit");

        port(8080);

        staticFiles.location("/static");
        get("/hello", (req, res) -> "Hello");
    }
}
