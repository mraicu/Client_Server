import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Node {
    public Concurent concurent;
    public Node next;
    public final Lock lock = new ReentrantLock();

    public Node(Concurent concurent) {
        this.concurent = concurent;
    }
}