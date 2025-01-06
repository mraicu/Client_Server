package util;

import entity.Participant;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class IOHandler {

    public static List<String> generateFileNames() {
        List<String> files = new LinkedList<>();
        String format = "Rezultate_C%d_P%d.txt";
        for (int i = 1; i <= Constants.NUMBER_OF_THREADS; ++i) {
            for (int j = 1; j <= 10; ++j) {
                files.add(String.format(format, i, j));
            }
        }
        return files;
    }
    public static List<Participant> readParticipants(List<String> files) {
        List<Participant> concurenti = new LinkedList<>();
        for (String file : files) {
            String country = file.split("_")[1];
            try (BufferedReader br = new BufferedReader(new FileReader(
                    Objects.requireNonNull(Client.class.getClassLoader().getResource("data/" + file)).getFile()
            ))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] tokens = line.split(" ");
                    concurenti.add(new Participant(tokens[0], Integer.parseInt(tokens[1]), getCountry(country)));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return concurenti;
    }

    private static String getCountry(String country) {
        if (country.equals("C1")) {
            return "Romania";
        } else if (country.equals("C2")) {
            return "Austria";
        } else if (country.equals("C3")) {
            return "Germania";
        } else if (country.equals("C4")) {
            return "Italia";
        } else if (country.equals("C5")) {
            return "Spania";
        } else {
            throw new RuntimeException("Invalid country!");
        }
    }
}
