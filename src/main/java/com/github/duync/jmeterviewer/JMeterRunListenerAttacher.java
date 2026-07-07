package com.github.duync.jmeterviewer;

import org.apache.jmeter.testelement.TestPlan;
import org.apache.jorphan.collections.HashTree;

final class JMeterRunListenerAttacher {
    private JMeterRunListenerAttacher() {
    }

    static void attach(HashTree tree, Object listener) {
        if (tree == null || listener == null) {
            return;
        }
        if (!attachToTestPlans(tree, listener)) {
            tree.add(listener);
        }
    }

    private static boolean attachToTestPlans(HashTree tree, Object listener) {
        boolean attached = false;
        for (Object item : tree.getArray()) {
            HashTree childTree = tree.getTree(item);
            if (item instanceof TestPlan) {
                childTree.add(listener);
                attached = true;
            }
            attached |= attachToTestPlans(childTree, listener);
        }
        return attached;
    }
}
