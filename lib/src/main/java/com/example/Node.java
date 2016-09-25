package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Node implements Init {

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    protected List<Node> dependencies = Collections.EMPTY_LIST;
    private Runnable task;

    public Node() {
        this(null);
    }

    public Node(Runnable task) {
        this.task = task == null ? new EmptyRunnable(): task;
    }

    @Override
    public boolean isStarted() {
        return countDownLatch.getCount() == 0;
    }

    public void dependsOn(Node... newDependencies) {
        if (dependencies == Collections.EMPTY_LIST) {
            dependencies = new ArrayList<>();
        }
        for (Node node : newDependencies) {
            if (node.dependencies.contains(this)) {
                throw new IllegalArgumentException(String.format(
                        "Error adding dependency: %s, circular dependency detected", node));
            }
            if (!dependencies.contains(node)) {
                dependencies.add(node);
            }
        }
    }

    @Override
    public void run() {
        System.out.println(String.format("%s, on thread %s : RUNNING", this.toString(), Thread.currentThread().getName()));
        for (Node dependency : dependencies) {
            try {
                System.out.println(String.format("%s, on thread %s : WAITING for %s", this.toString(), Thread.currentThread().getName(), dependency));
                dependency.countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println(String.format("%s, on thread %s : EXECUTE TASK", this.toString(), Thread.currentThread().getName()));
        runTask();

        countDownLatch.countDown();
        System.out.println(String.format("%s, on thread %s : END", this.toString(), Thread.currentThread().getName()));
    }

    protected void runTask() {
        this.task.run();
    }

    private static class EmptyRunnable implements Runnable {
        @Override
        public void run() {

        }
    }

    @Override
    public String toString() {
        return "Node{" +
                "task=" + task +
                '}';
    }
}
