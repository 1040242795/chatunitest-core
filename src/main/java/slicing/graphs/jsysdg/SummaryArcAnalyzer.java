package slicing.graphs.jsysdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import slicing.arcs.Arc;
import slicing.graphs.CallGraph;
import slicing.graphs.sdg.AbstractSummaryArcAnalyzer;
import slicing.nodes.GraphNode;
import slicing.nodes.SyntheticNode;
import slicing.nodes.VariableAction;
import slicing.nodes.io.FormalIONode;
import slicing.nodes.oo.MemberNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates and places on the graph all summary arcs between actual-in and actual-out, return and exception/normal
 * return nodes. Additionally, it generates them between the object trees of each of the aforementioned nodes.
 */
public class SummaryArcAnalyzer extends AbstractSummaryArcAnalyzer<SyntheticNode<?>, SyntheticNode<?>, SyntheticNode<?>> {
    public SummaryArcAnalyzer(JSysDG sdg, CallGraph graph) {
        super(sdg, graph);
    }

    @Override
    protected Set<SyntheticNode<?>> getFormalOutNodes(CallableDeclaration<?> declaration) {
        Set<SyntheticNode<?>> set = super.getFormalOutNodes(declaration);
        for (var node : Set.copyOf(set)) {
            if (node.getVariableActions().isEmpty())
                continue;
            assert node.getVariableActions().size() == 1;
            VariableAction action = node.getVariableActions().get(0);
            if (action.hasObjectTree())
                set.addAll(action.getObjectTree().leaves());
        }
        return set;
    }

    @Override
    protected Set<SyntheticNode<?>> computeFormalIn(SyntheticNode<?> formalOut) {
        Set<SyntheticNode<?>> result = new HashSet<>();
        for (GraphNode<?> graphNode : ((JSysDG) sdg).createSlicingAlgorithm().traverseProcedure(formalOut).getGraphNodes())
            if (isFormalIn(graphNode) && graphNode instanceof SyntheticNode)
                result.add((SyntheticNode<?>) graphNode);
        return result;
    }

    @Override
    protected Collection<? extends SyntheticNode<?>> findActualIn(CallGraph.Edge<?> edge, SyntheticNode<?> formalIn) {
        return sdg.incomingEdgesOf(formalIn).stream()
                .filter(Arc::isInterproceduralInputArc)
                .map(sdg::getEdgeSource)
                .filter(actualIn -> goToParent(actualIn).getAstNode() == edge.getCall())
                .map(node -> (SyntheticNode<?>) node)
                .collect(Collectors.toSet());
    }

    @Override
    protected Collection<? extends SyntheticNode<?>> findOutputNode(CallGraph.Edge<?> edge, SyntheticNode<?> formalOut) {
        return sdg.outgoingEdgesOf(formalOut).stream()
                .filter(Arc::isInterproceduralOutputArc)
                .map(sdg::getEdgeTarget)
                .filter(actualOut -> goToParent(actualOut).getAstNode() == edge.getCall())
                .map(node -> (SyntheticNode<?>) node)
                .collect(Collectors.toSet());
    }

    private boolean isFormalIn(GraphNode<?> graphNode) {
        GraphNode<?> parent = goToParent(graphNode);
        return parent instanceof FormalIONode && ((FormalIONode) parent).isInput();
    }

    private GraphNode<?> goToParent(GraphNode<?> memberNode) {
        if (memberNode instanceof MemberNode)
            return goToParent(((MemberNode) memberNode).getParent());
        return memberNode;
    }
}
