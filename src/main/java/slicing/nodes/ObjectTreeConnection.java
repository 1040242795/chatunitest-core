package slicing.nodes;

import slicing.arcs.Arc;
import slicing.arcs.pdg.FlowDependencyArc;
import slicing.arcs.pdg.ObjectFlowDependencyArc;
import slicing.arcs.sdg.ParameterInOutArc;
import slicing.graphs.Graph;
import slicing.graphs.jsysdg.JSysDG;
import slicing.graphs.jsysdg.JSysPDG;
import slicing.nodes.oo.MemberNode;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

/** A connection between two object trees. This object can specify the connection between two different
 *  levels of object trees, for example to represent the assignment {@code a.b = c.d}. */
class ObjectTreeConnection implements VariableAction.PDGConnection {

    protected final VariableAction sourceAction;
    protected final VariableAction targetAction;
    protected final String sourceMember;
    protected final String targetMember;

    protected boolean applied = false;

    public ObjectTreeConnection(VariableAction sourceAction, VariableAction targetAction, String sourceMember, String targetMember) {
        this.sourceAction = sourceAction;
        this.targetAction = targetAction;
        this.sourceMember = sourceMember;
        this.targetMember = targetMember;
    }

    /** Apply the connection represented by this object on an SDG. This means that all arcs will be interprocedural. */
    public void applySDG(JSysDG graph) {
        if (!applied) {
            connectTrees(graph, ParameterInOutArc::new, ParameterInOutArc.ObjectFlow::new);
            applied = true;
        }
    }

    @Override
    public void apply(JSysPDG graph) {
        if (!applied)
            connectTrees(graph, FlowDependencyArc::new, ObjectFlowDependencyArc::new);
    }

    protected void connectTrees(Graph graph, Supplier<Arc> flowSupplier, Supplier<Arc> objFlowSupplier) {
        Collection<ObjectTree> source = Collections.singleton(null);
        Collection<ObjectTree> target = Collections.singleton(null);
        assert sourceMember.isEmpty() || sourceAction.hasObjectTree();
        assert targetMember.isEmpty() || targetAction.hasObjectTree();
        if (sourceAction.hasObjectTree())
            source = sourceAction.getObjectTree().findObjectTreeOfPolyMember(sourceMember);
        if (targetAction.hasObjectTree())
            target = targetAction.getObjectTree().findObjectTreeOfPolyMember(targetMember);
        for (ObjectTree treeSrc : source)
            for (ObjectTree treeTgt : target)
                connectOT(treeSrc, treeTgt,
                        treeSrc != null ? treeSrc.getMemberNode() : sourceAction.getGraphNode(),
                        treeTgt != null ? treeTgt.getMemberNode() : targetAction.getGraphNode(),
                        graph, flowSupplier, objFlowSupplier);
    }

    private void connectOT(ObjectTree source, ObjectTree target, GraphNode<?> rootSrc, GraphNode<?> rootTgt,
                           Graph graph, Supplier<Arc> flowSupplier, Supplier<Arc> objFlowSupplier) {
        if (source == null || target == null) {
            if (!rootSrc.equals(rootTgt)) {
                graph.addEdge(rootSrc, rootTgt, flowSupplier.get());
                applied = true;
            }
        } else {
            applied = true;
            graph.addEdge(rootSrc, rootTgt, objFlowSupplier.get());
            graph.addEdge(rootSrc, rootTgt, flowSupplier.get());
            for (String treeMember : target.nameIterable()) {
                if (!source.hasMember(treeMember))
                    continue;
                MemberNode src = source.getNodeFor(treeMember);
                MemberNode tgt = target.getNodeFor(treeMember);
                if (target.hasChildren(treeMember))
                    graph.addEdge(src, tgt, objFlowSupplier.get());
                graph.addEdge(src, tgt, flowSupplier.get());
            }
        }
    }
}
