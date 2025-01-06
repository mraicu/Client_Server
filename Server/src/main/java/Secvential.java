import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Secvential {
    private LinkedList<Concurent> clasament = new LinkedList<>();
    private List<String> blackList = new ArrayList<>();

    public void rezolvare() {
        long startTime = System.currentTimeMillis();
        File folder = new File("C:\\Users\\adrian.stan\\Desktop\\School\\PPD\\LAB4\\LAB4\\RezultateConcurs");
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            processLine(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\adrian.stan\\Desktop\\School\\PPD\\LAB4\\LAB4\\src\\main\\java\\org\\example\\ClasamentSecvential.txt"))) {
            for (Concurent concurent : clasament) {
                writer.write(concurent.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Time taken: " + duration + " milliseconds");
    }

    private void processLine(String line) {
        String[] parts = line.split(",");
        Concurent concurent = new Concurent(parts[0], Integer.parseInt(parts[1]), "");
        if (!blackList.contains(concurent.getId())){
            if (concurent.getPunctaj() < 0){
                blackList.add(concurent.getId());
                for (Concurent c : clasament) {
                    if (c.getId().equals(concurent.getId())) {
                        clasament.remove(c);
                        break;
                    }
                }
            } else {
                for (Concurent c : clasament) {
                    if (c.getId().equals(concurent.getId())) {
                        concurent.setPunctaj(concurent.getPunctaj() + c.getPunctaj());
                        clasament.remove(c);
                        break;
                    }
                }
                boolean added = false;
                for (int i = 0; i < clasament.size(); i++) {
                    if (concurent.getPunctaj() > clasament.get(i).getPunctaj() ||
                            (concurent.getPunctaj() == clasament.get(i).getPunctaj() && concurent.getId().compareTo(clasament.get(i).getId()) < 0)) {
                        clasament.add(i, concurent);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    clasament.add(concurent);
                }
            }

        }
    }
}
