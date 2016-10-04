package com.ncornette.superinit;

public interface InitLoaderCallback {

    void onFinished();

    void onError(NodeExecutionError nodeError);

    void onError(Throwable error);

}
