import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import spark.*;
import static spark.Spark.*;
import org.apache.velocity.Template;
import spark.resource.ClassPathResource;

import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public static void main(String[] args) {
        System.out.println("Hello Minitwit");

        port(8080);

        staticFiles.location("/static");
        get("/hello", (req, res) -> "Hello");
        get(URLS.PUBLIC_TIMELINE, WebApplication.servePublicTimelinePage);
        get(URLS.REGISTER, WebApplication.serveRegisterPage);
        post(URLS.REGISTER, WebApplication.serveRegisterPage);
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

    public static Route serveRegisterPage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();

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
            // TODO: get_user_id
            else {
                try {
                    var conn = new SQLite().getConnection();
                    var insert = conn.prepareStatement("insert into user (\n" +
                            "                username, email, pw_hash) values (?, ?, ?)");
                    insert.setString(1, request.queryParams("username"));
                    insert.setString(2, request.queryParams("email"));
                    insert.setString(3, request.queryParams("password"));
                    insert.execute();

                    // TODO: splash "You were successfully registered and can login now"

                    response.redirect(/*URLS.LOGIN*/ "/login"); // TODO: use constant
                } catch (Exception e) {
                    e.printStackTrace();
                    return e.toString();
                }
            }
        }

        return WebApplication.render(model, WebApplication.Templates.REGISTER);
    };
}
