package com.ncornette.superinit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class InitLoader {

    private final ExecutorService executorService;
    private final MyThreadFactory threadFactory;
    Collection<InitNode> resolved;
    private InitNode endNode;
    private List<InitNode> errorNodes;
    private InitLoaderCallback loaderCallback;

    public InitLoader(int nThreads) {
        threadFactory = new MyThreadFactory();
        executorService = new ThreadPoolExecutor(nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), threadFactory);
    }

    public void load(InitLoaderCallback loaderCallback, Collection<? extends InitNode> initNodes) {
        this.loaderCallback = loaderCallback;
        errorNodes = new ArrayList<>();
        if (resolved != null) {
            throw new IllegalStateException("Load() method already called");
        }

        endNode = new TerminateInitNode(loaderCallback);
        resolved = new ArrayList<>();
        dep_resolve(initNodes, resolved);

        endNode.dependsOn(resolved);
        resolved.add(endNode);

        try {
            executeNodes(loaderCallback, resolved);
        } catch (Exception e) {
            cancel();
            if (loaderCallback != null) {
                loaderCallback.onError(e);
            }
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

    static void dep_resolve(Collection<? extends InitNode> initNodes, Collection<InitNode> resolved) {
        for (InitNode initNode : initNodes) {
            if (initNode.dependencies().isEmpty()) {
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
        for (InitNode dependency : initNode.dependencies()) {
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

    public void cancel() {
        shutdown();
        for (InitNode initNode : resolved) {
            if (!initNode.cancelled()) {
                initNode.cancel();
            }
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public void awaitTasks() throws InterruptedException {
        while (!endNode.await(100, TimeUnit.MILLISECONDS));
    }

    public void awaitTermination() throws InterruptedException {
        if (executorService.isTerminated()) {
            return;
        }
        if (!executorService.isShutdown()) {
            awaitTasks();
            shutdown();
        }
        while(!executorService.awaitTermination(100, TimeUnit.MILLISECONDS));
    }

    public void interrupt() {
        executorService.shutdownNow();
    }

    public void retry() {
        retry(loaderCallback);
    }

    public void retry(InitLoaderCallback newCallback) {
        if (executorService.isTerminated() || executorService.isShutdown()) {
            throw new IllegalStateException("ExecutorService is terminated or shutdown.");
        }
        resolved = null;
        endNode = null;

        Collection<InitNode> initNodes = InitNode.newNodesWithDescendants(errorNodes);
        if (initNodes != null) {
            load(newCallback, initNodes);
        }
    }

    private static class NodeUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private InitLoader initLoader;
        private final InitLoaderCallback loaderCallback;

        public NodeUncaughtExceptionHandler(InitLoader initLoader, InitLoaderCallback loaderCallback) {
            this.initLoader = initLoader;
            this.loaderCallback = loaderCallback;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            try {
                if (throwable instanceof NodeExecutionError) {
                    NodeExecutionError nodeExecutionError = (NodeExecutionError) throwable;
                    if (loaderCallback != null) {
                        loaderCallback.onNodeError(nodeExecutionError);
                    }
                    // Cancel node in error & descendants
                    nodeExecutionError.node().cancel();
                    initLoader.addErrorNodes(nodeExecutionError.node());
                } else {
                    // Cancel all tasks from initloader
                    if (loaderCallback != null) {
                        loaderCallback.onError(throwable);
                    }
                    // Cancel all
                    initLoader.cancel();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addErrorNodes(InitNode... node) {
        if (errorNodes == null) {
            errorNodes = Collections.synchronizedList(new ArrayList<InitNode>());
        }
        errorNodes.addAll(Arrays.asList(node));
    }

    private static class MyThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final ThreadGroup threadGroup;

        public MyThreadFactory() {
            threadGroup = new ThreadGroup("InitLoader");
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(threadGroup, r,
                    String.format("InitLoader-thread-%d", threadNumber.getAndIncrement()));
        }
    }
}
