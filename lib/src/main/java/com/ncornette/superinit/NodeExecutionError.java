package com.ncornette.superinit;

public class NodeExecutionError extends RuntimeException {

    private InitNode node;

    NodeExecutionError(InitNode node, Exception e) {
        super("Failed running node: " + node, e);
        this.node = node;
    }

    public InitNode node() {
        return node;
    }
}
