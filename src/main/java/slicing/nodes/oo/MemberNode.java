package slicing.nodes.oo;

import com.github.javaparser.ast.Node;
import slicing.nodes.GraphNode;
import slicing.nodes.SyntheticNode;

import java.util.LinkedList;

/** A synthetic node that represents an object or field that is within a
 *  VariableAction. They are placed in the graph when the PDG is built,
 *  and allow for a more granular representation and slicing of objects. */
public class MemberNode extends SyntheticNode<Node> {
    protected GraphNode<?> parent;

    public MemberNode(String instruction, GraphNode<?> parent) {
        this(instruction, null, parent);
    }

    public MemberNode(String instruction, Node astNode, GraphNode<?> parent) {
        super(instruction, astNode, new LinkedList<>());
        this.parent = parent;
    }

    public GraphNode<?> getParent() {
        return parent;
    }

    public void setParent(GraphNode<?> parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return String.format("%s{id: %s, label: '%s'}",
                getClass().getSimpleName(),
                getId(),
                getLabel()
        );
    }

    /** Create a copy of this node with the given argument as its parent node. */
    public MemberNode copyToParent(GraphNode<?> parent) {
        return new MemberNode(label, parent);
    }
}
