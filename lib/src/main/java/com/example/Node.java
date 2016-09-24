package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Node implements Init {

    CountDownLatch countDownLatch = new CountDownLatch(1);

    protected List<Node> dependencies = Collections.EMPTY_LIST;
    private Runnable task;

    public Node() {
        this(null);
    }

    public Node(Runnable task) {
        this.task = task;
    }

    @Override
    public boolean isStarted() {
        return countDownLatch.getCount() == 0;
    }

    public void dependsOn(Node... newDependencies) {
        for (Node node : newDependencies) {
            if (node.dependencies.contains(this)) {
                throw new IllegalArgumentException(String.format(
                        "Error adding dependency: %s, circular dependency detected", node));
            }
        }
        if (dependencies == Collections.EMPTY_LIST) {
            dependencies = new ArrayList<>();
        }
        Collections.addAll(dependencies, newDependencies);
    }

    @Override
    public void run() {
        for (Node dependency : dependencies) {
            try {
                dependency.countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (this.task != null) {
            this.task.run();
        }
        countDownLatch.countDown();
    }

}
