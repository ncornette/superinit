package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class InitNode implements Init {

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    protected List<InitNode> dependencies = Collections.EMPTY_LIST;
    private Runnable task;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;
    private AtomicBoolean cancelled = new AtomicBoolean(false);
    private Exception error;

    public InitNode() {
        this(null);
    }

    public InitNode(Runnable task) {
        this.task = task == null ? new EmptyRunnable(): task;
    }

    @Override
    public boolean finished() {
        return countDownLatch.getCount() == 0;
    }

    public boolean success() {
        return !cancelled() && error() == null && finished();
    }

    public boolean cancelled() {
        return cancelled.get();
    }

    public Exception error() {
        return error;
    }

    public void dependsOn(Collection<InitNode> newDependencies) {
        if (dependencies == Collections.EMPTY_LIST) {
            dependencies = new ArrayList<>();
        }
        for (InitNode initNode : newDependencies) {
            if (initNode.dependencies.contains(this)) {
                throw new IllegalArgumentException(String.format(
                        "Error adding dependency: %s, circular dependency detected", initNode));
            }
            if (!dependencies.contains(initNode)) {
                dependencies.add(initNode);
            }
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
        for (InitNode dependency : dependencies) {
            try {
                System.out.println(String.format("%s, on thread %s :   WAITING for %s", this.toString(), Thread.currentThread().getName(), dependency));
                dependency.countDownLatch.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException("interrupted", e);
            }
        }

        if (cancelled.get()) {
            System.out.println(String.format("%s, on thread %s :   CANCELLED", this.toString(), Thread.currentThread().getName()));
            throw new TaskCancelledError(this, null);
        }

        runTask();

        countDownLatch.countDown();
        cancelled.set(false);
        System.out.println(String.format("%s, on thread %s : END", this.toString(), Thread.currentThread().getName()));
    }

    protected void runTask() {
        try {
            this.task.run();
        } catch (Exception e) {
            error = e;
            throw new RunTaskError(this, e);
        }
    }

    public void setUncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    public void cancel() {
        if (!finished() && error() == null) {
            cancelled.set(true);
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

    public static class RunTaskError extends RuntimeException {

        private InitNode node;

        public RunTaskError(InitNode node, Exception e) {
            super("Failed running node: "+ node, e);
            this.node = node;
        }

        public InitNode node() {
            return node;
        }
    }

    public static class TaskCancelledError extends RunTaskError {

        public TaskCancelledError(InitNode node, Exception e) {
            super(node, e);
        }
    }
}
