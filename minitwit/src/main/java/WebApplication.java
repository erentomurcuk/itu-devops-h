import com.google.gson.GsonBuilder;
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

    // The ID of the latest request made by the simulator
    public static int LATEST = 0;

    private static final Gravatar gravatar = new Gravatar()
            .setSize(48)
            .setRating(GravatarRating.GENERAL_AUDIENCES)
            .setDefaultImage(GravatarDefaultImage.IDENTICON);

    public static void main(String[] args) {
        System.out.println("Hello Minitwit");

        var port = System.getenv("MINITWIT_PORT");
        if (port == null) {
            port = "8080";
        }
        port(Integer.parseInt(port));

        staticFiles.location("/static");

        before("/*", (req, res) -> {
            // Setup initial session state once
            if (req.session().isNew()) {
                req.session().attribute("alerts", new ArrayList<>());
            }
        });

        after("/*", (req, res) -> {
            // TODO: currently doesn't log query parameters or unusual headers
            System.out.println(LocalDateTime.now() + " - " + req.uri() + " - " + res.status());
        });

        get(URLS.PUBLIC_TIMELINE, WebApplication.servePublicTimelinePage);
        get(URLS.REGISTER, WebApplication.serveRegisterPage);
        get(URLS.LOGIN, WebApplication.serveLoginPage);
        get(URLS.LOGOUT, WebApplication.handleLogoutRequest);
        get(URLS.USER, WebApplication.serveUserTimelinePage);
        get(URLS.USER_TIMELINE, WebApplication.serveUserByUsernameTimelinePage);

        post(URLS.REGISTER, WebApplication.serveRegisterPage);
        post(URLS.ADD_MESSAGE, WebApplication.add_message);
        get(URLS.FOLLOW, WebApplication.serveFollowPage);
        get(URLS.UNFOLLOW, WebApplication.serveUnfollowPage);
        post(URLS.LOGIN, WebApplication.serveLoginPage);


        // Sim API
        path("/api", () -> {
            // All endpoints in this path must be authenticated
            before("/*", protectEndpoint);

            get(URLS.SIM_MESSAGES, WebApplication.serveSimMsgs, gson::toJson);
            post(URLS.SIM_REGISTER, WebApplication.serveSimRegister); // Handles JSON on its own
            get(URLS.SIM_LATEST, WebApplication.serveSimLatest, gson::toJson);
            get(URLS.SIM_MSGS_USR, WebApplication.serveSimMsgsUsr, gson::toJson);
            post(URLS.SIM_MSGS_USR, WebApplication.serveSimMsgsUsr, gson::toJson);
            post(URLS.SIM_FLLWS, WebApplication.serveSimFllws);
            get(URLS.SIM_FLLWS, WebApplication.serveSimFllws);
        });

    }

    public static ResultSet getUser(Connection conn, Integer userId) throws SQLException {
        if (userId == null) return null;

        var statement = conn.prepareStatement("select * from user where user_id = ?");

        statement.setInt(1, userId);
        ResultSet rs = statement.executeQuery();

        return rs;
    }

    public static int getUserID(SQLite db, String username) throws SQLException {
        var conn = db.getConnection();
        var statement = conn.prepareStatement("select user_id from user where username = ?");

        statement.setString(1, username);
        ResultSet rs = statement.executeQuery();

        // No user with that username found
        if (rs.isClosed()) {
            conn.close();
            return 0;
        }

        var userID = rs.getInt("user_id");
        conn.close();
        return userID;
    }

    public static ArrayList<HashMap<String, Object>> getMessages() throws SQLException {
        var db = new SQLite();
        var conn = db.getConnection();

        var messageStmt = conn.prepareStatement(
                "select message.*, user.* from message, user\n" +
                        "        where message.flagged = 0 and message.author_id = user.user_id\n" +
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
            result.put("username", messageRs.getString("username"));
            result.put("email", messageRs.getString("email"));
            messages.add(result);
        }

        conn.close();

        return messages;
    }

    public static String render(Session session, Map<String, Object> model, String templatePath) {
        try {
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
            return writer.toString();
        } catch (Exception e) {
            // If a error happens above, print it and return it as a response
            e.printStackTrace();
            return e.toString();
        } finally {
            ((List<String>) session.attribute("alerts")).clear();
        }
    }

    private static void addAlert(Session session, String message) {
        ((List<String>) session.attribute("alerts")).add(message);
    }

    // Used by timeline.vm to display user's gravatar
    public static String getGravatarURL(String email) {
        return gravatar.getUrl(email);
    }

    public static String register(String username, String email, String password, String password2) throws SQLException {
        var db = new SQLite();
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

            var conn = new SQLite().getConnection();
            var insert = conn.prepareStatement("insert into user (\n" +
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

        try {
            if (request.session().attribute("user_id") == null) {
                response.status(401);
                return "Unauthorised";
            }

            var db = new SQLite();
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
        }

        catch (Exception e) {
            e.printStackTrace();
            return e.toString();

        }

        addAlert(request.session(), "Your message was recorded");

        response.redirect(URLS.USER, 303);

        return "";
    };

    public static Route serveFollowPage = (Request request, Response response) -> {
        var db = new SQLite();
        var conn = db.getConnection();
        try {

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
                    Map.entry("username", request.params(":username"))
            )));

            addAlert(request.session(), "You are now following " + request.params(":username"));

            return "";
        } catch (Exception e) {
            conn.close();
            e.printStackTrace();
            return e.toString();
        }
    };

    public static Route serveUnfollowPage = (Request request, Response response) -> {

        var db = new SQLite();
        var conn = db.getConnection();

        var insert = conn.prepareStatement("delete from follower where who_id=? and whom_id=?");

        var whom_id = getUserID(db, request.params(":username"));

        insert.setInt(1, request.session().attribute("user_id"));
        insert.setInt(2, whom_id);
        insert.execute();

        conn.close();

        addAlert(request.session(), "You are no longer following " + request.params(":username"));

        response.redirect(URLS.urlFor(URLS.USER_TIMELINE, Map.ofEntries(
                Map.entry("username", request.params(":username"))
        )));

        return "";
    };

    public static Route serveSimFllws = (Request request, Response response) -> {
        updateLatest(request);

        var db = new SQLite();
        var conn = db.getConnection();

        var who_id = getUserID(new SQLite(), request.params(":username"));

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
            return "";
        }

        if (Objects.equals(request.requestMethod(), "GET")) {

            var insert = conn.prepareStatement("SELECT user.username FROM user INNER JOIN follower ON follower.whom_id=user.user_id WHERE follower.who_id=? LIMIT ?");

            var noFollowers = Integer.parseInt(request.queryParamOrDefault("no", "100"));

            insert.setInt(1, who_id);
            insert.setInt(2, noFollowers);

            var rs = insert.executeQuery();


            var follows = new Follows();
            follows.follows = new ArrayList<>();
            while (rs.next()) {
                follows.follows.add(rs.getString("username"));
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
        try {
            Map<String, Object> model = new HashMap<>();

            var db = new SQLite();
            var conn = db.getConnection();

            var userID = (Integer) request.session().attribute("user_id");
            var loggedInUser = getUser(conn, (userID));
            if (loggedInUser != null) model.put("user", loggedInUser.getString("username"));

            // Where does this come from in python?
            model.put("title", "Public Timeline");
            model.put("login", URLS.LOGIN);

            var messages = getMessages();
            model.put("messages", messages);

            conn.close();

            return WebApplication.render(request.session(), model, WebApplication.Templates.PUBLIC_TIMELINE);
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    };

    public static Route serveUserTimelinePage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        var db = new SQLite();
        var conn = db.getConnection();

        var userID = (Integer) request.session().attribute("user_id");
        var loggedInUser = getUser(conn, (userID));
        if (loggedInUser != null) model.put("user", loggedInUser.getString("username"));

        else if (loggedInUser == null) {
            response.redirect(URLS.PUBLIC_TIMELINE);
            conn.close();
            return "";
        }

        model.put("endpoint", URLS.USER);
        model.put("title", "My Timeline");

        var statement = conn.prepareStatement(
                "        select message.*, user.* from message, user\n" +
                        "        where message.flagged = 0 and message.author_id = user.user_id and (\n" +
                        "            user.user_id = ? or\n" +
                        "            user.user_id in (select whom_id from follower\n" +
                        "                                    where who_id = ?))\n" +
                        "        order by message.pub_date desc limit ?");

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
            result.put("username", rs.getString("username"));
            result.put("email", rs.getString("email"));
            result.put("pw_hash", rs.getString("pw_hash"));
            results.add(result);
        }
        model.put("messages", results);

        conn.close();

        return WebApplication.render(request.session(), model, WebApplication.Templates.PUBLIC_TIMELINE);
    };

    public static Route serveUserByUsernameTimelinePage = (Request request, Response response) -> {
        try {
            Map<String, Object> model = new HashMap<>();

            var db = new SQLite();
            var conn = db.getConnection();

            var userID = (Integer) request.session().attribute("user_id");
            var loggedInUser = getUser(conn, (userID));
            if (loggedInUser != null) {
                model.put("user", loggedInUser.getString("username"));
                model.put("user_id", loggedInUser.getInt("user_id"));
            }

            model.put("endpoint", URLS.USER_TIMELINE);

            var profileStmt = conn.prepareStatement(
                    "select * from user where username = ?"
            );
            profileStmt.setString(1, request.params(":username"));
            var profileRs = profileStmt.executeQuery();
            if (profileRs.isClosed()) {
                response.status(404);
                conn.close();
                return "404"; // TODO: What does the old one do?
            }
            var profileUser = new HashMap<String, Object>();
            profileUser.put("user_id", profileRs.getInt("user_id"));
            profileUser.put("username", profileRs.getString("username"));
            profileUser.put("email", profileRs.getString("email"));
            model.put("profile_user", profileUser);
            profileRs.close();

            model.put("title", profileUser.get("username") + "'s Timeline");

            if (userID != null) {
                var followedStmt = conn.prepareStatement("select 1 from follower where\n" +
                        "follower.who_id = ? and follower.whom_id = ?");
                followedStmt.setInt(1, userID);
                followedStmt.setInt(2, (Integer) profileUser.get("user_id"));
                var followedRs = followedStmt.executeQuery();
                if (!followedRs.isClosed()) {
                    model.put("followed", true);
                } else {
                    model.put("followed", false);
                }
            } else {
                model.put("followed", false);
            }

            var messageStmt = conn.prepareStatement(
                    "select message.*, user.* from message, user where\n" +
                            "            user.user_id = message.author_id and user.user_id = ?\n" +
                            "            order by message.pub_date desc limit ?");
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
                result.put("username", messageRs.getString("username"));
                result.put("email", messageRs.getString("email"));
                messages.add(result);
            }
            model.put("messages", messages);

            conn.close();

            return WebApplication.render(request.session(), model, WebApplication.Templates.PUBLIC_TIMELINE);
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    };

    public static Route serveRegisterPage = (Request request, Response response) -> {
        Map<String, Object> model = new HashMap<>();
        try {

            var db = new SQLite();
            var conn = db.getConnection();
            var insert = conn.prepareStatement("insert into user (\n" +
                    "username, email, pw_hash) values (?, ?, ?)");

            // TODO: Get logged in user (if any)
            // if (userIsLoggedIn) {
            //     response.redirect(URLS.LOGIN);
            //     return;
            // }

            model.put("messages", new ArrayList<String>() {});
            model.put("username", request.queryParams("username") == null ? "" : request.queryParams("username"));
            model.put("email", request.queryParams("email") == null ? "" : request.queryParams("email"));
            model.put("title", "Sign Up");

            if (request.requestMethod().equals("POST")) {
                var username = request.queryParams("username");
                var email = request.queryParams("email");
                var password = request.queryParams("password");
                var password2 = request.queryParams("password2");

                var error = register(username, email, password, password2);
                if (error != null) {
                    model.put("error", error);
                } else {
                    addAlert(request.session(), "You were successfully registered and can log in now");

                    response.redirect(URLS.LOGIN);

                    // No need to render due to redirect
                    // Rendering would clear the alerts too early
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }

        return WebApplication.render(request.session(), model, WebApplication.Templates.REGISTER);
    };

    public static Route serveLoginPage = (Request request, Response response) -> {
        if (request.session().attribute("user_id") != null) {
            response.redirect(URLS.USER);
        }
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Sign In");

        var enteredUserName = request.queryParams("username");
        var enteredPW = request.queryParams("password");

        if (request.requestMethod().equals("POST")) {
            var db = new SQLite();
            var connection = db.getConnection();
            var lookup = connection.prepareStatement("select * from user where\n" +
                    "            username = ?");
            lookup.setString(1, enteredUserName);
            ResultSet rs = lookup.executeQuery();

            if (rs.isClosed()) {
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
        return null;
    };

    public static Filter protectEndpoint = (Request request, Response response) -> {
        var auth = request.headers("Authorization");
        // Auth code is simulator:super_safe!
        if (auth == null || !auth.equals("Basic c2ltdWxhdG9yOnN1cGVyX3NhZmUh")) {
            halt(403, gson.toJson(
                    Map.ofEntries(
                            Map.entry("status", 403),
                            Map.entry("error_msg", "You are not authorized to use this resource!")
                    )
            ));
        }
    };

    public static Route serveSimMsgs = (Request request, Response response) -> {
        // Update LATEST static variable
        updateLatest(request);

        var messages = getMessages();
        var filteredMessages = messages.stream().map((message) -> Map.ofEntries(
               Map.entry("content", message.get("text")),
               Map.entry("pub_date", message.get("pub_date")),
               Map.entry("user", message.get("username"))
       ));
        return filteredMessages.toList();
    };

    //  --------------------------------------
    //              Simulator API
    //  ---------------------------------------

    public static Route serveSimMsgsUsr = (Request request, Response response) -> {
        updateLatest(request);

        SQLite db = new SQLite();
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
                var query = connection.prepareStatement("SELECT message.*, user.* FROM message, user " +
                        "                   WHERE message.flagged = 0 AND" +
                        "                   user.user_id = message.author_id AND user.user_id = ?" +
                        "                   ORDER BY message.pub_date DESC LIMIT ?" );
                query.setInt(1, userID);
                query.setInt(2, noMsgs);
                var messages = query.executeQuery();
                var filteredMessages = new ArrayList<Map>();
                while(messages.next()) {
                    var filteredMessage = new HashMap();
                    filteredMessage.put("content", messages.getString("text"));
                    filteredMessage.put("pub_date", messages.getInt("pub_date"));
                    filteredMessage.put("user", messages.getString("username"));
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
            return "";
        }
        connection.close();
        response.status(404);
        return "404 Not Found";
    };

    public static Route serveSimRegister = (Request request, Response response) -> {
        try {
            updateLatest(request);

            var json = gson.fromJson(request.body(), HashMap.class);
            var username = String.valueOf(json.get("username"));
            var email = String.valueOf(json.get("email"));
            var password = String.valueOf(json.get("pwd"));

            var error = register(username, email, password, password);
            if (error != null) {
                response.status(400);
                return gson.toJson(
                        Map.ofEntries(
                                Map.entry("status", 400),
                                Map.entry("error_msg", error)
                        )
                );
            }

            response.status(204);
            return "";
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }
    };

    public static Route serveSimLatest = (Request request, Response response) -> {
        Map<String, Integer> map = new HashMap<>();
        map.put("latest", LATEST);
        return map;
    };
}