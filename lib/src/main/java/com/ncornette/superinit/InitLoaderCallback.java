package com.ncornette.superinit;

public interface InitLoaderCallback {

    void onFinished();

    void onNodeError(NodeExecutionError nodeError);

    void onError(Throwable error);

    void onCancelled();
}
