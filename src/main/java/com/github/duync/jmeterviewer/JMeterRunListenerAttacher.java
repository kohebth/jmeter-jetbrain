package com.github.duync.jmeterviewer;

import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jorphan.collections.HashTree;

final class JMeterRunListenerAttacher {
    private JMeterRunListenerAttacher() {
    }

    static void attach(HashTree tree, Object listener) {
        if (tree == null || listener == null) {
            return;
        }
        if (!attachToThreadGroups(tree, listener)) {
            tree.add(listener);
        }
    }

    private static boolean attachToThreadGroups(HashTree tree, Object listener) {
        boolean attached = false;
        for (Object item : tree.getArray()) {
            HashTree childTree = tree.getTree(item);
            if (item instanceof AbstractThreadGroup) {
                childTree.add(listener);
                attached = true;
            }
            attached |= attachToThreadGroups(childTree, listener);
        }
        return attached;
    }
}
