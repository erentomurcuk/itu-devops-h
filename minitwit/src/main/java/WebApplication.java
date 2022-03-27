import Metrics.PrometheusMetrics;
import Metrics.TimerStopper;
import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import com.timgroup.jgravatar.Gravatar;
import com.timgroup.jgravatar.GravatarDefaultImage;
import com.timgroup.jgravatar.GravatarRating;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.DateTool;
import org.springframework.security.crypto.bcrypt.BCrypt;
import spark.*;
import static spark.Spark.*;
import org.apache.velocity.Template;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WebApplication {

    public static class Templates {
        public static final String PUBLIC_TIMELINE = "/templates/timeline.vm";
        public static final String LOGIN = "/templates/login.vm";
        public static final String REGISTER = "/templates/register.vm";
    }

    public static class URLS {
        public static final String PUBLIC_TIMELINE = "/public";
        public static final String USER_TIMELINE = "/:username";
        public static final String USER = "/";
        public static final String LOGIN = "/login";
        public static final String LOGOUT = "/logout";
        public static final String REGISTER = "/register";
        public static final String ADD_MESSAGE = "/add_message";
        public static final String FOLLOW = "/:username/follow";
        public static final String UNFOLLOW = "/:username/unfollow";


        /**
         * Transforms a url with :parameters using a map with keys and values
         * @param url to use
         * @param values map of parameters and their values, must be String or Integer values
         * @return url with parameters replaces with values
         * @throws Exception
         */
        public static String urlFor(String url, Map<String, Object> values) throws Exception {
            String u = url;
            for (String key: values.keySet()) {
                if (values.get(key) instanceof String) {
                    u = u.replace(":" + key, (String) values.get(key));
                } else if (values.get(key) instanceof Integer) {
                    u = u.replace(":" + key, String.valueOf(values.get(key)));
                } else {
                    u = u.replace(":" + key, "" + values.get(key));
                }
            }

            return u;
        }

        public static String urlFor(String url) throws Exception {
            return urlFor(url, new HashMap<String, Object>());
        }

        // Sim API
        public static final String SIM_MESSAGES = "/msgs";
        public static final String SIM_REGISTER = "/register";
        public static final String SIM_LATEST = "/latest";
        public static final String SIM_MSGS_USR = "/msgs/:username";
        public static final String SIM_FLLWS = "/fllws/:username";
    }

    public static int PER_PAGE = 30;

    public static Gson gson = new Gson();

    public static PrometheusMetrics metrics = new PrometheusMetrics();

    // The ID of the latest request made by the simulator
    public static int LATEST = 0;

    private static final String USERNAME = "username";

    private static final Gravatar gravatar = new Gravatar()
            .setSize(48)
            .setRating(GravatarRating.GENERAL_AUDIENCES)
            .setDefaultImage(GravatarDefaultImage.IDENTICON);

    private static final String METRIC_TYPE_WEB = "web";
    private static final String METRIC_TYPE_API = "api";
    private static final String METRIC_TYPE_METRICS = "metrics";

    public static void main(String[] args) {
        System.out.println("Hello Minitwit");

        var port = System.getenv("MINITWIT_PORT");
        if (port == null) {
            port = "8080";
        }
        port(Integer.parseInt(port));

        staticFiles.location("/static");

        before("/*", (req, res) -> {
            req.attribute("metrics", metrics);
            if (!req.uri().startsWith("/static")) {
                req.attribute("startTime", System.nanoTime());
            }
            // Setup initial session state once
            if (req.session().isNew()) {
                req.session().attribute("alerts", new ArrayList<>());
            }
        });

        after("/*", (req, res) -> {
            long startTime = req.attribute("startTime");
            if (startTime != 0) {
                long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
                metrics.observeRequestTime(
                        time,
                        req.uri().startsWith("/api")
                                ? METRIC_TYPE_API
                                : req.uri().startsWith("/metrics") ? METRIC_TYPE_METRICS : METRIC_TYPE_WEB,
                        res.status()
                );
            }
            // TODO: currently doesn't log query parameters or unusual headers
            System.out.println(LocalDateTime.now() + " - " + req.uri() + " - " + res.status());
        });

        //before("/metrics", protectEndpoint("Basic asdf"));
        get("/metrics", catchRoute(WebApplication.serveMetrics));
        get("/metrics/stats", catchRoute(WebApplication.serveStats), gson::toJson);

        get(URLS.PUBLIC_TIMELINE, catchRoute(WebApplication.servePublicTimelinePage));
        get(URLS.REGISTER, catchRoute(WebApplication.serveRegisterPage));
        get(URLS.LOGIN, catchRoute(WebApplication.serveLoginPage));
        get(URLS.LOGOUT, catchRoute(WebApplication.handleLogoutRequest));
        get(URLS.USER, catchRoute(WebApplication.serveUserTimelinePage));
        get(URLS.USER_TIMELINE, catchRoute(WebApplication.serveUserByUsernameTimelinePage));

        post(URLS.REGISTER, catchRoute(WebApplication.serveRegisterPage));
        post(URLS.ADD_MESSAGE, catchRoute(WebApplication.add_message));
        get(URLS.FOLLOW, catchRoute(WebApplication.serveFollowPage));
        get(URLS.UNFOLLOW, catchRoute(WebApplication.serveUnfollowPage));
        post(URLS.LOGIN, catchRoute(WebApplication.serveLoginPage));


        // Sim API
        path("/api", () -> {
            // All endpoints in this path must be authenticated
            // Auth code is simulator:super_safe!
            before("/*", protectEndpoint("Basic c2ltdWxhdG9yOnN1cGVyX3NhZmUh"));

            get(URLS.SIM_MESSAGES, catchRoute(WebApplication.serveSimMsgs), gson::toJson);
            post(URLS.SIM_REGISTER, catchRoute(WebApplication.serveSimRegister)); // Handles JSON on its own
            get(URLS.SIM_LATEST, catchRoute(WebApplication.serveSimLatest), gson::toJson);
            get(URLS.SIM_MSGS_USR, catchRoute(WebApplication.serveSimMsgsUsr), gson::toJson);
            post(URLS.SIM_MSGS_USR, catchRoute(WebApplication.serveSimMsgsUsr), gson::toJson);
            post(URLS.SIM_FLLWS, catchRoute(WebApplication.serveSimFllws));
            get(URLS.SIM_FLLWS, catchRoute(WebApplication.serveSimFllws));
        });

    }

    public static Route catchRoute(Route r) {
        Route catcher = (Request request, Response response) -> {
            try {
                return r.handle(request, response);
            } catch (Exception e) {
                response.status(500);
                e.printStackTrace();
                return e.toString();
            }
        };

        return catcher;
    }

    public static ResultSet getUser(Connection conn, Integer userId) throws SQLException {
        if (userId == null) return null;

        var statement = conn.prepareStatement("select * from \"user\" where user_id = ?");

        statement.setInt(1, userId);
        ResultSet rs = statement.executeQuery();

        return rs;
    }

    public static int getUserID(SqlDatabase db, String username) throws SQLException {
        var conn = db.getConnection();
        var statement = conn.prepareStatement("select user_id from \"user\" where username = ?");

        statement.setString(1, username);
        ResultSet rs = statement.executeQuery();

        // No user with that username found
        if (rs.isClosed() || !rs.next()) {
            conn.close();
            return 0;
        }

        var userID = rs.getInt("user_id");
        conn.close();
        return userID;
    }

    public static ArrayList<HashMap<String, Object>> getMessages() throws SQLException {
        var db = new SqlDatabase();
        var conn = db.getConnection();

        var messageStmt = conn.prepareStatement(
                "select message.*, \"user\".* from message, \"user\"\n" +
                        "        where message.flagged = 0 and message.author_id = \"user\".user_id\n" +
                        "        order by message.pub_date desc limit ?");
        messageStmt.setInt(1, PER_PAGE);
        var messages = new ArrayList<HashMap<String, Object>>();
        var messageRs = messageStmt.executeQuery();
        while (messageRs.next()) {
            HashMap<String, Object> result = new HashMap<>();
            result.put("message_id", messageRs.getInt("message_id"));
            result.put("author_id", messageRs.getInt("author_id"));
            result.put("text", messageRs.getString("text"));
            result.put("pub_date", messageRs.getInt("pub_date"));
            result.put("flagged", messageRs.getInt("flagged"));
            result.put(USERNAME, messageRs.getString(USERNAME));
            result.put("email", messageRs.getString("email"));
            messages.add(result);
        }

        conn.close();

        return messages;
    }

    public static String render(Session session, Map<String, Object> model, String templatePath) {
        VelocityEngine engine = new VelocityEngine();
        // Required for Velocity to know where resources ends up
        engine.setProperty("resource.loader", "class");
        engine.setProperty("class.resource.loader.class","org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

        engine.init();

        Template template = engine.getTemplate(templatePath);

        // Set up variables available in templates
        VelocityContext ctx = new VelocityContext(model);
        ctx.put("webapp", WebApplication.class);
        ctx.put("urls", WebApplication.URLS.class);     //I think this is redundant no?
        //This is the easiest way I could insert constants into the context. Maybe not elegant, but it works.
        ctx.put("USER", URLS.USER);
        ctx.put("LOGIN", URLS.LOGIN);
        ctx.put("LOGOUT", URLS.LOGOUT);
        ctx.put("PUBLIC_TIMELINE", URLS.PUBLIC_TIMELINE);
        ctx.put("USER_TIMELINE", URLS.USER_TIMELINE);
        ctx.put("REGISTER",URLS.REGISTER);
        ctx.put("ADD_MESSAGE", URLS.ADD_MESSAGE);
        ctx.put("FOLLOW",URLS.FOLLOW);
        ctx.put("UNFOLLOW", URLS.UNFOLLOW);
        model.forEach((k, v) -> ctx.put(k, v));         //I think this might also be redundant

        ctx.put("date", new DateTool());

        ctx.put("alerts", session.attribute("alerts"));

        if (!ctx.containsKey("title")) {
            ctx.put("title", "Welcome");
        }

        // "Run" template and return result
        StringWriter writer = new StringWriter();
        template.merge(ctx, writer);
        ((List<String>) session.attribute("alerts")).clear();
        return writer.toString();
    }

    private static void addAlert(Session session, String message) {
        ((List<String>) session.attribute("alerts")).add(message);
    }

    // Used by timeline.vm to display user's gravatar
    public static String getGravatarURL(String email) {
        return gravatar.getUrl(email);
    }

    public static String register(String username, String email, String password, String password2) throws SQLException {
        var db = new SqlDatabase();
        if (username == null
                || username.equals("")) {
            return "You have to enter a username";
        }
        else if (email == null
                || email.equals("")
                || !email.contains("@")) {
            return "You have to enter a valid email";
        }
        else if (password == null
                || password2 == null) {
            return "You have to enter a password";
        }
        else if (!password.equals(password2)) {
            return "The two passwords do not match";
        }
        else if (getUserID(db, username) != 0) {
            return "The username is already taken";
        }
        else {
            String saltedPW = BCrypt.hashpw(password, BCrypt.gensalt());

            var conn = new SqlDatabase().getConnection();
            var insert = conn.prepareStatement("insert into \"user\" (\n" +
                    "                username, email, pw_hash) values (?, ?, ?)");

            insert.setString(1, username);
            insert.setString(2, email);
            insert.setString(3, saltedPW);

            insert.execute();

            conn.close();

            return null;
        }
    }

    public static void updateLatest(Request request) {
        String latest = request.queryParams("latest");
        if (latest != null) {
            LATEST = Integer.parseInt(latest);
        }
    }

    public static Route add_message = (Request request, Response response)  -> {
        Map<String, Object> model = new HashMap<>();

        if (request.session().attribute("user_id") == null) {
            response.status(401);
            return "Unauthorised";
        }

        var db = new SqlDatabase();
        var conn = db.getConnection();

        var insert = conn.prepareStatement(
            "insert into message (author_id, text, pub_date, flagged)\n" +
                "            values (?, ?, ?, 0)");

        long unixTime = System.currentTimeMillis() / 1000L;

        insert.setInt(1, request.session().attribute("user_id"));
        insert.setString(2, request.queryParams("text"));
        insert.setLong(3, unixTime);

        insert.execute();

        conn.close();

        addAlert(request.session(), "Your message was recorded");

        response.redirect(URLS.USER, 303);

        PrometheusMetrics metrics = request.attribute("metrics");
        metrics.incrementMessages(METRIC_TYPE_WEB);

        return "";
    };

    public static Route serveFollowPage = (Request request, Response response) -> {
        var db = new SqlDatabase();
        var conn = db.getConnection();

        var insert = conn.prepareStatement("insert into follower (\n" +
                "                who_id, whom_id) values (?, ?)");

        Integer currentUserID = request.session().attribute("user_id");
        if (currentUserID == null) {
            response.status(401);
            return "";
        }

        var whom_id = getUserID(db, request.params(":username"));

        if (whom_id == 0) {
            response.status(404);
            return "";
        }

        insert.setInt(1, currentUserID);
        insert.setInt(2, whom_id);
        insert.execute();

        conn.close();

        response.redirect(URLS.urlFor(URLS.USER_TIMELINE, Map.ofEntries(
                Map.entry(USERNAME, request.params(":username"))
        )));

        addAlert(request.session(), "You are now following " + request.params(":username"));

        PrometheusMetrics metrics = request.attribute("metrics");
        metrics.incrementFollows(METRIC_TYPE_WEB);

        return "";
    };

    public static Route serveUnfollowPage = (Request request, Response response) -> {

        var db = new SqlDatabase();
        var conn = db.getConnection();

        var insert = conn.prepareStatement("delete from follower where who_id=? and whom_id=?");

        var whom_id = getUserID(db, request.params(":username"));

        insert.setInt(1, request.session().attribute("user_id"));
        insert.setInt(2, whom_id);
        insert.execute();

        conn.close();

        addAlert(request.session(), "You are no longer following " + request.params(":username"));

        response.redirect(URLS.urlFor(URLS.USER_TIMELINE, Map.ofEntries(
                Map.entry(USERNAME, request.params(":username"))
        )));

        PrometheusMetrics metrics = request.attribute("metrics");
        metrics.incrementUnfollows(METRIC_TYPE_API);

        return "";
    };

    public static Route serveSimFllws = (Request request, Response response) -> {
        updateLatest(request);

        var db = new SqlDatabase();
        var conn = db.getConnection();

        var who_id = getUserID(new SqlDatabase(), request.params(":username"));

        if (who_id == 0) {
            conn.close();
            response.status(404);
            return "";
        }

        Gson gson = new Gson();
        Fllws fllws = gson.fromJson(request.body(), Fllws.class);


        if (Objects.equals(request.requestMethod(), "POST") && (fllws.follow != null && !fllws.follow.isEmpty())) {

            var whom_id = getUserID(db, fllws.follow);

            if (whom_id == 0) {
                conn.close();
                response.status(404);
                return "";
            }


            var insert = conn.prepareStatement("insert into follower (who_id, whom_id) values (?, ?)");

            insert.setInt(1, who_id);
            insert.setInt(2, whom_id);
            insert.execute();

            conn.close();

            response.status(204);

            PrometheusMetrics metrics = request.attribute("metrics");
            metrics.incrementFollows(METRIC_TYPE_API);

            return "";

        }

        if (Objects.equals(request.requestMethod(), "POST") && (fllws.unfollow != null && !fllws.unfollow.isEmpty())) {

            var whom_id = getUserID(db, fllws.unfollow);

            if (whom_id == 0) {
                conn.close();
                response.status(404);
                return "";
            }

            var insert = conn.prepareStatement("delete from follower where who_id=? and whom_id=?");

            insert.setInt(1, who_id);
            insert.setInt(2, whom_id);
            insert.execute();

            conn.close();

            response.status(204);

            PrometheusMetrics metrics = request.attribute("metrics");
            metrics.incrementUnfollows(METRIC_TYPE_API);

            return "";
        }

        if (Objects.equals(request.requestMethod(), "GET")) {

            var insert = conn.prepareStatement("SELECT \"user\".username FROM \"user\" INNER JOIN follower ON follower.whom_id=user.user_id WHERE follower.who_id=? LIMIT ?");

            var noFollowers = Integer.parseInt(request.queryParamOrDefault("no", "100"));

            insert.setInt(1, who_id);
            insert.setInt(2, noFollowers);

            var rs = insert.executeQuery();


            var follows = new Follows();
            follows.follows = new ArrayList<>();
            while (rs.next()) {
                follows.follows.add(rs.getString(USERNAME));
            }

            conn.close();

            var json = "";

            try {
                json = gson.toJson(follows, Follows.class);
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }

            response.status(200);
            return json;
        }


        conn.close();

        return "";
    };

    private class Fllws {
        @SerializedName("follow")
        private String follow;

        @SerializedName("unfollow")
        private String unfollow;
    }

    private static class Follows {
        @SerializedName("follows")
        private ArrayList<String> follows;
    }





    public static Route servePublicTimelinePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();

        var db = new SqlDatabase();
        var conn = db.getConnection();

        var userID = (Integer) request.session().attribute("user_id");
        var loggedInUser = getUser(conn, (userID));

        if (loggedInUser != null && loggedInUser.next()) {
            model.put("user", loggedInUser.getString(USERNAME));
        }

        // Where does this come from in python?
        model.put("title", "Public Timeline");
        model.put("login", URLS.LOGIN);

        var messages = getMessages();
        model.put("messages", messages);

        conn.close();

        return WebApplication.render(request.session(), model, WebApplication.Templates.PUBLIC_TIMELINE);
    };

    public static Route serveUserTimelinePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        var db = new SqlDatabase();
        var conn = db.getConnection();

        var userID = (Integer) request.session().attribute("user_id");
        var loggedInUser = getUser(conn, (userID));

        if (loggedInUser != null && loggedInUser.next()) {
            model.put("user", loggedInUser.getString(USERNAME));
        }

        else if (loggedInUser == null) {
            response.redirect(URLS.PUBLIC_TIMELINE);
            conn.close();
            return "";
        }

        model.put("endpoint", URLS.USER);
        model.put("title", "My Timeline");

        var statement = conn.prepareStatement("""
                select message.*, \"user\".* from message, 
                \"user\" where message.flagged = 0 and message.author_id = \"user\".user_id 
                and (\"user\".user_id = ? or \"user\".user_id 
                in (select whom_id from follower where who_id = ?)) 
                order by message.pub_date desc limit ?""");

        statement.setInt(1, request.session().attribute("user_id"));
        statement.setInt(2, request.session().attribute("user_id"));
        statement.setInt(3, WebApplication.PER_PAGE);
        ResultSet rs = statement.executeQuery();

        var results = new ArrayList<HashMap<String, Object>>();
        while (rs.next()) {
            HashMap<String, Object> result = new HashMap<>();
            result.put("message_id", rs.getInt("message_id"));
            result.put("author_id", rs.getInt("author_id"));
            result.put("text", rs.getString("text"));
            result.put("pub_date", rs.getString("pub_date")); // Type?
            result.put("flagged", rs.getInt("flagged"));
            result.put(USERNAME, rs.getString(USERNAME));
            result.put("email", rs.getString("email"));
            result.put("pw_hash", rs.getString("pw_hash"));
            results.add(result);
        }
        model.put("messages", results);

        conn.close();

        return WebApplication.render(request.session(), model, WebApplication.Templates.PUBLIC_TIMELINE);
    };

    public static Route serveUserByUsernameTimelinePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();

        var db = new SqlDatabase();
        var conn = db.getConnection();

        var userID = (Integer) request.session().attribute("user_id");
        var loggedInUser = getUser(conn, (userID));

        if (loggedInUser != null && loggedInUser.next()) {
            model.put("user", loggedInUser.getString(USERNAME));
            model.put("user_id", loggedInUser.getInt("user_id"));
        }

        model.put("endpoint", URLS.USER_TIMELINE);

        var profileStmt = conn.prepareStatement(
                "select * from \"user\" where username = ?"
        );
        profileStmt.setString(1, request.params(":username"));
        var profileRs = profileStmt.executeQuery();
        if (profileRs.isClosed() || !profileRs.next()) {
            response.status(404);
            conn.close();
            return "404"; // TODO: What does the old one do?
        }
        var profileUser = new HashMap<String, Object>();
        profileUser.put("user_id", profileRs.getInt("user_id"));
        profileUser.put(USERNAME, profileRs.getString(USERNAME));
        profileUser.put("email", profileRs.getString("email"));
        model.put("profile_user", profileUser);
        profileRs.close();

        model.put("title", profileUser.get(USERNAME) + "'s Timeline");

        if (userID != null) {
            var followedStmt = conn.prepareStatement("select 1 from follower where\n" +
                    "follower.who_id = ? and follower.whom_id = ?");
            followedStmt.setInt(1, userID);
            followedStmt.setInt(2, (Integer) profileUser.get("user_id"));
            var followedRs = followedStmt.executeQuery();
            if (!followedRs.isClosed() && followedRs.next()) {
                model.put("followed", true);
            } else {
                model.put("followed", false);
            }
        } else {
            model.put("followed", false);
        }

        var messageStmt = conn.prepareStatement("""
                select message.*, \"user\".* from message, 
                \"user\" where \"user\".user_id = message.author_id and \"user\".user_id = ? 
                order by message.pub_date desc limit ?""");

        messageStmt.setInt(1, (int) profileUser.get("user_id"));
        messageStmt.setInt(2, PER_PAGE);
        var messages = new ArrayList<HashMap<String, Object>>();
        var messageRs = messageStmt.executeQuery();
        while (messageRs.next()) {
            HashMap<String, Object> result = new HashMap<>();
            result.put("message_id", messageRs.getInt("message_id"));
            result.put("author_id", messageRs.getInt("author_id"));
            result.put("text", messageRs.getString("text"));
            result.put("pub_date", messageRs.getString("pub_date")); // Type?
            result.put("flagged", messageRs.getInt("flagged"));
            result.put(USERNAME, messageRs.getString(USERNAME));
            result.put("email", messageRs.getString("email"));
            messages.add(result);
        }
        model.put("messages", messages);

        conn.close();

        return WebApplication.render(request.session(), model, WebApplication.Templates.PUBLIC_TIMELINE);
    };

    public static Route serveRegisterPage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();

        // TODO: Get logged in user (if any)
        // if (userIsLoggedIn) {
        //     response.redirect(URLS.LOGIN);
        //     return;
        // }

        model.put("messages", new ArrayList<String>() {});
        model.put(USERNAME, request.queryParams(USERNAME) == null ? "" : request.queryParams(USERNAME));
        model.put("email", request.queryParams("email") == null ? "" : request.queryParams("email"));
        model.put("title", "Sign Up");

        if (request.requestMethod().equals("POST")) {
            var username = request.queryParams(USERNAME);
            var email = request.queryParams("email");
            var password = request.queryParams("password");
            var password2 = request.queryParams("password2");

            var error = register(username, email, password, password2);
            if (error != null) {
                model.put("error", error);
            } else {
                addAlert(request.session(), "You were successfully registered and can log in now");

                response.redirect(URLS.LOGIN);

                PrometheusMetrics metrics = request.attribute("metrics");
                metrics.incrementRegistrations(METRIC_TYPE_WEB);

                // No need to render due to redirect
                // Rendering would clear the alerts too early
                return null;
            }
        }

        return WebApplication.render(request.session(), model, WebApplication.Templates.REGISTER);
    };

    public static Route serveLoginPage = (Request request, Response response) -> {
        if (request.session().attribute("user_id") != null) {
            response.redirect(URLS.USER);
        }
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Sign In");

        var enteredUserName = request.queryParams(USERNAME);
        var enteredPW = request.queryParams("password");

        if (request.requestMethod().equals("POST")) {
            var db = new SqlDatabase();
            var connection = db.getConnection();
            var lookup = connection.prepareStatement("select * from \"user\" where\n" +
                    "            username = ?");
            lookup.setString(1, enteredUserName);
            ResultSet rs = lookup.executeQuery();

            if (rs.isClosed() || !rs.next()) {
                model.put("error", "Invalid username");
            }
            else if (!BCrypt.checkpw(enteredPW, rs.getString("pw_hash"))) {
                model.put("error", "Invalid Password");
            }
            else {
                var userID = rs.getInt("user_id");
                request.session().attribute("user_id", userID);

                rs.close();
                connection.close();

                addAlert(request.session(), "You were logged in");

                response.redirect(URLS.USER);

                PrometheusMetrics metrics = request.attribute("metrics");
                metrics.incrementSignins(METRIC_TYPE_WEB);

                // No need to render due to redirect
                // Rendering would clear the alerts too early
                return null;
            }
            rs.close();
            connection.close();
        }
        return WebApplication.render(request.session(), model, Templates.LOGIN);
    };

    public static Route handleLogoutRequest = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        request.session().removeAttribute("user_id");

        addAlert(request.session(), "You were logged out");

        response.redirect(URLS.PUBLIC_TIMELINE);

        PrometheusMetrics metrics = request.attribute("metrics");
        metrics.incrementSignouts(METRIC_TYPE_WEB);

        return null;
    };

    public static Filter protectEndpoint (String expectedAuth)  {
        return (Request request, Response response) -> {
            var auth = request.headers("Authorization");
            if (auth == null || !auth.equals(expectedAuth)) {
                halt(403, gson.toJson(
                        Map.ofEntries(
                                Map.entry("status", 403),
                                Map.entry("error_msg", "You are not authorized to use this resource!")
                        )
                ));
            }
        };
    };

    public static Route serveSimMsgs = (Request request, Response response) -> {
        // Update LATEST static variable
        updateLatest(request);

        var messages = getMessages();
        var filteredMessages = messages.stream().map((message) -> Map.ofEntries(
               Map.entry("content", message.get("text")),
               Map.entry("pub_date", message.get("pub_date")),
               Map.entry("user", message.get(USERNAME))
       ));
        return filteredMessages.toList();
    };

    //  --------------------------------------
    //              Simulator API
    //  ---------------------------------------

    public static Route serveSimMsgsUsr = (Request request, Response response) -> {
        updateLatest(request);

        SqlDatabase db = new SqlDatabase();
        var connection = db.getConnection();
        var userID = getUserID(db, request.params(":username"));

        int noMsgs =  Integer.parseInt(request.queryParamOrDefault("no", "100"));

        if (request.requestMethod().equals("GET")){
            if (userID == 0) {
                connection.close();
                response.status(404);
                return "404 Not Found";
            }
            else {
                var query = connection.prepareStatement("SELECT message.*, \"user\".* FROM message, \"user\" " +
                        "                   WHERE message.flagged = 0 AND" +
                        "                   \"user\".user_id = message.author_id AND \"user\".user_id = ?" +
                        "                   ORDER BY message.pub_date DESC LIMIT ?" );
                query.setInt(1, userID);
                query.setInt(2, noMsgs);
                var messages = query.executeQuery();
                var filteredMessages = new ArrayList<Map>();
                while(messages.next()) {
                    var filteredMessage = new HashMap();
                    filteredMessage.put("content", messages.getString("text"));
                    filteredMessage.put("pub_date", messages.getInt("pub_date"));
                    filteredMessage.put("user", messages.getString(USERNAME));
                    filteredMessages.add(filteredMessage);
                }
                connection.close();
                response.status(200);
                return filteredMessages.stream().toList();
            }
        }
        // Might pose a problem when we POST a message for a user that does not exist (lots of author_id=0 messages) not sure
        else if (request.requestMethod().equals("POST")) {
            var requestData = gson.fromJson(request.body(), HashMap.class);
            var time = System.currentTimeMillis() / 1000L;
            var text = requestData.get("content");

            var query = connection.prepareStatement("INSERT INTO message (author_id, text, pub_date, flagged) VALUES (?, ?, ?, 0)");
            query.setInt(1, userID);
            query.setString(2, text.toString());
            query.setLong(3, time);

            query.execute();
            connection.close();
            response.status(204);

            PrometheusMetrics metrics = request.attribute("metrics");
            metrics.incrementMessages(METRIC_TYPE_API);

            return "";
        }
        connection.close();
        response.status(404);
        return "404 Not Found";
    };

    public static Route serveSimRegister = (Request request, Response response) -> {
        updateLatest(request);

        var json = gson.fromJson(request.body(), HashMap.class);
        var username = String.valueOf(json.get(USERNAME));
        var email = String.valueOf(json.get("email"));
        var password = String.valueOf(json.get("pwd"));

        var error = register(username, email, password, password);
        if (error != null) {
            response.status(400);

            PrometheusMetrics metrics = request.attribute("metrics");
            metrics.incrementRegistrations(METRIC_TYPE_API);

            return gson.toJson(
                    Map.ofEntries(
                            Map.entry("status", 400),
                            Map.entry("error_msg", error)
                    )
            );
        }

        response.status(204);
        return "";
    };

    public static Route serveSimLatest = (Request request, Response response) -> {
        Map<String, Integer> map = new HashMap<>();
        map.put("latest", LATEST);
        return map;
    };

    public static Route serveMetrics = (Request request, Response response) -> {
        return metrics.metrics();
    };

    public static Route serveStats = (Request request, Response response) -> {
        SqlDatabase db = new SqlDatabase();
        var connection = db.getConnection();

        String bucketSizeParam = request.queryParams("bucket_size");
        if (bucketSizeParam == null) {
            bucketSizeParam = "10";
        }
        var bucketSize = Integer.parseInt(bucketSizeParam);

        /*
        SELECT
            CAST(followings/10 AS INT)*10 AS bucket_floor, -- CAST(x AS int) == FLOOR(x)
            COUNT(followings) AS count
        FROM (
            SELECT
                who_id,
                count(whom_id) AS followings
            FROM follower
            GROUP BY who_id
        )
        GROUP BY 1
        ORDER BY 1
         */
        var followersSql =
                "SELECT\n" +
                "   CAST(followings/? AS INT)*? AS bucket_floor, -- CAST(x AS int) == FLOOR(x)\n" +
                "   COUNT(followings) AS count\n" +
                "FROM (\n" +
                "   SELECT\n" +
                "       who_id,\n" +
                "       count(whom_id) AS followings\n" +
                "   FROM follower\n" +
                "   GROUP BY who_id\n" +
                ")\n" +
                "GROUP BY 1\n" +
                "ORDER BY 1";
        var followersQuery = connection.prepareStatement(followersSql);
        followersQuery.setInt(1, bucketSize);
        followersQuery.setInt(2, bucketSize);

        var followersBuckets = followersQuery.executeQuery();

        var followers = new ArrayList<Map>();
        while(followersBuckets.next()) {
            followers.add(Map.ofEntries(
                    Map.entry("bucket",
                            followersBuckets.getInt("bucket_floor")
                                    + "-"
                                    + (followersBuckets.getInt("bucket_floor") + bucketSize - 1)
                    ),
                    Map.entry("n", followersBuckets.getInt(2))
            ));
        }



        var followingSql =
                "SELECT\n" +
                        "   CAST(followings/? AS INT)*? AS bucket_floor, -- CAST(x AS int) == FLOOR(x)\n" +
                        "   COUNT(followings) AS count\n" +
                        "FROM (\n" +
                        "   SELECT\n" +
                        "       whom_id,\n" +
                        "       count(who_id) AS followings\n" +
                        "   FROM follower\n" +
                        "   GROUP BY whom_id\n" +
                        ")\n" +
                        "GROUP BY 1\n" +
                        "ORDER BY 1";
        var followingQuery = connection.prepareStatement(followingSql);
        followingQuery.setInt(1, bucketSize);
        followingQuery.setInt(2, bucketSize);

        var followingBuckets = followingQuery.executeQuery();

        var following = new ArrayList<Map>();
        while(followingBuckets.next()) {
            following.add(Map.ofEntries(
                    Map.entry("bucket",
                            followingBuckets.getInt("bucket_floor")
                                    + "-"
                                    + (followingBuckets.getInt("bucket_floor") + bucketSize - 1)
                    ),
                    Map.entry("n", followingBuckets.getInt(2))
            ));
        }


        connection.close();

        return Map.ofEntries(
                Map.entry("followerStats", followers),
                Map.entry("followingStats", following)
        );
    };
}