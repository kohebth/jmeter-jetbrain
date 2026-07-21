/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.apache.jmeter.exceptions.IllegalUserActionException;
import org.apache.jmeter.gui.action.ActionNames;
import org.apache.jmeter.gui.action.ActionRouter;
import org.apache.jmeter.gui.action.Load;
import org.apache.jmeter.gui.tree.JMeterTreeListener;
import org.apache.jmeter.gui.tree.JMeterTreeModel;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.collections.HashTree;
import org.apiguardian.api.API;

/**
 * Small, source-level embedding boundary for JMeter's native authoring UI.
 *
 * <p>The workspace deliberately owns one normal {@link GuiPackage}, action
 * router, tree model, tree listener, and hidden {@link MainFrame}. This keeps
 * JMeter's native menus, context actions, shortcuts, undo history, and GUI
 * component lifecycle intact while allowing another Swing application to own
 * the visible window and document persistence.</p>
 */
@API(since = "5.6.3", status = API.Status.EXPERIMENTAL)
public final class EmbeddedJMeterWorkspace implements AutoCloseable {
    private static final int EMBEDDED_ACTION_ID = 0x4a4d58;

    private final GuiPackage guiPackage;
    private final MainFrame mainFrame;
    private final ActionRouter actionRouter;
    private boolean closed;

    private EmbeddedJMeterWorkspace(
            GuiPackage guiPackage,
            MainFrame mainFrame,
            ActionRouter actionRouter) {
        this.guiPackage = guiPackage;
        this.mainFrame = mainFrame;
        this.actionRouter = actionRouter;
    }

    /**
     * Create the application-wide native JMeter authoring session.
     *
     * @return initialized workspace
     */
    public static EmbeddedJMeterWorkspace create() {
        requireEventDispatchThread();
        if (GuiPackage.getInstance() != null) {
            throw new IllegalStateException("A JMeter GUI workspace is already active");
        }

        GuiPackage guiPackage = null;
        MainFrame mainFrame = null;
        try {
            JMeterTreeModel treeModel = new JMeterTreeModel();
            JMeterTreeListener treeListener = new JMeterTreeListener(treeModel);
            ActionRouter actionRouter = ActionRouter.getInstance();
            actionRouter.populateCommandMap();
            treeListener.setActionHandler(actionRouter);
            GuiPackage.initInstance(treeListener, treeModel);
            guiPackage = GuiPackage.getInstance();
            mainFrame = new MainFrame(treeModel, treeListener, true);
            EmbeddedJMeterWorkspace workspace =
                    new EmbeddedJMeterWorkspace(guiPackage, mainFrame, actionRouter);

            actionRouter.doActionNow(workspace.event(ActionNames.ADD_ALL));
            mainFrame.getTree().setSelectionRow(1);
            return workspace;
        } catch (RuntimeException | Error failure) {
            if (guiPackage != null) {
                try {
                    if (mainFrame != null) {
                        mainFrame.closeEmbedded();
                    } else {
                        guiPackage.unregisterAsListener();
                        MainFrame partialFrame = guiPackage.getMainFrame();
                        if (partialFrame != null) {
                            partialFrame.dispose();
                        }
                    }
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                } finally {
                    GuiPackage.disposeInstance(guiPackage);
                }
            }
            throw failure;
        }
    }

    /** @return JMeter's native tree/form editor surface. */
    public JComponent getComponent() {
        ensureOpen();
        return mainFrame.getEmbeddedComponent();
    }

    /** Set the currently visible host component used to parent native dialogs. */
    public void setDialogParent(Component parent) {
        ensureOpen();
        guiPackage.setDialogParent(parent);
    }

    /**
     * Replace the native model from an in-memory JMX document.
     * Parsing completes before the current model is mutated.
     *
     * @param inputStream current IDE document contents
     * @param sourcePath local path used by JMeter for relative resources
     * @throws IOException when the document cannot be read
     * @throws IllegalUserActionException when the parsed tree cannot be installed
     */
    public void load(InputStream inputStream, Path sourcePath)
            throws IOException, IllegalUserActionException {
        requireEventDispatchThread();
        ensureOpen();
        HashTree parsedTree = SaveService.loadTree(inputStream);
        Load.insertLoadedTree(EMBEDDED_ACTION_ID, parsedTree, false);
        guiPackage.setTestPlanFile(sourcePath.toAbsolutePath().normalize().toString());
        markSaved();
    }

    /**
     * Flush the visible form and serialize the complete test plan.
     *
     * @return UTF-8-compatible JMX bytes produced by JMeter's SaveService
     * @throws IOException when serialization fails
     */
    public byte[] snapshot() throws IOException {
        requireEventDispatchThread();
        ensureOpen();
        guiPackage.updateCurrentNode();
        HashTree tree = guiPackage.getTreeModel().getTestPlan();
        convertTreeNodes(tree);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            SaveService.saveTree(tree, output);
            return output.toByteArray();
        }
    }

    /** @return whether the native model differs from its saved baseline. */
    public boolean isDirty() {
        requireEventDispatchThread();
        ensureOpen();
        actionRouter.doActionNow(event(ActionNames.CHECK_DIRTY));
        return guiPackage.isDirty();
    }

    /** Record the current native model as the successfully persisted baseline. */
    public void markSaved() {
        requireEventDispatchThread();
        ensureOpen();
        guiPackage.updateCurrentNode();
        HashTree currentTree = guiPackage.getTreeModel().getTestPlan();
        actionRouter.doActionNow(new ActionEvent(
                currentTree,
                EMBEDDED_ACTION_ID,
                ActionNames.SUB_TREE_SAVED));
    }

    @Override
    public void close() {
        requireEventDispatchThread();
        if (closed) {
            return;
        }
        closed = true;
        guiPackage.setDialogParent(null);
        mainFrame.closeEmbedded();
        GuiPackage.disposeInstance(guiPackage);
    }

    private ActionEvent event(String actionName) {
        return new ActionEvent(this, EMBEDDED_ACTION_ID, actionName);
    }

    private static void convertTreeNodes(HashTree tree) {
        for (Object key : new ArrayList<>(tree.list())) {
            convertTreeNodes(tree.getTree(key));
            if (key instanceof JMeterTreeNode) {
                TestElement testElement = ((JMeterTreeNode) key).getTestElement();
                tree.replaceKey(key, testElement);
            }
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("The embedded JMeter workspace is closed");
        }
    }

    private static void requireEventDispatchThread() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Embedded JMeter workspace operations must run on the Swing EDT");
        }
    }
}
