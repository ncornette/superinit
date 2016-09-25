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

    public void load(InitLoaderCallback loaderCallback, InitNode... initNodes) {

        resolved = new ArrayList<>();
        dep_resolve(Arrays.asList(initNodes), resolved);

        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                System.err.println(thread + " error: " + throwable);
                if (throwable instanceof InitNode.RunTaskError) {
                    InitNode.RunTaskError runTaskError = (InitNode.RunTaskError) throwable;
                    loaderCallback.onError(runTaskError.node(), throwable);
                } else {
                    loaderCallback.onError(null, throwable);
                }

                cancel();
            }
        };

        for (InitNode initNode : resolved) {
            initNode.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            executorService.execute(initNode);
            System.out.printf("Load %s%n", initNode);
        }

        endInitNode = new InitNode(new Runnable() {
            @Override
            public void run() {
                loaderCallback.onTerminate();
            }
        });
        endInitNode.dependsOn(initNodes);
        executorService.execute(endInitNode);
    }

    private void cancel() {
        executorService.shutdown();
        for (InitNode initNode : resolved) {
            initNode.cancel();
        }
        for (InitNode initNode : resolved) {
            System.out.println("unlock: " + initNode);
            initNode.unlock();
        }
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
        executorService.awaitTermination(24, TimeUnit.HOURS);
    }

    public interface InitLoaderCallback {
        void onTerminate();
        void onError(InitNode initNode, Throwable t);
    }
}
