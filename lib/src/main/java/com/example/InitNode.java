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

    Set<InitNode> dependencies = NO_NODES;
    Set<InitNode> descendants = NO_NODES;

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

    public void dependsOn(Collection<InitNode> dependencies) {
        if (this.dependencies == NO_NODES) {
            this.dependencies = new HashSet<>();
        }
        for (InitNode dependency : dependencies) {
            if (dependency.dependencies.contains(this) || dependency == this) {
                throw new IllegalArgumentException(String.format(
                        "Error adding dependency: %s, circular dependency detected", dependency));
            }
        }
        setDependencies(dependencies);
        setDescendantOf(dependencies);
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
        cancelDescendents();
        unlock();
        unlockDescendants();
    }

    private void cancelDescendents() {
        for (InitNode descendant : descendants) {
            descendant.cancel();
            descendant.cancelDescendents();
        }
    }

    private void unlockDescendants() {
        for (InitNode descendant : descendants) {
            descendant.unlock();
            descendant.unlockDescendants();
        }
    }

    @Override
    public void run() {
        if (this.uncaughtExceptionHandler != null) {
            Thread.currentThread().setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
        }
        System.out.println(String.format("Run %s, on thread %s : RUNNING", this.toString(), Thread.currentThread().getName()));
        for (InitNode dependency : dependencies) {
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

    private boolean setDependencies(Collection<InitNode> dependencies) {
        return this.dependencies.addAll(dependencies);
    }

    private void setDescendantOf(Collection<InitNode> dependencies) {
        for (InitNode dependency : dependencies) {
            if (dependency.descendants == NO_NODES) {
                dependency.descendants = new HashSet<>();
            }
            dependency.descendants.add(this);
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
