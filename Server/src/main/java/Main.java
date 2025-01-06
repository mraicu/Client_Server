import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {

    public static boolean compareFiles(String filePath1, String filePath2) {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(filePath1));
             BufferedReader reader2 = new BufferedReader(new FileReader(filePath2))) {

            String line1 = reader1.readLine();
            String line2 = reader2.readLine();

            while (line1 != null || line2 != null) {
                if (line1 == null || line2 == null) {
                    return false;
                }
                if (!line1.equals(line2)) {
                    return false;
                }
                line1 = reader1.readLine();
                line2 = reader2.readLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        Util util = new Util();
        int THREAD_POOL_SIZE_R = 10;
        int THREAD_POOL_SIZE_W = 10;

        util.log("-----------------------------------------------------");
        Server server = new Server(THREAD_POOL_SIZE_R, THREAD_POOL_SIZE_W);
        server.start();
    }

}