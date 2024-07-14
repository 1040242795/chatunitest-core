package slicing.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import slicing.graphs.cfg.CFG;
import slicing.graphs.pdg.PDG;
import slicing.graphs.sdg.SDG;
import slicing.utils.ASTUtils;

import java.util.*;

import static slicing.graphs.exceptionsensitive.ESCFG.ACTIVE_EXCEPTION_VARIABLE;

/**
 * Represents a node in the various graphs ({@link CFG CFG}, {@link PDG PDG} and {@link SDG SDG}),
 * including its AST representation and the connections it has to other nodes in the same graph.
 * It can hold a string of characters that will be used to represent it. <br/>
 * It is immutable.
 * @param <N> The type of the AST represented by this node.
 */
public class GraphNode<N extends Node> implements Comparable<GraphNode<?>> {
    /** A unique id within the graph. */
    protected final long id;
    /** The textual representation of the node. */
    protected final String label;
    /** The JavaParser AST node represented by this node. */
    protected final N astNode;
    /** A sorted list of actions (usages, definitions and declarations) performed in this node. */
    protected final List<VariableAction> variableActions;
    /** The method calls contained  */
    protected final List<Resolvable<? extends ResolvedMethodLikeDeclaration>> methodCalls = new LinkedList<>();
    /** Nodes that are generated as a result of the instruction represented by this GraphNode and that may
     *  be included in Movable actions. */
    protected final Set<SyntheticNode<?>> syntheticNodesInMovables = new HashSet<>();

    /** @see #isImplicitInstruction() */
    protected boolean isImplicit = false;

    /** Create a graph node, with id and variable actions generated automatically. */
    public GraphNode(String label, N astNode) {
        this(IdHelper.getInstance().getNextId(), label, astNode);
    }

    /** Create a graph node, with variable actions generated automatically. */
    protected GraphNode(long id, String label, N astNode) {
        this(id, label, astNode, new LinkedList<>());
        extractVariables();
    }

    /** Create a graph node, with id generated automatically. */
    public GraphNode(String label, N astNode, List<VariableAction> variableActions) {
        this(IdHelper.getInstance().getNextId(), label, astNode, variableActions);
    }

    protected GraphNode(long id, String label, N astNode, List<VariableAction> variableActions) {
        this.id = id;
        this.label = label;
        this.astNode = astNode;
        this.variableActions = variableActions;
    }

    /** Search for all the declarations, definitions and usages in this node. */
    protected void extractVariables() {
        new VariableVisitor().startVisit(this);
    }

    /** A unique id in this graph. */
    public long getId() {
        return id;
    }

    /** The AST node represented by this graph. */
    public N getAstNode() {
        return astNode;
    }

    /** An unmodifiable list of variable actions in this node. */
    public List<VariableAction> getVariableActions() {
        return Collections.unmodifiableList(variableActions);
    }

    /** Returns the last variable action in this node.
     *  @throws IllegalStateException If there are no variable actions. */
    public VariableAction getLastVariableAction() {
        if (variableActions.isEmpty())
            throw new IllegalStateException("There are no variable actions in this node");
        return variableActions.get(variableActions.size() - 1);
    }

    /** The node's label. It represents the portion of the node that
     *  is covered by this node, in the case of block statements. */
    public String getLabel() {
        return label;
    }

    /** The node's long-form label, including its id and information on variables. */
    public String getLongLabel() {
        String label = getId() + ": " + getLabel().replace("\\", "\\\\");
        if (!getVariableActions().isEmpty())
            label += "\\n" + getVariableActions().stream().map(Object::toString).reduce((a, b) -> a + "," + b).orElse("--");
        return label;
    }

    /** Marks the current node as implicit.
     *  @see #isImplicitInstruction() */
    public void markAsImplicit() {
        this.isImplicit = true;
        variableActions.stream()
                .filter(VariableAction.Movable.class::isInstance)
                .map(VariableAction.Movable.class::cast)
                .map(VariableAction.Movable::getRealNode)
                .forEach(GraphNode::markAsImplicit);
    }

    /** Whether this graph node represents an AST node that didn't exist explicitly, such as 'super()'. */
    public boolean isImplicitInstruction() {
        return isImplicit;
    }

