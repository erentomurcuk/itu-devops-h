import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.security.crypto.bcrypt.BCrypt;
import spark.*;
import static spark.Spark.*;
import org.apache.velocity.Template;
import spark.resource.ClassPathResource;

import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WebApplication {
    public static class Templates {
        public static final String PUBLIC_TIMELINE = "/templates/timeline.vm";
        public static final String REGISTER = "/templates/register.vm";
    }
    public static class URLS {
        public static final String PUBLIC_TIMELINE = "/public";
        public static final String USER_TIMELINE = "/<username>"; // TODO
        public static final String USER = "/";
        public static final String REGISTER = "/register";
    }

    public static int PER_PAGE = 30;

    public static void main(String[] args) {
        System.out.println("Hello Minitwit");

        port(8080);

        staticFiles.location("/static");
        get("/hello", (req, res) -> "Hello");
        get(URLS.PUBLIC_TIMELINE, WebApplication.servePublicTimelinePage);
        get(URLS.REGISTER, WebApplication.serveRegisterPage);
        post(URLS.REGISTER, WebApplication.serveRegisterPage);
        get(URLS.USER, WebApplication.serveUserTimelinePage);
    }

    public static int getUserID(SQLite db, String username) throws SQLException {
        var conn = db.getConnection();
        var statement = conn.prepareStatement("select user_id from user where username = ?");

        statement.setString(1, username);
        ResultSet rs = statement.executeQuery();

        // No user with that username found
        if (rs.isClosed()) {
            return 0;
        }
        
        return rs.getInt("user_id");
    }

    public static String render(Map<String, Object> model, String templatePath) {
        try {
            VelocityEngine engine = new VelocityEngine();
            // Required for Velocity to know where resources ends up
            engine.setProperty("resource.loader", "class");
            engine.setProperty("class.resource.loader.class","org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

            engine.init();

            Template template = engine.getTemplate(templatePath);

            // Set up variables available in templates
            VelocityContext ctx = new VelocityContext(model);
            ctx.put("urls", WebApplication.URLS.class);
            model.forEach((k, v) -> ctx.put(k, v));

            // "Run" template and return result
            StringWriter writer = new StringWriter();
            template.merge(ctx, writer);
            return writer.toString();
        } catch (Exception e) {
            // If a error happens above, print it and return it as a response
            e.printStackTrace();
            return e.toString();
        }
    }

    public static Route servePublicTimelinePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        // TODO: Get logged in user (if any)
        // model.put("user", loggedInUser);
        // TODO: Port flask "flashes"
        model.put("messages", new ArrayList<String>() {
            {
                add("Hello minitwit");
            }
        });
        model.put("splash", URLS.PUBLIC_TIMELINE);
        // Where does this come from in python?
        model.put("title", "Public timeline");

        return WebApplication.render(model, WebApplication.Templates.PUBLIC_TIMELINE);
    };

    public static Route serveUserTimelinePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        // TODO: Get logged in user (if any)
        if (false) {
            response.redirect(URLS.PUBLIC_TIMELINE);
        }
        // TODO: Port flask "flashes"

        model.put("splash", URLS.USER);
        // Where does this come from in python?
        model.put("title", "Public timeline");

        var db = new SQLite();
        var conn = db.getConnection();
        var statement = conn.prepareStatement(
            "        select message.*, user.* from message, user\n" +
                "        where message.flagged = 0 and message.author_id = user.user_id and (\n" +
                "            user.user_id = ? or\n" +
                "            user.user_id in (select whom_id from follower\n" +
                "                                    where who_id = ?))\n" +
                "        order by message.pub_date desc limit ?");

        statement.setInt(1, 1); // TODO: user id
        statement.setInt(2, 1); // TODO: user id
        statement.setInt(3, WebApplication.PER_PAGE);
        ResultSet rs = statement.executeQuery();

        var results = new ArrayList<HashMap<String, Object>>();
        while (rs.next()) {
            var result = new HashMap();
            result.put("message_id", rs.getInt("message_id"));
            result.put("author_id", rs.getInt("author_id"));
            result.put("text", rs.getString("text"));
            result.put("pub_date", rs.getString("pub_date")); // Type?
            result.put("flagged", rs.getInt("flagged"));
            result.put("username", rs.getString("username"));
            result.put("email", rs.getString("email"));
            result.put("pw_hash", rs.getString("pw_hash"));
            results.add(result);
        }
        model.put("messages", results);

        return WebApplication.render(model, WebApplication.Templates.PUBLIC_TIMELINE);
    };

    public static Route serveRegisterPage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        try {
            var db = new SQLite();
            var conn = db.getConnection();
            var insert = conn.prepareStatement("insert into user (\n" +
                    "                username, email, pw_hash) values (?, ?, ?)");


            // TODO: Get logged in user (if any)
            // if (userIsLoggedIn) {
            //     response.redirect(URLS.LOGIN);
            //     return;
            // }

            // TODO: Port flask "flashes"
            model.put("messages", new ArrayList<String>() {});
            //model.put("splash", URLS.PUBLIC_TIMELINE);
            model.put("username", request.queryParams("username") == null ? "" : request.queryParams("username"));
            model.put("email", request.queryParams("email") == null ? "" : request.queryParams("email"));

            if (request.requestMethod().equals("POST")) {
                if (request.queryParams("username") == null
                        || request.queryParams("username").equals("")) {
                    model.put("error", "You have to enter a username");
                }
                else if (request.queryParams("email") == null
                        || request.queryParams("email").equals("")
                        || !request.queryParams("email").contains("@")) {
                    model.put("error", "You have to enter a valid email");
                }
                else if (request.queryParams("password") == null
                        || request.queryParams("password2") == null) {
                    model.put("error", "You have to enter a password");
                }
                else if (!request.queryParams("password").equals(request.queryParams("password2"))) {
                    model.put("error", "YThe two passwords do not match");
                }
                else if (getUserID(db, request.queryParams("username")) != 0) {
                    model.put("error", "The username is already taken");
                }
                else {
                    String saltedPW = BCrypt.hashpw(request.queryParams("password"), BCrypt.gensalt());

                    insert.setString(1, request.queryParams("username"));
                    insert.setString(2, request.queryParams("email"));
                    insert.setString(3, saltedPW);
                    insert.execute();

                    // TODO: splash "You were successfully registered and can login now"

                    response.redirect(/*URLS.LOGIN*/ "/login"); // TODO: use constant
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }

        return WebApplication.render(model, WebApplication.Templates.REGISTER);
    };
}
