package com.ncornette.superinit;

import java.util.Collections;

public class InitLoaderDependenciesNoDelayRepeat extends InitLoaderDependencies {

    public static final int REPEAT_COUNT = 20;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Ensure input order does not interfere with results
        Collections.shuffle(initNodes);
    }

    @Override
    protected int taskDelay() {
        return 0;
    }

    @Override
    public void test_InitNode_Cancel() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            if (i > 0) setUp();
            super.test_InitNode_Cancel();
            if (i < REPEAT_COUNT - 1) tearDown();
        }
    }

    @Override
    public void test_InitLoader_Task_Error() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            if (i > 0) setUp();
            super.test_InitLoader_Task_Error();
            if (i < REPEAT_COUNT - 1) tearDown();
        }
    }

    @Override
    public void test_InitLoader_1Thread() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            if (i > 0) setUp();
            super.test_InitLoader_1Thread();
            if (i < REPEAT_COUNT - 1) tearDown();
        }
    }

    @Override
    public void test_InitLoader_2Threads() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            if (i > 0) setUp();
            super.test_InitLoader_2Threads();
            if (i < REPEAT_COUNT - 1) tearDown();
        }
    }

    @Override
    public void test_InitLoader_3Threads() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            if (i > 0) setUp();
            super.test_InitLoader_3Threads();
            if (i < REPEAT_COUNT - 1) tearDown();
        }
    }

    @Override
    public void test_InitLoader_5Threads() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            if (i > 0) setUp();
            super.test_InitLoader_5Threads();
            if (i < REPEAT_COUNT - 1) tearDown();
        }
    }

    @Override
    public void test_InitLoader_9Threads() throws Exception {
        for (int i = 0; i < REPEAT_COUNT; i++) {
            if (i > 0) setUp();
            super.test_InitLoader_9Threads();
            if (i < REPEAT_COUNT - 1) tearDown();
        }
    }
}
