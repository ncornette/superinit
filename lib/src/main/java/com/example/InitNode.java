package com.example;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class InitNode implements Runnable {

    private static final Set<InitNode> NO_NODES = Collections.emptySet();

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    protected Set<InitNode> parents = NO_NODES;
    protected Set<InitNode> children = NO_NODES;

    private Runnable task;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private boolean cancelled = false;
    private Exception error;

    public InitNode() {
        this(null);
    }

    public InitNode(Runnable task) {
        this.task = task == null ? new EmptyRunnable(): task;
    }

    public boolean finished() {
        return countDownLatch.getCount() == 0;
    }

    public boolean success() {
        return !cancelled() && error() == null && finished();
    }

    public boolean cancelled() {
        return cancelled;
    }

    public Exception error() {
        return error;
    }

    public void dependsOn(Collection<InitNode> parentNodes) {
        if (parents == NO_NODES) {
            parents = new HashSet<>();
        }
        for (InitNode parentNode : parentNodes) {
            if (parentNode.parents.contains(this) || parentNode == this) {
                throw new IllegalArgumentException(String.format(
                        "Error adding dependency: %s, circular dependency detected", parentNode));
            }
        }
        setParents(parentNodes);
        setChildOf(parentNodes);
    }

    private boolean setParents(Collection<InitNode> parentNodes) {
        return parents.addAll(parentNodes);
    }

    private void setChildOf(Collection<InitNode> parentNodes) {
        for (InitNode parentNode : parentNodes) {
            if (parentNode.children == NO_NODES) {
                parentNode.children = new HashSet<>();
            }
            parentNode.children.add(this);
        }

    }

    public void dependsOn(InitNode... newDependencies) {
        dependsOn(Arrays.asList(newDependencies));
    }

    @Override
    public void run() {
        if (this.uncaughtExceptionHandler != null) {
            Thread.currentThread().setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
        }
        System.out.println(String.format("%s, on thread %s : RUNNING", this.toString(), Thread.currentThread().getName()));
        for (InitNode dependency : parents) {
            try {
                System.out.println(String.format("%s, on thread %s :   WAITING for %s", this.toString(), Thread.currentThread().getName(), dependency));
                dependency.countDownLatch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException("interrupted", e);
            }
        }

        if (cancelled) {
            System.out.println(String.format("%s, on thread %s :   CANCELLED", this.toString(), Thread.currentThread().getName()));
            return;
        }

        runTask();

        countDownLatch.countDown();
        cancelled = false;
        System.out.println(String.format("%s, on thread %s : END", this.toString(), Thread.currentThread().getName()));
    }

    protected void runTask() {
        try {
            this.task.run();
        } catch (Exception e) {
            error = e;
            throw new TaskExecutionError(this, e);
        }
    }

    public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    public void cancel() {
        if (!finished() && error() == null) {
            cancelled = true;
        }
    }

    void unlock() {
        if (countDownLatch.getCount() == 1) {
            countDownLatch.countDown();
        }
    }

    private static class EmptyRunnable implements Runnable {
        @Override
        public void run() {

        }
    }

    @Override
    public String toString() {
        return "InitNode{" +
                "task=" + task +
                '}';
    }

    public static class TaskExecutionError extends RuntimeException {

        private InitNode node;

        public TaskExecutionError(InitNode node, Exception e) {
            super("Failed running node: "+ node, e);
            this.node = node;
        }

        public InitNode node() {
            return node;
        }
    }

    public static class TaskCancelledError extends TaskExecutionError {

        public TaskCancelledError(InitNode node, Exception e) {
            super(node, e);
        }
    }
}
