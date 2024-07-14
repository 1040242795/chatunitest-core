package slicing.nodes.io;

import com.github.javaparser.ast.body.CallableDeclaration;
import slicing.nodes.SyntheticNode;

import java.util.LinkedList;

/** A node representing the returned value from a declaration. */
public class MethodExitNode extends SyntheticNode<CallableDeclaration<?>> {
    public MethodExitNode(CallableDeclaration<?> astNode) {
        super("Exit", astNode, new LinkedList<>());
    }
}
