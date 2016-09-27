package com.example;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class InitNode implements Runnable {

    private static final Set<InitNode> NO_NODES = Collections.emptySet();

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    Set<InitNode> parents = NO_NODES;
    Set<InitNode> children = NO_NODES;

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
        return finished() && !cancelled() && !error();
    }

    public boolean cancelled() {
        return cancelled;
    }

    public boolean error() {
        return error != null;
    }

    public Throwable getError() {
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

    public void dependsOn(InitNode... newDependencies) {
        dependsOn(Arrays.asList(newDependencies));
    }

    public void await() throws InterruptedException {
        countDownLatch.await(24, TimeUnit.HOURS);
    }

    public void cancel() {
        if (!finished() && !error()) {
            cancelled = true;
        }
    }

    public void cancelTree() {
        cancel();
        cancelChildren();
        unlock();
        unlockChildren();
    }

    private void cancelChildren() {
        for (InitNode child : children) {
            child.cancel();
            child.cancelChildren();
        }
    }

    private void unlockChildren() {
        for (InitNode child : children) {
            child.unlock();
            child.unlockChildren();
        }
    }

    @Override
    public void run() {
        if (this.uncaughtExceptionHandler != null) {
            Thread.currentThread().setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
        }
        System.out.println(String.format("Run %s, on thread %s : RUNNING", this.toString(), Thread.currentThread().getName()));
        for (InitNode dependency : parents) {
            try {
                System.out.println(String.format("Run %s, on thread %s :   WAITING for %s", this.toString(), Thread.currentThread().getName(), dependency));
                dependency.countDownLatch.await();
                if (dependency.cancelled()) {
                    cancel();
                }
            } catch (InterruptedException e) {
                throw new IllegalStateException("interrupted", e);
            }
        }

        if (cancelled) {
            System.out.println(String.format("Run %s, on thread %s :   CANCELLED", this.toString(), Thread.currentThread().getName()));
            return;
        }

        runTask();

        countDownLatch.countDown();
        cancelled = false;
        System.out.println(String.format("Run %s, on thread %s : END", this.toString(), Thread.currentThread().getName()));
    }

    @Override
    public String toString() {
        return "InitNode{" +
                "task=" + task +
                '}';
    }

    void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    void unlock() {
        if (countDownLatch.getCount() == 1) {
            countDownLatch.countDown();
        }
    }

    protected void runTask() {
        try {
            this.task.run();
        } catch (Exception e) {
            error = e;
            throw new TaskExecutionError(this, e);
        }
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

    private static class EmptyRunnable implements Runnable {
        @Override
        public void run() {

        }
    }

    static class TaskExecutionError extends RuntimeException {

        private InitNode node;

        TaskExecutionError(InitNode node, Exception e) {
            super("Failed running node: "+ node, e);
            this.node = node;
        }

        InitNode node() {
            return node;
        }
    }

    static class TaskCancelledError extends TaskExecutionError {

        public TaskCancelledError(InitNode node, Exception e) {
            super(node, e);
        }
    }
}
