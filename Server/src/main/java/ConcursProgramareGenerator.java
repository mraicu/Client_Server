import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class ConcursProgramareGenerator {
    public static void main(String[] args) {
        // Setările inițiale
        String[] tari = {"Tara1", "Tara2", "Tara3", "Tara4", "Tara5"};
        int numarProbleme = 10;
        int minConcurenti = 80;
        int maxConcurenti = 100;
        String outputDir = "RezultateConcurs";

        // Creează directorul pentru rezultate
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdir();
        }

        Random random = new Random();

        // Generarea datelor pentru fiecare țară
        for (String tara : tari) {
            // Număr aleatoriu de concurenți
            int numarConcurenti = random.nextInt(maxConcurenti - minConcurenti + 1) + minConcurenti;

            // Generăm ID-uri pentru concurenți
            String[] concurenti = new String[numarConcurenti];
            for (int i = 0; i < numarConcurenti; i++) {
                concurenti[i] = "ID_" + tara + "_" + (i + 1);
            }

            // Generăm fișiere pentru probleme
            for (int problema = 1; problema <= numarProbleme; problema++) {
                String fileName = outputDir + "/Rezultate" + tara + "_P" + problema + ".txt";

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                    for (String concurent : concurenti) {
                        // Probabilitatea ca un concurent să nu rezolve problema
                        if (random.nextDouble() < 0.2) { // 20% șanse să nu apară
                            continue;
                        }

                        // Generăm punctajul: -1 (fraudă) sau între 0 și 100
                        int punctaj = random.nextDouble() < 0.02
                                ? -1
                                : random.nextInt(101); // punctaj între 0 și 100

                        // Scriem în fișier
                        writer.write(concurent + "," + punctaj);
                        writer.newLine();
                    }
                } catch (IOException e) {
                    System.err.println("Eroare la scrierea fișierului: " + fileName);
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Fișierele au fost generate în directorul: " + outputDir);
    }
}
