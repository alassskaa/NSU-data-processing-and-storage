package ru.sidorenko.java_2;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public final class LinkedStringList implements SortableList, Iterable<String> {

    private final Node head = new Node(null);

    @Override
    public void addFirst(String value) {
        Node node = new Node(Objects.requireNonNull(value));
        head.lock.lock();
        try {
            node.next = head.next;
            head.next = node;
        } finally {
            head.lock.unlock();
        }
    }

    @Override
    public int print(PrintStream out) {
        int count = 0;
        for (String value : this) {
            out.println(value);
            count++;
        }
        return count;
    }

    @Override
    public List<String> snapshot() {
        List<String> values = new ArrayList<>();
        for (String value : this) {
            values.add(value);
        }
        return values;
    }

    @Override
    public BubblePass newPass() {
        return new Pass();
    }

    @Override
    public Iterator<String> iterator() {
        return new LockingIterator();
    }

    private static Node swapIfGreater(Node before, Node first, Node second) {
        if (first.value.compareTo(second.value) <= 0) {
            return first;
        }
        before.next = second;
        first.next = second.next;
        second.next = first;
        return second;
    }

    private static final class Node {

        private final String value;
        private final ReentrantLock lock = new ReentrantLock();
        private Node next;

        private Node(String value) {
            this.value = value;
        }
    }

    private final class LockingIterator implements Iterator<String> {

        private Node current = head;
        private boolean finished;

        private LockingIterator() {
            current.lock.lock();
        }

        @Override
        public boolean hasNext() {
            if (finished) {
                return false;
            }
            if (current.next == null) {
                finished = true;
                current.lock.unlock();
                return false;
            }
            return true;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            Node following = current.next;
            following.lock.lock();
            current.lock.unlock();
            current = following;
            return following.value;
        }
    }

    private final class Pass implements BubblePass {

        private Node previous = head;

        @Override
        public boolean step(long insideStepMillis) throws InterruptedException {
            Node before = previous;
            before.lock.lockInterruptibly();
            try {
                Node first = before.next;
                if (first == null) {
                    return false;
                }
                first.lock.lockInterruptibly();
                try {
                    Node second = first.next;
                    if (second == null) {
                        return false;
                    }
                    second.lock.lockInterruptibly();
                    try {
                        Thread.sleep(insideStepMillis);
                        previous = swapIfGreater(before, first, second);
                        return true;
                    } finally {
                        second.lock.unlock();
                    }
                } finally {
                    first.lock.unlock();
                }
            } finally {
                before.lock.unlock();
            }
        }
    }
}
