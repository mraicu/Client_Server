import java.util.ArrayList;

public class Consumer extends Thread{
    private FineGrainLinkedList clasament;
    private CustomQueue<Concurent> queue;
    private ArrayList<String> blackList;

    public Consumer(FineGrainLinkedList clasament, CustomQueue<Concurent> queue, ArrayList<String> blackList) {
        this.clasament = clasament;
        this.queue = queue;
        this.blackList = blackList;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Concurent concurent = queue.dequeue();
                if (concurent.getId() == "null") {
                    break;
                }
                processConcurent(concurent);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        clasament.writeToFile("clasament.txt");
    }

    private void processConcurent(Concurent concurent) {
        synchronized (blackList) {
            if (!blackList.contains(concurent.getId())) {
                if (concurent.getPunctaj() < 0) {
                    blackList.add(concurent.getId());
                    clasament.delete(concurent.getId());
                } else {
                    Concurent existing = clasament.get(concurent.getId());
                    if (existing != null) {
                        concurent.setPunctaj(concurent.getPunctaj() + existing.getPunctaj());
                        clasament.update(concurent);
                    } else {
                        clasament.insert(concurent);
                    }
                }
            }
        }
    }
}
