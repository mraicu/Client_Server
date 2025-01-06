import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final CustomQueue<Concurent> queue;
    private final FineGrainLinkedList clasament;
    private final ArrayList<String> blackList;
    private final int threadCount;
    private static final HashMap<String, CompletableFuture<List<CountryScore>>> countryRankingsCache = new HashMap<>();
    private static final long CACHE_EXPIRATION_TIME = 5000;
    private static long lastRankingCalculationTime = 0;
    Util util = new Util();


    public ClientHandler(Socket clientSocket, CustomQueue<Concurent> queue, FineGrainLinkedList clasament, ArrayList<String> blackList, int threadCount) {
        this.clientSocket = clientSocket;
        this.queue = queue;
        this.clasament = clasament;
        this.blackList = blackList;
        this.threadCount = threadCount;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line = in.readLine();
            util.log("Socket input from client: " + line);
            while (line != null) {
                if (line.equals("END")) {
                    util.log("Received termination signal.");
                    break;
                }
                if (line.startsWith( "ADD_CONTESTANT")) {
                    util.log("ADD_CONTESTANT: " + line);
                    handleAddContestant(line, out);
                } else if (line.startsWith("REQUEST_PARTIAL_RANKING")) {
                    util.log("REQUEST_PARTIAL_RANKING");
                    handlePartialRanking(out);
                } else if (line.startsWith("REQUEST_FINAL_RANKING")) {

                    handleFinalRanking(out);
                } else if (line.startsWith("RECEIVE_FILES")) {
                    sendFinalFilesToClient();
                } else {
                    out.println("INVALID_REQUEST");
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
                System.out.println("Socket CLOSE!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleAddContestant(String line, PrintWriter out) throws InterruptedException {
        String[] parts = line.split(",");
        if (parts.length == 61) { // 1 for the command and 3 for each of the 20 contestants
            util.log("Received 20 contestants from country " + parts[3]);
            for (int i = 1; i < parts.length; i += 3) {
                String countryId = parts[i];
                String contestantId = parts[i + 1];
                int score = Integer.parseInt(parts[i + 2]);

                if (!blackList.contains(countryId)) {
                    Concurent concurent = new Concurent(contestantId, score, countryId);
                    queue.enqueue(concurent);
                } else {
                    out.println("COUNTRY_BLACKLISTED");
                }
            }
            out.println("CHUNK_ADDED");
        } else {
            out.println("INVALID_FORMAT");
        }
    }

    private void handlePartialRanking(PrintWriter out) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastRankingCalculationTime > CACHE_EXPIRATION_TIME) {
            CompletableFuture<List<CountryScore>> rankingFuture = CompletableFuture.supplyAsync(this::calculateCountryRanking);
            countryRankingsCache.put("partial", rankingFuture);
            lastRankingCalculationTime = currentTime;
        }

        CompletableFuture<List<CountryScore>> rankingFuture = countryRankingsCache.get("partial");

        // Send the result after the future completes
        rankingFuture.whenComplete((ranking, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                out.println("RANKING_ERROR");
            } else {
                out.println("PARTIAL_RANKING");
                // Send each ranking entry to the client
                for (CountryScore score : ranking) {
                    out.println(score);
                }
            }
        });
    }

    private List<CountryScore> calculateCountryRanking() {
        Map<String, Integer> countryScores = new HashMap<>();
        Node current = clasament.head.next;

        while (current != clasament.tail) {
            Concurent concurent = current.concurent;
            countryScores.merge(concurent.countryId, concurent.punctaj, Integer::sum);
            current = current.next;
        }

        List<CountryScore> ranking = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countryScores.entrySet()) {
            ranking.add(new CountryScore(entry.getValue(), entry.getKey()));
        }

        ranking.sort(Comparator.comparingInt(CountryScore::getScore).reversed().thenComparing(CountryScore::getCountryId));
        return ranking;
    }

    private void handleFinalRanking(PrintWriter out) {
        for (int i = 0; i < threadCount; i++) {
            try {
                queue.enqueue(null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        CompletableFuture<List<CountryScore>> rankingFuture = countryRankingsCache.get("partial");
        if (rankingFuture != null) {
            rankingFuture.whenComplete((ranking, ex) -> {
                if (ex != null) {
                    ex.printStackTrace();
                    out.println("RANKING_ERROR");
                    util.log("Error while calculating final ranking");
                } else {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\mraic\\OneDrive\\Desktop\\Scoala\\CS\\5th_sem\\Parallel_and_Distributed_Programming\\P1\\Client-Server\\Server\\src\\main\\java\\Clasament.txt"))) {
                        for (CountryScore score : ranking) {
                            writer.write(score.toString());
                            writer.newLine();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    sendFinalFilesToClient();
                }
            });
        } else {
            out.println("NO_RANKING_AVAILABLE");
        }
    }

    private void sendFinalFilesToClient() {
        try {
            sendFile("Clasament.txt");
            sendFile("clasament-tari.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(String filename) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));
             OutputStream os = clientSocket.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }

}