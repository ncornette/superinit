package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InitLoader {

    private final ExecutorService executorService;
    List<Node> resolved;
    Node terminationNode;

    public InitLoader(int nThreads) {
        executorService = Executors.newFixedThreadPool(nThreads);
    }

    public void load(Runnable terminateCallback, Node... nodes) {

        resolved = new ArrayList<>();
        dep_resolve(Arrays.asList(nodes), resolved);

        for (Node node : resolved) {
            executorService.execute(node);
            System.out.printf("submit %s%n", node);
        }

        terminationNode = new Node(terminateCallback);
        terminationNode.dependsOn(nodes);
        executorService.execute(terminationNode);
    }

    public void load(Node... nodes) {
        this.load(null, nodes);
    }

    void dep_resolve(List<Node> nodes, List<Node> resolved) {
        for (Node node : nodes) {
            dep_resolve(node, resolved);
        }

    }

    void dep_resolve(Node node, List<Node> resolved) {
        for (Node dependency : node.dependencies) {
            dep_resolve(dependency, resolved);
        }
        if (!resolved.contains(node)) {
            resolved.add(node);
        }
    }

    public void await() throws InterruptedException {
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }
}
