import entity.Participant;
import util.Client;
import util.IOHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main_5c {
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        double start = System.currentTimeMillis();

        for (int i = 0; i < 5; i++) {
            int clientId = i + 1;
            List<String> files = IOHandler.generateFileNames(clientId);
            List<Participant> concurenti = IOHandler.readParticipants(files);
            executor.submit(() -> {
                try {
                    System.out.println("Client " + clientId + " starting...");
                    Client client = new Client(new ArrayList<>(concurenti)); // Pass a copy of participants for each client
                    client.startSending();
                    System.out.println("Client " + clientId + " finished.");
                } catch (Exception e) {
                    System.err.println("Error in Client " + clientId + ": " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(100);
        }

        double end = System.currentTimeMillis();

        System.out.println("All clients completed in " + (end - start) + " ms");
    }
}
