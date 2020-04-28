package dao;
import model.Instructor;
import model.Quiz;

import java.io.FileInputStream;

public final class DaoUtil {

    private DaoUtil() {
        // This class is not mean to be instantiated!
    }

    public static void addSampleFiles(Sql2oFileDao fileDao) { }

//    public static void addSampleQuizzes(QuizDao quizDao) {
////        quizDao.add(new Quiz(1, 1, "A", 10, 12, 6, 7));
////        quizDao.add(new Quiz(1, 2, "B", 8, 3, 21, 9));
////        quizDao.add(new Quiz(2, 1, "C", 5, 1, 6, 25));
////        quizDao.add(new Quiz(9999, 1, "D", 11, 20, 2, 3));
//    }

    public static void addSampleUsers(InstructorDao instructorDao) {
        instructorDao.registerUser(new Instructor("Allen", "zchen85@jhu.edu", "9999"));
        instructorDao.registerUser(new Instructor("Bob Wang", "bob@jhu.edu", "8888"));
        instructorDao.registerUser(new Instructor("Richard", "richard@jhu.edu", "7777"));
    }

    public static void addSampleUserFiles(InstructorDao instructorDao) {
//        instructorDao.storeUserFileInfo(1, 1);
//        instructorDao.storeUserFileInfo(1, 2);
//        instructorDao.storeUserFileInfo(2, 1);
    }
}
