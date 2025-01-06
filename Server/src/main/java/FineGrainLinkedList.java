import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FineGrainLinkedList {
    final Node head;
    final Node tail;

    Util util = new Util();

    public FineGrainLinkedList() {
        head = new Node(null);
        tail = new Node(null);
        head.next = tail;
    }

    public void insert(Concurent concurent) {
        Node pred = head;
        pred.lock.lock();
        try {
            Node curr = pred.next;
            curr.lock.lock();
            try {
                while (curr != tail) {
                    pred.lock.unlock();
                    pred = curr;
                    curr = curr.next;
                    curr.lock.lock();
                }
                Node newNode = new Node(concurent);
                newNode.next = curr;
                pred.next = newNode;
            } finally {
                curr.lock.unlock();
            }
        } finally {
            pred.lock.unlock();
        }
    }

    public void delete(String id) {
        Node pred = head;
        pred.lock.lock();
        try {
            Node curr = pred.next;
            curr.lock.lock();
            try {
                while (curr != tail && !curr.concurent.getId().equals(id)) {
                    pred.lock.unlock();
                    pred = curr;
                    curr = curr.next;
                    curr.lock.lock();
                }
                if (curr != tail) {
                    pred.next = curr.next;
                }
            } finally {
                curr.lock.unlock();
            }
        } finally {
            pred.lock.unlock();
        }
    }

    public Concurent get(String id) {
        Node pred = head;
        pred.lock.lock();
        try {
            Node curr = pred.next;
            curr.lock.lock();
            try {
                while (curr != tail && !curr.concurent.getId().equals(id)) {
                    pred.lock.unlock();
                    pred = curr;
                    curr = curr.next;
                    curr.lock.lock();
                }
                return curr != tail ? curr.concurent : null;
            } finally {
                curr.lock.unlock();
            }
        } finally {
            pred.lock.unlock();
        }
    }

    public void update(Concurent concurent) {
        Node pred = head;
        pred.lock.lock();
        try {
            Node curr = pred.next;
            curr.lock.lock();
            try {
                while (curr != tail && !curr.concurent.getId().equals(concurent.getId())) {
                    pred.lock.unlock();
                    pred = curr;
                    curr = curr.next;
                    curr.lock.lock();
                }
                if (curr != tail) {
                    curr.concurent.setPunctaj(concurent.getPunctaj());
                }
            } finally {
                curr.lock.unlock();
            }
        } finally {
            pred.lock.unlock();
        }
    }
    public void writeToFile(String filePath) {
        List<Concurent> concurents = new ArrayList<>();
        Node current = head.next;
        while (current != tail) {
            concurents.add(current.concurent);
            current = current.next;
        }

        concurents.sort(Comparator.comparingInt(Concurent::getPunctaj).reversed().thenComparing(Concurent::getId));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            util.log("Writing to file..");
            for (Concurent concurent : concurents) {
                writer.write(concurent.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}