    // =============================================================
    // ===================  Variables and Calls  ===================
    // =============================================================

    /** Whether this node contains the given call AST node. */
    public boolean containsCall(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        for (Resolvable<? extends ResolvedMethodLikeDeclaration> c : methodCalls)
            if (c == call || ASTUtils.equalsWithRange((Node) c, (Node) call))
                return true;
        return false;
    }

    /** Append or prepend the given set of actions to the actions of the given call. */
    public void addActionsForCall(List<VariableAction.Movable> actions, Resolvable<? extends ResolvedMethodLikeDeclaration> call, boolean prepend) {
        for (int i = 0; i < variableActions.size(); i++) {
            VariableAction var = variableActions.get(i);
            if (var instanceof VariableAction.CallMarker) {
                VariableAction.CallMarker marker = (VariableAction.CallMarker) var;
                if (marker.getCall().equals(call) && marker.isEnter() == prepend) {
                    variableActions.addAll(prepend ? i + 1 : i, actions);
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Could not find markers for " + call.resolve().getSignature() + " in " + this);
    }

    /** Register a node that is contained in this node until the CFG
     *  is converted into the PDG. */
    public void addSyntheticNode(SyntheticNode<?> node) {
        syntheticNodesInMovables.add(node);
    }

    /** @see #syntheticNodesInMovables */
    public Collection<SyntheticNode<?>> getSyntheticNodesInMovables() {
        return Collections.unmodifiableSet(syntheticNodesInMovables);
    }

    /** Append a variable action to the list of variable actions. When the action
     *  is movable, its real node is registered in {@link #syntheticNodesInMovables}. */
    public void addVariableAction(VariableAction action) {
        if (action instanceof VariableAction.Movable)
            syntheticNodesInMovables.add(((VariableAction.Movable) action).getRealNode());
        variableActions.add(action);
    }

    /**
     * Searches for the last variable action that matches the given real node, and appends
     * the given action immediately after.
     */
    @SuppressWarnings("unchecked")
    public void addVariableActionAfterLastMatchingRealNode(VariableAction.Movable action, SyntheticNode<?> realNode) {
        boolean found = false;
        for (int i = 0; i < variableActions.size(); i++) {
            VariableAction a = variableActions.get(i);
            if (a instanceof VariableAction.Movable
                    && ((VariableAction.Movable) a).getRealNode() == realNode) {
                found = true;
            } else if (found) {
                // The previous one matched, this one does not. Add before this one.
                variableActions.add(i, action);
                return;
            }
        }
        // If the last one matched, add to the end
        if (found)
            variableActions.add(action);
        else {
            assert syntheticNodesInMovables.contains(realNode);
            addActionsForCall(List.of(action), (Resolvable<? extends ResolvedMethodLikeDeclaration>) realNode.getAstNode(), true);
        }
    }

    /** Adds the variable action DEF(-active-exception-) to the end of this method. */
    public void addVADefineActiveException(Expression expression) {
        variableActions.add(new VariableAction.Definition(VariableAction.DeclarationType.SYNTHETIC, ACTIVE_EXCEPTION_VARIABLE, this, expression));
    }

    /** Adds the variable action USE(-active-exception-) to the end of this method. */
    public void addVAUseActiveException() {
        variableActions.add(new VariableAction.Usage(VariableAction.DeclarationType.SYNTHETIC, ACTIVE_EXCEPTION_VARIABLE, this));
    }

    /** Create and append a call marker to the list of actions of this node. */
    public void addCallMarker(Resolvable<? extends ResolvedMethodLikeDeclaration> call, boolean enter) {
        if (enter) methodCalls.add(call);
        variableActions.add(new VariableAction.CallMarker(call, this, enter));
    }

    // ============================================================
    // =======================  Overridden  =======================
    // ============================================================

    @Override
    public String toString() {
        return String.format("%s{id: %s, label: '%s', astNodeType: %s}",
                getClass().getSimpleName(),
                getId(),
                getLabel(),
                getAstNode().getClass().getSimpleName()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        return o instanceof GraphNode && Objects.equals(getId(), ((GraphNode<?>) o).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public int compareTo(GraphNode<?> o) {
        return Long.compare(id, o.id);
    }
}
