package slicing.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.visitor.CloneVisitor;
import slicing.nodes.GraphNode;
import slicing.utils.ASTUtils;
import slicing.utils.NodeHashSet;

import java.util.*;

/** The representation of a slice, or a subset of a graph's nodes.
 *  A slice can be obtained from any {@link Sliceable} object, and converted
 *  to code with {@link #toAst()}. */
public class Slice {
    /** Nodes contained in this slice, mapped by id. */
    private final Map<Long, GraphNode<?>> map = new HashMap<>();
    /** The nodes that conform the slicing criterion. */
    private final Set<GraphNode<?>> immutableSC;

    public Slice(Set<GraphNode<?>> slicingCriterion) {
        immutableSC = Set.copyOf(slicingCriterion);
        addAll(slicingCriterion);
    }

    public Set<GraphNode<?>> getCriterion() {
        return immutableSC;
    }

    /** Add a node to this slice. */
    public void add(GraphNode<?> node) {
        assert !map.containsKey(node.getId());
        map.put(node.getId(), node);
    }

    /** Add multiple nodes to this slice. */
    public void addAll(Collection<GraphNode<?>> nodes) {
        nodes.forEach(this::add);
    }

    /** Whether the slice contains the given node. */
    public boolean contains(GraphNode<?> node) {
        return map.containsKey(node.getId());
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Slice && map.equals(((Slice) obj).map);
    }

    /** Obtain the nodes from this slice. */
    public Set<GraphNode<?>> getGraphNodes() {
        return Set.copyOf(map.values());
    }

    /** Organize all nodes pertaining to this slice in one or more CompilationUnits. CompilationUnits
     *  themselves need not be part of the slice to be included if any of their components are present. */
    public NodeList<CompilationUnit> toAst() {
        Map<CompilationUnit, NodeHashSet<Node>> cuMap = ASTUtils.newIdentityHashMap();
        // Add each node to the corresponding bucket of the map
        // Nodes may not belong to a compilation unit (fictional nodes), and they are skipped for the slice.
        for (GraphNode<?> graphNode : map.values()) {
            if (graphNode.isImplicitInstruction() || graphNode.getAstNode() == null)
                continue;
            Optional<CompilationUnit> cu = graphNode.getAstNode().findCompilationUnit();
            if (cu.isEmpty()) continue;
            cuMap.computeIfAbsent(cu.get(), compilationUnit -> new NodeHashSet<>());
            cuMap.get(cu.get()).add(graphNode.getAstNode());
        }
        // Traverse the AST of each compilation unit, creating a copy and
        // removing any element not present in the slice.
        NodeList<CompilationUnit> cus = new NodeList<>();
        SlicePruneVisitor sliceVisitor = new SlicePruneVisitor();
        CloneVisitor cloneVisitor = new CloneVisitor();
        for (Map.Entry<CompilationUnit, NodeHashSet<Node>> entry : cuMap.entrySet()) {
            CompilationUnit clone = (CompilationUnit) entry.getKey().accept(cloneVisitor, null);
            if (entry.getKey().getStorage().isPresent())
                clone.setStorage(entry.getKey().getStorage().get().getPath(),
                        entry.getKey().getStorage().get().getEncoding());
            clone.accept(sliceVisitor, entry.getValue());
            clone.removePackageDeclaration();
            clone.getImports().clear();
            cus.add(clone);
        }
        return cus;
    }
}
