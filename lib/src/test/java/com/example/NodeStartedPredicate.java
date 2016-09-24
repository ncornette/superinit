package com.example;

import java.util.function.Predicate;

class NodeStartedPredicate implements Predicate<Init> {
    @Override
    public boolean test(Init init) {
        return init.isStarted();
    }
}
