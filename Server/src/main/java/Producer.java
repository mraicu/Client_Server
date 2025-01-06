import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Producer implements Runnable {
    private final List<File> files;
    private final CustomQueue<Concurent> queue;
    private final CountDownLatch latch;

    public Producer(List<File> files, CustomQueue<Concurent> queue, CountDownLatch latch) {
        this.files = files;
        this.queue = queue;
        this.latch = latch;
    }

    @Override
    public void run() {
        try {
            for (File file : files) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split(",");
                        Concurent concurent = new Concurent(parts[0], Integer.parseInt(parts[1]), "");
                        queue.enqueue(concurent);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            latch.countDown();
        }
    }
}