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
        public static final String LOGIN_SCREEN = "/templates/login.vm";
    }
    public static class URLS {
        public static final String PUBLIC_TIMELINE = "/public";
        public static final String USER_TIMELINE = "/<username>"; // TODO
        public static final String USER = "/";
        public static final String LOGIN_PAGE = "/login";
    }

    public static void main(String[] args) {
        System.out.println("Hello Minitwit");

        port(8080);

        staticFiles.location("/static");
        get("/hello", (req, res) -> "Hello");
        get(URLS.PUBLIC_TIMELINE, WebApplication.servePublicTimelinePage);
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
        model.put("login", URLS.LOGIN_PAGE);

        return WebApplication.render(model, WebApplication.Templates.PUBLIC_TIMELINE);
    };

    public static Route serveLoginPage = (Request request, Response response) -> {
        var db = new SQLite();
        var connection = db.getConnection();
        var insert = connection.prepareCall("select * from user where\n" +
                "            username = ?");
        Map<String, Object> model = new HashMap<>();
        //model.put("messages", new ArrayList<>());
        return render(model, Templates.LOGIN_SCREEN);
    };

    public static Route handleLoginPost = (Request request, Response response) -> {
        return render(new HashMap<>(), Templates.PUBLIC_TIMELINE);
    };

    /*public static Route handleLoginPost = (Request request, Response response) -> {
        Map<String,Object> model = new HashMap<>();
        model.put("u
    }/**/
}
