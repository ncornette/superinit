package com.ncornette.superinit;

class NotifyTerminateTask implements Runnable {
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

    @Override
    public String toString() {
        return "NotifyTerminateTask{}";
    }
}
