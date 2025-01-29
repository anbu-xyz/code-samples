package uk.anbu.poc.stickyloadbalancer;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class Main {
    static Random random = new Random();
    static ConcurrentHashMap<String, Entry> locks = new ConcurrentHashMap<>();
    public static void main(String[] args) {
        var blockingQueue = new ArrayBlockingQueue<Work>(100);
        // ------------------------------
        createWriter(blockingQueue);
        // ---------------------------

        Thread reader = new Thread(() -> {
            while(true) {
                doItAll(blockingQueue);
            }
        });
        reader.setName("reader");
        reader.start();
    }

    private static void doItAll(ArrayBlockingQueue<Work> blockingQueue) {
        var work = readNextWork(blockingQueue);
        var newList = new ArrayDeque<Work>();
        String threadName = "worker #" + work.partitionId();
        Entry previous;
        synchronized (locks) {
            previous = locks.putIfAbsent(threadName, new Entry(threadName, newList));
        }
        if (previous == null) {
            System.out.println("===> creating new");
            synchronized (threadName) {
                newList.push(work);
            }
            Thread worker = new Thread(() ->{
                String currentThreadName = Thread.currentThread().getName();
                var entry = locks.get(currentThreadName);
                Work nextWork;
                synchronized (threadName) {
                    nextWork = entry.workQueue.poll();
                }
                while (nextWork != null) {
                    System.out.println(currentThreadName + " doing " + nextWork);
                    sleep(random.nextInt(10, 100));
                    System.out.println(currentThreadName + " done " + nextWork);
                    synchronized (threadName) {
                        nextWork = entry.workQueue.poll();
                        if (nextWork == null) {
                            synchronized (locks) {
                                nextWork = entry.workQueue.poll();
                                if(nextWork == null) {
                                    locks.remove(threadName);
                                }
                            }
                        }
                    }
                }
            });
            worker.setName("worker #" + work.partitionId());
            worker.start();
        } else {
            System.out.println("===> Adding to previous");
            synchronized (previous.threadName) {
                previous.workQueue.add(work);
            }
        }
    }

    private static Work readNextWork(ArrayBlockingQueue<Work> blockingQueue) {
        var work = blockingQueue.poll();
        while(work == null) {
            sleep(100);
            work = blockingQueue.poll();
        }
        return work;
    }

    private static void createWriter(ArrayBlockingQueue<Work> blockingQueue) {
        Random random = new Random();
        var workList = IntStream.range(1, 1_000)
            .mapToObj(i -> new Work(random.nextInt(1, 6), i))
            .toList();

        workList.forEach(System.out::println);

        Thread writerThread = new Thread(() -> {
            AtomicInteger counter = new AtomicInteger();
            workList.stream().forEach(work -> {
                if ((counter.getAndIncrement()) % 28 == 0) {
                    sleep(1_000);
                    System.out.println("--------------------------------------------------------");
                }
                try {
                    blockingQueue.put(work);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            System.out.println("Done");
        });
        writerThread.setName("Writer");
        writerThread.start();
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public record Entry (String threadName, Queue<Work> workQueue) {}
    public record Work(int partitionId, int workNumber) {}
}
