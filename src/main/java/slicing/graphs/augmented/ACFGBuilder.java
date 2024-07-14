package slicing.graphs.augmented;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.stmt.*;
import slicing.graphs.cfg.CFGBuilder;
import slicing.nodes.GraphNode;
import slicing.utils.ASTUtils;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/** Populates a {@link ACFG}, given one and an AST root node.
 *  @see CFGBuilder Parent class for more instructions. */
public class ACFGBuilder extends CFGBuilder {
    /** Same as {@link #hangingNodes}, but to be connected as non-executable edges. */
    protected final List<GraphNode<?>> nonExecHangingNodes = new LinkedList<>();
    /** Stack of non-executable hanging nodes for temporary storage. */
    protected final Deque<List<GraphNode<?>>> nonExecHangingNodesStack = new LinkedList<>();

    protected ACFGBuilder(ACFG graph) {
        super(graph);
    }

    @Override
    protected void connectTo(GraphNode<?> node) {
        for (GraphNode<?> src : nonExecHangingNodes)
            ((ACFG) graph).addNonExecutableControlFlowArc(src, node);
        super.connectTo(node);
    }

    @Override
    protected void clearHanging() {
        super.clearHanging();
        nonExecHangingNodes.clear();
    }

    @Override
    protected void saveHanging() {
        nonExecHangingNodesStack.push(List.copyOf(nonExecHangingNodes));
        super.saveHanging();
    }

    @Override
    protected void restoreHanging() {
        nonExecHangingNodes.addAll(nonExecHangingNodesStack.pop());
        super.restoreHanging();
    }

    @Override
    protected void dropHanging() {
        nonExecHangingNodesStack.pop();
        super.dropHanging();
    }

    // ======================================================================
    // ========================== Normal AST nodes ==========================
    // ======================================================================

    @Override
    protected boolean isEmptyLoop(GraphNode<?> condition) {
        return super.isEmptyLoop(condition) && nonExecHangingNodes.isEmpty();
    }

    @Override
    public void visit(SwitchStmt switchStmt, Void arg) {
        // Link previous statement to the switch's selector
        switchEntriesStack.push(new LinkedList<>());
        breakStack.push(new LinkedList<>());
        GraphNode<?> cond = connectTo(switchStmt, String.format("switch (%s)", switchStmt.getSelector()));
        switchStmt.getSelector().accept(this, arg);
        // expr --> each case (fallthrough by default, so case --> case too)
        for (SwitchEntry entry : switchStmt.getEntries()) {
            entry.accept(this, arg); // expr && prev case --> case --> next case
            hangingNodes.add(cond); // expr --> next case
        }
        // The next statement will be linked to:
        //		1. All break statements that broke from the switch (done with break section)
        // 		2. If the switch doesn't have a default statement, the switch's selector (already present)
        // 		3. If the last entry doesn't break, to the last statement (present already)
        // If the last case is a default case, remove the selector node from the list of nodes (see 2)
        if (ASTUtils.switchHasDefaultCase(switchStmt))
            hangingNodes.remove(cond);
        List<GraphNode<SwitchEntry>> entries = switchEntriesStack.pop();
        GraphNode<SwitchEntry> def = null;
        for (GraphNode<SwitchEntry> entry : entries) {
            if (entry.getAstNode().getLabels().isEmpty()) {
                def = entry;
                break;
            }
        }
        if (def != null) {
            saveHanging();
            clearHanging();
            entries.remove(def);
            nonExecHangingNodes.addAll(entries);
            connectTo(def);
            clearHanging();
            restoreHanging();
            nonExecHangingNodes.add(def);
        } else {
            nonExecHangingNodes.addAll(entries);
        }
        // End block and break section
        hangingNodes.addAll(breakStack.pop());
    }

    @Override
    public void visit(BreakStmt breakStmt, Void arg) {
        GraphNode<BreakStmt> node = connectTo(breakStmt);
        if (breakStmt.getLabel().isPresent())
            breakMap.get(breakStmt.getLabel().get()).add(node);
        else
            breakStack.peek().add(node);
        clearHanging();
        nonExecHangingNodes.add(node); // NEW vs CFG
    }

    @Override
    public void visit(ContinueStmt continueStmt, Void arg) {
        GraphNode<ContinueStmt> node = connectTo(continueStmt);
        if (continueStmt.getLabel().isPresent())
            continueMap.get(continueStmt.getLabel().get()).add(node);
        else
            continueStack.peek().add(node);
        clearHanging();
        nonExecHangingNodes.add(node); // NEW vs CFG
    }

    @Override
    public void visit(ReturnStmt returnStmt, Void arg) {
        GraphNode<ReturnStmt> node = connectTo(returnStmt);
        returnList.add(node);
        clearHanging();
        nonExecHangingNodes.add(node); // NEW vs CFG
    }

    // ======================================================================
    // ============================ Declarations ============================
    // ======================================================================

    @Override
    protected void buildExit(CallableDeclaration<?> callableDeclaration) {
        nonExecHangingNodes.add(graph.getRootNode());
        super.buildExit(callableDeclaration);
    }
}
