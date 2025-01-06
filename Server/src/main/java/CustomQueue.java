import java.lang.reflect.Array;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CustomQueue<T> {
    private final T[] items;
    private int head, tail, count;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    @SuppressWarnings("unchecked")
    public CustomQueue(Class<T> clazz, int capacity) {
        items = (T[]) Array.newInstance(clazz, capacity);
    }

    public void enqueue(T item) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {
                notFull.await();
            }
            items[tail] = item;
            if (++tail == items.length) {
                tail = 0;
            }
            ++count;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T dequeue() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            T item = items[head];
            if (++head == items.length) {
                head = 0;
            }
            --count;
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }
}