package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InitLoader {

    private final ExecutorService executorService;
    List<InitNode> resolved;
    private InitNode endInitNode;

    public InitLoader(int nThreads) {
        executorService = Executors.newFixedThreadPool(nThreads);
    }

    public void load(Runnable loadedCallback, InitNode... initNodes) {

        resolved = new ArrayList<>();
        dep_resolve(Arrays.asList(initNodes), resolved);

        for (InitNode initNode : resolved) {
            executorService.execute(initNode);
            System.out.printf("Load %s%n", initNode);
        }

        endInitNode = new InitNode(loadedCallback);
        endInitNode.dependsOn(initNodes);
        executorService.execute(endInitNode);
    }

    public void load(InitNode... initNodes) {
        this.load(null, initNodes);
    }

    void dep_resolve(List<InitNode> initNodes, List<InitNode> resolved) {
        for (InitNode initNode : initNodes) {
            dep_resolve(initNode, resolved);
        }

    }

    void dep_resolve(InitNode initNode, List<InitNode> resolved) {
        for (InitNode dependency : initNode.dependencies) {
            dep_resolve(dependency, resolved);
        }
        if (!resolved.contains(initNode)) {
            resolved.add(initNode);
        }
    }

    public void await() throws InterruptedException {
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }
}
