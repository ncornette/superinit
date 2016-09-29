package com.example;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class InitNode implements Runnable {

    private static final Set<InitNode> EMPTY_SET = Collections.emptySet();
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    Set<InitNode> dependencies = EMPTY_SET;
    Set<InitNode> descendants = EMPTY_SET;

    private Runnable task;
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    private boolean executed = false;
    private boolean cancelled = false;
    private Exception error = null;

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
        return executed;
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
        if (this.dependencies == EMPTY_SET) {
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
        countDownLatch.await();
    }

    public void cancel() {
        if (finished()) {
            return;
        }
        cancelled = true;
        for (InitNode descendant : descendants) {
            descendant.cancel();
        }
        unlock();
    }

    @Override
    public void run() {
        if (success() || error()) {
            throw new IllegalStateException(String.format("%s already executed.", this.toString()));
        }
        if (this.uncaughtExceptionHandler != null) {
            Thread.currentThread().setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
        }
        for (InitNode dependency : dependencies) {
            try {
                dependency.await();
            } catch (InterruptedException e) {
                throw new IllegalStateException("interrupted", e);
            }
        }

        if (cancelled()) {
            return;
        }

        try {
            onRunTask();
            this.task.run();
        } catch (Exception e) {
            error = e;
            throw new TaskExecutionError(this, e);
        }

        error = null;
        executed = true;
        cancelled = false;
        countDownLatch.countDown();
    }

    protected void onRunTask() {

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

    private boolean setDependencies(Collection<InitNode> dependencies) {
        return this.dependencies.addAll(dependencies);
    }

    private void setDescendantOf(Collection<InitNode> dependencies) {
        for (InitNode dependency : dependencies) {
            if (dependency.descendants == EMPTY_SET) {
                dependency.descendants = new HashSet<>();
            }
            dependency.descendants.add(this);
        }

    }

    private static class EmptyRunnable implements Runnable {
        @Override public void run() {}
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

}
