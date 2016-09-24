package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InitLoader {

    private final ExecutorService executorService;
    List<Node> resolved;
    Node terminationInit;

    public InitLoader() {
        executorService = Executors.newFixedThreadPool(5);
    }

    public void load(Runnable terminateCallback, Node... inits) {

        resolved = new ArrayList<>();
        dep_resolve(Arrays.asList(inits), resolved);

        for (Node node : resolved) {
            executorService.submit(node);
        }

        terminationInit = new Node(terminateCallback);
        terminationInit.dependsOn(inits);
        executorService.submit(terminationInit);
    }

    public void load(Node... inits) {
        this.load(null, inits);
    }

    void dep_resolve(List<Node> init, List<Node> resolved) {
        for (Node node : init) {
            dep_resolve(node, resolved);
        }

    }

    void dep_resolve(Node init, List<Node> resolved) {
        for (Node node : init.dependencies) {
            dep_resolve(node, resolved);
        }
        if (!resolved.contains(init)) {
            resolved.add(init);
        }
    }

    public void await() throws InterruptedException {
        for (Node node : resolved) {
            node.countDownLatch.await();
        }

    }
}
