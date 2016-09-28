package com.example;

import java.util.Collections;

public class InitLoaderDependenciesNoDelayRepeat extends InitLoaderDependencies {

    public static final int REPEAT_COUNT = 50;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Collections.shuffle(initNodes);
    }

    @Override
    protected int taskDelay() {
        return 0;
    }

    @Override
    public void test_InitNode_Cancel() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            super.test_InitNode_Cancel();
            setUp();
        }
    }

    @Override
    public void test_InitNode_Load_Twice() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            super.test_InitNode_Load_Twice();
            setUp();
        }
    }

    @Override
    public void test_InitLoader_Task_Error() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            super.test_InitLoader_Task_Error();
            setUp();
        }
    }

    @Override
    public void test_InitLoader_1Thread() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            super.test_InitLoader_1Thread();
            setUp();
        }
    }

    @Override
    public void test_InitLoader_2Threads() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            super.test_InitLoader_2Threads();
            setUp();
        }
    }

    @Override
    public void test_InitLoader_3Threads() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            super.test_InitLoader_3Threads();
            setUp();
        }
    }

    @Override
    public void test_InitLoader_5Threads() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            super.test_InitLoader_5Threads();
            setUp();
        }
    }

    @Override
    public void test_InitLoader_9Threads() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            super.test_InitLoader_9Threads();
            setUp();
        }
    }
}
