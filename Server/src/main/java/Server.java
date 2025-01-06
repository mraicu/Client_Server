import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12348;
    private final ExecutorService clientHandlerPool;
    private final ExecutorService contestantAdderPool;
    private final CustomQueue<Concurent> queue;
    private final FineGrainLinkedList clasament;
    private final ArrayList<String> blackList;
    private final int contestantAdderThreadPoolSize;
    private final int clientHandlerThreadPoolSize;

    Util util = new Util();

    public Server(int clientHandlerThreadPoolSize, int contestantAdderThreadPoolSize) {
        this.clientHandlerPool = Executors.newFixedThreadPool(clientHandlerThreadPoolSize);
        this.clientHandlerThreadPoolSize = clientHandlerThreadPoolSize;
        this.contestantAdderPool = Executors.newFixedThreadPool(contestantAdderThreadPoolSize);
        this.contestantAdderThreadPoolSize = contestantAdderThreadPoolSize;
        this.queue = new CustomQueue<>(Concurent.class, 1000);
        this.clasament = new FineGrainLinkedList();
        this.blackList = new ArrayList<>();
    }

    public void start() {
        for (int i = 0; i < contestantAdderThreadPoolSize; i++) {
            contestantAdderPool.execute(new Consumer(clasament, queue, blackList));
        }
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {

                Socket clientSocket = serverSocket.accept();
                util.log("The socket is connected: " + clientSocket.isConnected());
                clientHandlerPool.execute(new ClientHandler(clientSocket, queue, clasament, blackList, clientHandlerThreadPoolSize));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            clientHandlerPool.shutdown();
            contestantAdderPool.shutdown();
        }
    }


}