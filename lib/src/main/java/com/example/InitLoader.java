package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InitLoader {

    private final ExecutorService executorService;
    Collection<InitNode> resolved;

    public InitLoader(int nThreads) {
        executorService = Executors.newFixedThreadPool(nThreads);
    }

    public void load(InitLoaderCallback loaderCallback, Collection<InitNode> initNodes) {
        if (resolved != null) {
            throw new IllegalStateException("Load() method already called");
        }
        resolved = new ArrayList<>();
        dep_resolve(initNodes, resolved);

        InitNode endNode = new InitNode(new NotifyTerminateTask(loaderCallback));
        endNode.dependsOn(resolved);
        resolved.add(endNode);

        try {
            executeNodes(loaderCallback, resolved);
        } catch (Exception e) {
            loaderCallback.onError(null, e);
        }
    }

    public void load(InitLoaderCallback loaderCallback, InitNode... initNodes) {
        load(loaderCallback, Arrays.asList(initNodes));
    }

    private void executeNodes(InitLoaderCallback loaderCallback, Collection<InitNode> nodes) {
        for (InitNode node : nodes) {
            node.setUncaughtExceptionHandler(new LoadUncaughtExceptionHandler(loaderCallback));
            executorService.execute(node);
            System.out.printf("Load %s%n", node);
        }
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

    static void dep_resolve(Collection<InitNode> initNodes, Collection<InitNode> resolved) {
        for (InitNode initNode : initNodes) {
            if (initNode.dependencies.isEmpty()) {
                resolved.add(initNode);
            }
        }
        for (InitNode initNode : initNodes) {
            dep_resolve(initNode, resolved);
        }

    }

    private static void dep_resolve(InitNode initNode, Collection<InitNode> resolved) {
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

    private class LoadUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final InitLoaderCallback loaderCallback;

        public LoadUncaughtExceptionHandler(InitLoaderCallback loaderCallback) {
            this.loaderCallback = loaderCallback;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            //System.err.println(thread + " error: " + throwable);
            if (throwable instanceof InitNode.RunTaskError) {
                InitNode.RunTaskError runTaskError = (InitNode.RunTaskError) throwable;
                loaderCallback.onError(runTaskError.node(), throwable);
            } else {
                loaderCallback.onError(null, throwable);
            }

            cancel();
        }
    }

    private static class NotifyTerminateTask implements Runnable {
        private final InitLoaderCallback loaderCallback;

        public NotifyTerminateTask(InitLoaderCallback loaderCallback) {
            this.loaderCallback = loaderCallback;
        }

        @Override
        public void run() {
            loaderCallback.onTerminate();
        }
    }
}
