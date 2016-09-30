package com.ncornette.superinit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InitLoader {

    private final ExecutorService executorService;
    Collection<InitNode> resolved;

    public InitLoader(int nThreads) {
        executorService = Executors.newFixedThreadPool(nThreads);
    }

    public void load(InitLoaderCallback loaderCallback, Collection<? extends InitNode> initNodes) {
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
            loaderCallback.onError(e);
        }
    }

    public void load(InitLoaderCallback loaderCallback, InitNode... initNodes) {
        load(loaderCallback, Arrays.asList(initNodes));
    }

    private void executeNodes(InitLoaderCallback loaderCallback, Collection<InitNode> nodes) {
        for (InitNode node : nodes) {
            node.setUncaughtExceptionHandler(new NodeUncaughtExceptionHandler(this, loaderCallback));
            executorService.execute(node);
        }
    }

    public void cancel() {
        executorService.shutdown();
        for (InitNode initNode : resolved) {
            initNode.cancel();
        }
    }

    static void dep_resolve(Collection<? extends InitNode> initNodes, Collection<InitNode> resolved) {
        for (InitNode initNode : initNodes) {
            if (initNode.dependencies.isEmpty()) {
                // Insert orphans first
                resolved.add(initNode);
            }
        }

        List<InitNode> seen = new ArrayList<>();
        for (InitNode initNode : initNodes) {
            seen.clear();
            dep_resolve(initNode, resolved, seen);
        }

    }

    private static void dep_resolve(InitNode initNode, Collection<InitNode> resolved, Collection<InitNode> seen) {
        seen.add(initNode);
        for (InitNode dependency : initNode.dependencies) {
            if (!resolved.contains(dependency)) {
                if (seen.contains(dependency)) {
                    throw new IllegalArgumentException(String.format("Circular Dependency: %s --> %s", initNode, dependency));
                }
                dep_resolve(dependency, resolved, seen);
            }
        }
        if (!resolved.contains(initNode)) {
            resolved.add(initNode);
        }
    }

    public void await() throws InterruptedException {
        for (InitNode initNode : resolved) {
            initNode.await();
        }
    }


    public interface InitLoaderCallback {

        void onFinished();

        void onError(Throwable t);

    }

    private static class NodeUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private InitLoader initLoader;
        private final InitLoaderCallback loaderCallback;

        public NodeUncaughtExceptionHandler(InitLoader initLoader, InitLoaderCallback loaderCallback) {
            this.initLoader = initLoader;
            this.loaderCallback = loaderCallback != null ? loaderCallback : new InitLoaderCallback() {
                @Override public void onFinished() {}
                @Override public void onError(Throwable t) {
                    t.printStackTrace();
                }
            };
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            try {
                if (throwable instanceof InitNode.NodeExecutionError) {
                    InitNode.NodeExecutionError nodeExecutionError = (InitNode.NodeExecutionError) throwable;
                    loaderCallback.onError(nodeExecutionError);
                    nodeExecutionError.node().cancel();
                } else {
                    // Cancel all tasks from initloader
                    loaderCallback.onError(throwable);
                    initLoader.cancel();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class NotifyTerminateTask implements Runnable {
        private final InitLoaderCallback loaderCallback;

        public NotifyTerminateTask(InitLoaderCallback loaderCallback) {
            this.loaderCallback = loaderCallback;
        }

        @Override
        public void run() {
            if (loaderCallback != null) {
                loaderCallback.onFinished();
            }
        }
    }
}
