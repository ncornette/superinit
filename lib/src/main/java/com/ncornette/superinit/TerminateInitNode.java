package com.ncornette.superinit;

public class TerminateInitNode extends InitNode {

    private final InitLoaderCallback loaderCallback;

    public TerminateInitNode(InitLoaderCallback loaderCallback) {
        this.loaderCallback = loaderCallback;
    }

    @Override
    public void cancel() {

        // Cannot be cancelled
        //super.cancel();

    }

    @Override
    protected void runTask() {
        super.runTask();

        if (loaderCallback != null) {
            loaderCallback.onFinished();
        }

    }
}
