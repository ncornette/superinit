package com.ncornette.superinit;

public class TerminateInitNode extends InitNode {

    private final InitLoaderCallback loaderCallback;
    private boolean mCancelled;

    public TerminateInitNode(InitLoaderCallback loaderCallback) {
        this.loaderCallback = loaderCallback;
        mCancelled = false;
    }

    @Override
    public void cancel() {
        mCancelled = true;
        // Cannot be cancelled
        //super.cancel();

    }

    @Override
    protected void runTask() {
        super.runTask();

        if (mCancelled) {
            loaderCallback.onCancelled();
        } else if (loaderCallback != null) {
            loaderCallback.onFinished();
        }

    }
}
