package com.example;

public class InitLoaderDependencies extends InitLoaderTest {

    @Override
    protected void setupDependencies() {
        // +--- C
        // |    +--- D
        // |    \--- B
        // |         +--- A
        // |         \--- F
        // +--- E
        // +--- G
        // +--- H
        //      \--- I

        initA.dependsOn(initB);
        initB.dependsOn(initC);
        initD.dependsOn(initC);
        initF.dependsOn(initB);
        initI.dependsOn(initH);

    }
}
