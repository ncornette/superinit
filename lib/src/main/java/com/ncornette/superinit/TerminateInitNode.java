package com.ncornette.superinit;

import java.util.concurrent.atomic.AtomicBoolean;

public class TerminateInitNode extends InitNode {

    private final InitLoaderCallback loaderCallback;
    private AtomicBoolean mCancelled = new AtomicBoolean(false);

    public TerminateInitNode(InitLoaderCallback loaderCallback) {
        this.loaderCallback = loaderCallback;
    }

    @Override
    public void cancel() {
        mCancelled.set(true);
        // Cannot be cancelled
        //super.cancel();

    }

    @Override
    protected void runTask() {
        super.runTask();

        if (mCancelled.get()) {
            loaderCallback.onCancelled();
        } else if (loaderCallback != null) {
            loaderCallback.onFinished();
        }

    }
}
