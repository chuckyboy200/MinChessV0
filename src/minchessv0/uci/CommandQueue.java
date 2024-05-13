package minchessv0.uci;

import java.util.ArrayList;
import java.util.List;

public class CommandQueue {
    
    public CommandQueue() {
        this.queue = new ArrayList<>();
    }

    public synchronized void add(String string) {
        this.queue.add(string);
    }

    public synchronized void addFront(String string) {
        this.queue.add(0, string);
    }

    public synchronized String getNext() {
        if(this.queue.size() == 0) return "";
        String next = this.queue.get(0);
        this.queue.remove(0);
        return next;
    }

    public synchronized boolean hasNext() {
        return !this.queue.isEmpty();
    }

    private List<String> queue;

}
