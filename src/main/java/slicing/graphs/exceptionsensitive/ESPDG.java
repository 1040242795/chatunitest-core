package slicing.graphs.exceptionsensitive;

import slicing.arcs.pdg.ConditionalControlDependencyArc;
import slicing.graphs.augmented.PPDG;
import slicing.graphs.pdg.PDG;
import slicing.nodes.GraphNode;

/** An exception-sensitive PDG, equivalent to a PPDG that is built based on the {@link ESCFG},
 * and adding {@link ConditionalControlDependencyArc conditional control dependency arcs}. */
public class ESPDG extends PPDG {
    public ESPDG() {
        this(new ESCFG());
    }

    public ESPDG(ESCFG escfg) {
        super(escfg);
    }

    /** Add a conditional control dependency arc of type 1.
     * @see ConditionalControlDependencyArc.CC1 */
    public void addCC1Arc(GraphNode<?> src, GraphNode<?> dst) {
        addEdge(src, dst, new ConditionalControlDependencyArc.CC1());
    }

    /** Add a conditional control dependency arc of type 2.
     * @see ConditionalControlDependencyArc.CC2 */
    public void addCC2Arc(GraphNode<?> src, GraphNode<?> dst) {
        addEdge(src, dst, new ConditionalControlDependencyArc.CC2());
    }

    @Override
    protected PDG.Builder createBuilder() {
        return new Builder();
    }

    /** Builds an ESPDG, adding conditional control dependency arcs.
     * @see PPDG.Builder
     * @see ConditionalControlDependencyBuilder */
    public class Builder extends PPDG.Builder {
        protected Builder() {
            super();
        }

        @Override
        protected void buildControlDependency() {
            super.buildControlDependency();
            new ConditionalControlDependencyBuilder((ESCFG) cfg, ESPDG.this).build();
        }
    }
}
