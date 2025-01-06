import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.CountDownLatch;

public class NotSecvential {
    private ArrayList<String> blackList = new ArrayList<>();
    private CustomQueue<Concurent> queue = new CustomQueue<>(Concurent.class, 100);
    private FineGrainLinkedList clasament = new FineGrainLinkedList();
    private ExecutorService executor;
    private int threadCount;
    private int threadProducerCount;

    public NotSecvential(int threadCount, int threadProducerCount){
        this.threadCount = threadCount;
        this.threadProducerCount = threadProducerCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public void rezolvare(){
        long startTime = System.currentTimeMillis();

        File folder = new File("C:\\Users\\adrian.stan\\Desktop\\School\\PPD\\LAB4\\LAB4\\RezultateConcurs");
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            List<List<File>> chunks = splitIntoChunks(listOfFiles, threadProducerCount);
            CountDownLatch latch = new CountDownLatch(chunks.size());

            for (List<File> chunk : chunks) {
                executor.execute(new Producer(chunk, queue, latch));
            }

            for (int i = 0; i < threadCount - threadProducerCount; i++) {
                executor.execute(new Consumer(clasament, queue, blackList));
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < threadCount - threadProducerCount; i++) {
                try {
                    queue.enqueue(null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        executor.shutdown();
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Time taken: " + duration + " milliseconds");
    }

    private static List<List<File>> splitIntoChunks(File[] files, int n) {
        List<List<File>> chunks = new ArrayList<>();
        int baseChunkSize = files.length / n;
        int remainder = files.length % n;

        int start = 0;
        for (int i = 0; i < n; i++) {
            int chunkSize = baseChunkSize + (i < remainder ? 1 : 0);
            List<File> chunk = new ArrayList<>();
            for (int j = 0; j < chunkSize; j++) {
                chunk.add(files[start + j]);
            }
            chunks.add(chunk);
            start += chunkSize;
        }

        return chunks;
    }
}