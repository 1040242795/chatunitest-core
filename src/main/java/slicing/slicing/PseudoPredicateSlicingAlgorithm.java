package slicing.slicing;

import slicing.arcs.Arc;
import slicing.graphs.augmented.PSDG;
import slicing.nodes.GraphNode;

import java.util.Set;

public class PseudoPredicateSlicingAlgorithm extends ClassicSlicingAlgorithm {
    protected Set<GraphNode<?>> slicingCriterion;

    public PseudoPredicateSlicingAlgorithm(PSDG graph) {
        super(graph);
    }

    @Override
    public Slice traverseProcedure(GraphNode<?> slicingCriterion) {
        this.slicingCriterion = Set.of(slicingCriterion);
        return super.traverseProcedure(slicingCriterion);
    }

    @Override
    public Slice traverse(Set<GraphNode<?>> slicingCriterion) {
        this.slicingCriterion = slicingCriterion;
        return super.traverse(slicingCriterion);
    }

    @Override
    protected boolean ignorePass1(Arc arc) {
        return super.ignorePass1(arc) || ignorePseudoPredicate(arc);
    }

    @Override
    protected boolean ignorePass2(Arc arc) {
        return super.ignorePass2(arc) || ignorePseudoPredicate(arc);
    }

    @Override
    public boolean ignoreProcedure(Arc arc) {
        return super.ignoreProcedure(arc) || ignorePseudoPredicate(arc);
    }

    protected boolean ignorePseudoPredicate(Arc arc) {
        GraphNode<?> target = graph.getEdgeTarget(arc);
        return ((PSDG) graph).isPseudoPredicate(target)
                && arc.isControlDependencyArc()
                && !slicingCriterion.contains(target);
    }
}
