import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final CustomQueue<Concurent> queue;
    private final FineGrainLinkedList clasament;
    private final ArrayList<String> blackList;
    private final int threadCount;
    private static final HashMap<String, CompletableFuture<List<CountryScore>>> countryRankingsCache = new HashMap<>();
    private static final long CACHE_EXPIRATION_TIME = 50000;
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

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("ADD_CONTESTANT")) {
                    util.log("ADD_CONTESTANT: " + line);
                    handleAddContestant(line, out);
                } else if (line.startsWith("REQUEST_PARTIAL_RANKING")) {
                    util.log("REQUEST_PARTIAL_RANKING");
                    handlePartialRanking(out);
                } else if (line.startsWith("REQUEST_FINAL_RANKING")) {
                    handleFinalRanking(out);
//                } else if (line.startsWith("RECEIVE_FILES")) {
//                    sendFinalFilesToClient();
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

        if (parts.length < 4 || (parts.length - 1) % 3 != 0) {
            out.println("INVALID_FORMAT");
            return;
        }

        util.log("Processing contestants from line...");

        int contestantCount = (parts.length - 1) / 3; // Exclude the command part
        util.log("Received " + contestantCount + " contestants.");

        for (int i = 1; i < parts.length; i += 3) {
            String countryId = parts[i];
            String contestantId = parts[i + 1];

            int score;
            try {
                score = Integer.parseInt(parts[i + 2]);
            } catch (NumberFormatException e) {
                util.log("Invalid score format for contestant: " + parts[i + 2]);
                out.println("INVALID_SCORE");
                continue;
            }

            if (!blackList.contains(countryId)) {
                Concurent concurent = new Concurent(contestantId, score, countryId);
                queue.enqueue(concurent);
            } else {
                out.println("COUNTRY_BLACKLISTED");
            }
        }

        out.println("CHUNK_ADDED");
    }


    private void handlePartialRanking(PrintWriter out) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastRankingCalculationTime > CACHE_EXPIRATION_TIME) {
            CompletableFuture<List<CountryScore>> rankingFuture = CompletableFuture.supplyAsync(this::calculateCountryRanking);
            countryRankingsCache.put("partial", rankingFuture);
            lastRankingCalculationTime = currentTime;
        }

        CompletableFuture<List<CountryScore>> rankingFuture = countryRankingsCache.get("partial");

        rankingFuture.whenComplete((ranking, ex) -> {
            if (ex != null) {
                ex.printStackTrace();
                out.println("RANKING_ERROR");
            } else {
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
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter("clasament_tari.txt"))) {
                        for (CountryScore score : ranking) {
                            writer.write(score.toString());
                            writer.newLine();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    getFinalRanking();
                    sendFinalFilesToClient();
                }
            });
        } else {
            out.println("NO_RANKING_AVAILABLE");
        }
    }

    private void getFinalRanking() {
        //TODO: clasamentul final cu toti concurentii si toate tarile(clasament.txt?)
    }

    private void sendFinalFilesToClient() {
        String zipFileName = "final_files.zip";

        try {
            // Create and zip the files
            zipFiles(zipFileName, "clasament_tari.txt", "clasament.txt");//TODO: de adaugat si clasamentul final: clasament.txt?

            // Send the zip file
            sendFile(zipFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void zipFiles(String zipFileName, String... fileNames) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFileName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (String fileName : fileNames) {
                File file = new File(fileName);
                if (!file.exists()) {
                    System.out.println("File " + fileName + " not found, skipping.");
                    continue;
                }
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry entry = new ZipEntry(file.getName());
                    zos.putNextEntry(entry);

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                    zos.closeEntry();
                }
            }
            System.out.println("Created zip file: " + zipFileName);
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