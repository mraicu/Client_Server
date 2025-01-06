import entity.Participant;
import util.Client;
import util.IOHandler;

import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        List<String> files = IOHandler.generateFileNames();
        List<Participant> concurenti = IOHandler.readParticipants(files);

        double start = System.currentTimeMillis();

        Client client = new Client(concurenti);
        client.startSending();


        double end = System.currentTimeMillis();

        System.out.println(end - start);
    }
}
