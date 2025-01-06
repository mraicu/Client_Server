package util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import entity.Participant;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private static final double DELTA_X = 1;

    @Getter
    private final ExecutorService executor;

    private static final int PORT = 12348;
    private final List<Participant> concurenti;

    public Client(List<Participant> concurenti) {
        this.executor = Executors.newFixedThreadPool(Constants.NUMBER_OF_THREADS);
        this.concurenti = concurenti;
    }

    public void startSending() {
        // create a CountDownLatch to synchronize the threads
        CountDownLatch latch = new CountDownLatch(Constants.NUMBER_OF_THREADS);
        for (int i = 0; i < Constants.NUMBER_OF_THREADS; ++i) {
            int finalI = i;
            int size = concurenti.size() / Constants.NUMBER_OF_THREADS;
            executor.submit(() -> {
                int start = finalI * size;
                int end = start + size;
                for (int j = start; j < end; j += Constants.CHUNK_SIZE) {
                    sendChunk(concurenti.subList(j, Math.min(j + Constants.CHUNK_SIZE, end)));
                    System.out.println("Sent " + j + " to " + Math.min(j + Constants.CHUNK_SIZE, end));
                    try {
                        Thread.sleep((long) (DELTA_X * 1000));
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted while sleeping!");
                    }
                }
                getClasament();
//                getClasamentFinal();
                latch.countDown();
            });
        }
        try {
            latch.await();
            sendTerminationSignal();
        } catch (InterruptedException e) {
            System.out.println("Interrupted while waiting for threads to finish!");
        }
        executor.shutdown();
    }

    private void sendTerminationSignal() {
        try (Socket socket = new Socket("localhost", PORT);
             OutputStream output = socket.getOutputStream()) {
            output.write("END\n".getBytes()); // Send termination signal
            output.flush();
            System.out.println("Termination signal sent to server.");
        } catch (IOException e) {
            System.out.println("Error sending termination signal: " + e.getMessage());
        }
    }


    private void sendChunk(List<Participant> chunk) {
        try (Socket socket = new Socket("localhost", PORT);
             OutputStream output = socket.getOutputStream();
             InputStream input = socket.getInputStream()) {

            StringBuilder command = new StringBuilder("ADD_CONTESTANT");
            for (Participant participant : chunk) {
                command.append(",").append(participant.getCountryId())
                        .append(",").append(participant.getId())
                        .append(",").append(participant.getPunctaj());
            }

            output.write((command+"\n").getBytes());
            output.flush();

            byte[] responseBuffer = new byte[1024];
            int bytesRead = input.read(responseBuffer);
            String response = new String(responseBuffer, 0, bytesRead);

            System.out.println("Raspuns de la server: " + response);
        } catch (IOException e) {
            System.out.println("Error sending chunk: " + e.getMessage());
        }
    }


    public void sendChunkSize(int size) {
        try (Socket socket = new Socket("localhost", PORT);
             OutputStream output = socket.getOutputStream();
             InputStream input = socket.getInputStream()) {

            String command = "SEND_CHUNK_SIZE";
            String data = command + ":" + (size / 20);

            // send the command and data
            output.write(data.getBytes());
            output.flush();

            // server s response
            byte[] responseBuffer = new byte[1024];
            int bytesRead = input.read(responseBuffer);
            String response = new String(responseBuffer, 0, bytesRead);

            System.out.println("Raspuns de la server: " + response);

        } catch (IOException e) {
            System.out.println("Error sending chunk size: " + e.getMessage());
        }
    }


    public void getClasament() {
        try (Socket socket = new Socket("localhost", PORT);
             OutputStream output = socket.getOutputStream();
             InputStream input = socket.getInputStream()) {

            String command = "REQUEST_PARTIAL_RANKING";
            output.write((command+"\n").getBytes());
            output.flush();

            byte[] responseBuffer = new byte[4096];
            int bytesRead = input.read(responseBuffer);
            String responseJson = new String(responseBuffer, 0, bytesRead);

            ObjectMapper objectMapper = new ObjectMapper();
            List<CountryScore> ranking = objectMapper.readValue(responseJson, new TypeReference<>() {});

            ranking.forEach(score -> System.out.println(score.getCountryId() + " " + score.getScore() +
                    ", de la threadul cu id = " + Thread.currentThread().getId()));

        } catch (IOException e) {
            System.out.println("Error getting clasament: " + e.getMessage());
        }
    }

    public void getClasamentFinal() {
        try (Socket socket = new Socket("localhost", PORT);
             OutputStream output = socket.getOutputStream();
             InputStream input = socket.getInputStream()) {

            String command = "REQUEST_FINAL_RANKING";
            output.write(command.getBytes());
            output.flush();

            byte[] fileBuffer = input.readAllBytes();
            saveToFile("clasamentFinal" + Thread.currentThread().getId() + ".txt", fileBuffer);

        } catch (IOException e) {
            System.out.println("Error getting clasament final: " + e.getMessage());
        }
    }


    private void saveToFile(String fileName, byte[] fileContent) {
        try {
            Path filePath = Paths.get(fileName);
            Files.write(filePath, fileContent);
            System.out.println("Fisier " + fileName + " salvat local.");
        } catch (IOException e) {
            System.out.println("Error saving file " + fileName + ": " + e.getMessage());
        }
    }
}
