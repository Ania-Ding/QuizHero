package api;
import com.google.gson.Gson;
import dao.*;
import exception.*;
import io.javalin.core.util.FileUtil;
import model.*;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;
import io.javalin.http.UploadedFile;
import org.apache.commons.io.FileUtils;

import java.io.*;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ApiServer {

    public static boolean INITIALIZE_WITH_SAMPLE_DATA = true;
//    public static int PORT = 7000;
    public static int PORT = getHerokuAssignedPort();
    private static Javalin app;

    private ApiServer() {
        // This class is not meant to be instantiated!
    }

    private static int getHerokuAssignedPort() {
        String herokuPort = System.getenv("PORT");
        if (herokuPort != null) {
            return Integer.parseInt(herokuPort);
        }
        return 7000;
    }

    public static void start() throws URISyntaxException{
        QuizDao quizDao = DaoFactory.getQuizDao();
        RecordDao recordDao = DaoFactory.getRecordDao();
        InstructorDao instructorDao = DaoFactory.getInstructorDao();
        // add some sample data
        if (INITIALIZE_WITH_SAMPLE_DATA) {
            DaoUtil.addSampleUsers(instructorDao);
            DaoUtil.addSampleQuizzes(quizDao);
            DaoUtil.addSampleUserFiles(instructorDao);
        }

        // Routing
        getHomepage();
        getAllQuizStat(quizDao);
        getQuizStatByFileId(quizDao);
        getSingleQuizStat(quizDao);

        postQuiz(quizDao);
        postRecords(recordDao);

        login(instructorDao);
        register(instructorDao);

        uploadFile(instructorDao);
        fetchFile(instructorDao);

        startJavalin();

        // Handle exceptions
        app.exception(ApiError.class, (exception, ctx) -> {
            ApiError err = (ApiError) exception;
            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("status", err.getStatus());
            jsonMap.put("errorMessage", err.getMessage());
            ctx.status(err.getStatus());
            ctx.json(jsonMap);
        });

//        runs after every request (even if an exception occurred)
//        app.after(ctx -> {
//            // run after all requests
//            ctx.contentType("application/json");
//        });
    }

    public static void stop() {
        app.stop();
    }

    private static void getHomepage() {
        // Catch-all route for the single-page application;
        // The ReactJS application
        app = Javalin.create(config -> {
            config.addStaticFiles("/public");
            config.enableCorsForAllOrigins();
            config.addSinglePageRoot("/", "/public/index.html");
        });

        // app.get("/", ctx -> ctx.result("Welcome to QuizHero!"));
    }

    private static void getAllQuizStat(QuizDao quizDao) {
        // handle HTTP Get request to retrieve all Quiz statistics
        app.get("/quizstat", ctx -> {
            if (ctx.queryParam("fileId") != null) {
                int fileId = Integer.parseInt(ctx.queryParam("fileId"));
                System.out.println("File id: " + fileId);
                List<Quiz> quizzesByFileId = quizDao.getQuizStatByFileId(fileId);
                ctx.json(quizzesByFileId);
            }
            else {
                List<Quiz> quizzes = quizDao.getAllQuizStat();
                ctx.json(quizzes);
            }
            ctx.contentType("application/json");
            ctx.status(200); // everything ok!
        });
    }

    private static void getQuizStatByFileId(QuizDao quizDao) {
        // handle HTTP Get request to retrieve all Quiz statistics of a single file
        app.get("/quizstat/:fileid", ctx -> {
            int fileId = Integer.parseInt(ctx.pathParam("fileid"));
            List<Quiz> quizzes = quizDao.getQuizStatByFileId(fileId);
            ctx.json(quizzes);
            ctx.status(200);
        });
    }

    private static void getSingleQuizStat(QuizDao quizDao) {
        // handle HTTP Get request to retrieve statistics of a single question in a file
        app.get("/quizstat/:fileid/:questionid", ctx -> {
            int fileId = Integer.parseInt(ctx.pathParam("fileid"));
            int questionId = Integer.parseInt(ctx.pathParam("questionid"));
            Quiz quiz = quizDao.getSingleQuizStat(fileId, questionId);
            ctx.json(quiz);
            ctx.status(200);
        });
    }

    // add postQuiz method
    private static void postQuiz(QuizDao quizDao) {
        // quizzes are initialized once a markdown in quiz format is uploaded
        app.post("/quiz", ctx -> {
            Quiz quiz = ctx.bodyAsClass(Quiz.class);
            try {
                quizDao.add(quiz);
                ctx.json(quiz);
                ctx.contentType("application/json");
                ctx.status(201); // created successfully
            } catch (DaoException ex) {
                throw new ApiError(ex.getMessage(), 500);
            }
        });
    }

    private static void postRecords(RecordDao recordDao) {
        // student adds a record of a Quiz question through HTTP POST request
        app.post("/record", ctx -> {
            Record record = ctx.bodyAsClass(Record.class);
            try {
                recordDao.add(record);
                ctx.json(record);
                ctx.contentType("application/json");
                ctx.status(201); // created successfully
            } catch (DaoException ex) {
                throw new ApiError(ex.getMessage(), 404); // quiz not found
            }
        });
    }

    private static void login(InstructorDao instructorDao) {
        // instructor login action, return user including his/her id
        app.post("/login", ctx -> {
            String email = ctx.queryParam("email");
            String pswd = ctx.queryParam("pswd");
            System.out.println("email: " + email + " pswd: " + pswd);
//            Instructor user = ctx.bodyAsClass(Instructor.class);
            try {
                Instructor instructor = instructorDao.checkUserIdentity(email, pswd);
                ctx.json(instructor);
                ctx.contentType("application/json");
                ctx.status(201); // created successfully
            } catch (DaoException ex) {
                throw new ApiError(ex.getMessage(), 500); // server internal error
            } catch (LoginException ex) {
                throw new ApiError(ex.getMessage(), 500); // user not found
            }
        });
    }

    private static void register(InstructorDao instructorDao) {
        // instructor login action, return user including his/her id
        app.post("/register", ctx -> {
            Instructor instructor = ctx.bodyAsClass(Instructor.class);
            try {
                instructorDao.registerUser(instructor);
                ctx.json(instructor);
                ctx.contentType("application/json");
                ctx.status(201); // created successfully
            } catch (DaoException ex) {
                throw new ApiError(ex.getMessage(), 500); // server internal error
            } catch (RegisterException ex) {
                throw new ApiError(ex.getMessage(), 403); // request forbidden, user already exists
            }
        });
    }

    // Upload a file and save it to the local file system
    private static void uploadFile(InstructorDao instructorDao) {
        app.post("/upload", context -> {
            // fetch user id from form-data, if no key then return -1 as default
            int userId = Integer.parseInt(context.formParam("userId", "-1"));
            System.out.println("user id: " + userId);

            UploadedFile uploadedFile = context.uploadedFile("file");
            try (InputStream inputStream = uploadedFile.getContent()) {
                File localFile = new File("upload/" + uploadedFile.getFilename());
                FileUtils.copyInputStreamToFile(inputStream, localFile);
                String url = localFile.getAbsolutePath();
                System.out.println("url: " + url);

                // generate file id
                int fileId = new Random().nextInt(100000);
                // store user-file info in database
                instructorDao.storeUserFileInfo(userId, fileId, url);
                // return fileId to front-end
                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("fileId", fileId);
                fileMap.put("url", url);
                context.json(fileMap);
                context.contentType("application/json");
                context.status(201);
            } catch (NullPointerException npEx) {
                throw new ApiError("file upload error: " + npEx.getMessage(), 400); // client bad request
            } catch (IOException ioEx) {
                throw new ApiError("internal server error: " + ioEx.getMessage(),500); // io exception
            }
        });
    }

    // front-end fetches the specified file
    private static void fetchFile(InstructorDao instructorDao) {
        app.get("/fetch", context -> {
            /* BufferedInputStream是套在某个其他的InputStream外，起着缓存的功能，用来改善里面那个InputStream的性能
            它自己不能脱离里面那个单独存在。FileInputStream是读取一个文件来作InputStream。
            所以可以把BufferedInputStream套在FileInputStream外，来改善FileInputStream的性能。
            */
            String fileUrl = context.queryParam("fileUrl"); // get url of the file from form-data
            System.out.println(fileUrl);
            File localFile = new File(fileUrl); // create file object, passed into FileInputStream(File)
            try {
                InputStream inputStream = new BufferedInputStream(new FileInputStream(localFile));
                System.out.println("find local file.");
                context.header("Content-Disposition", "attachment; filename=\"" + localFile.getName() + "\"");
                context.header("Content-Length", String.valueOf(localFile.length()));
                context.result(inputStream);
                context.status(200);
            } catch (FileNotFoundException ex) {
                throw new ApiError("file not found! " + ex.getMessage(), 400); // bad request
            }
        });
    }

    private static void startJavalin() {
        Gson gson = new Gson();
        JavalinJson.setFromJsonMapper(gson::fromJson);
        JavalinJson.setToJsonMapper(gson::toJson);
        app.start(PORT);
    }
}
