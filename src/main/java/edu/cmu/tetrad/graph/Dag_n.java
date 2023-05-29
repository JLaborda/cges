///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015, 2022 by Peter Spirtes, Richard        //
// Scheines, Joseph Ramsey, and Clark Glymour.                               //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.graph;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

/**
 * Represents a directed acyclic graph--that is, a graph containing only
 * directed edges, with no cycles. Variables are permitted to be either measured
 * or latent, with at most one edge per node pair, and no edges to self.
 *
 * @author Joseph Ramsey
 */
public final class Dag_n implements Graph {
    static final long serialVersionUID = 23L;

    /**
     * The wrapped graph.
     */
    private final Graph graph;

    /**
     * A dpath matrix for the DAG. If used, it is updated (where necessary) each
     * time the getDirectedPath method is called with whatever edges are stored
     * in the dpathNewEdges list. New edges that are added are appended to the
     * dpathNewEdges list. When edges are removed and when nodes are added or
     * removed, dpath is set to null.
     */
    private transient byte[][] dpath;

    /**
     * New edges that need to be added to the dpath matrix.
     */
    private transient LinkedList<Edge> dpathNewEdges = new LinkedList<>();

    /**
     * The order of nodes used for dpath.
     */
    private int size;

    private Map<Node, Integer> nodesHash = new HashMap<>();
    int n = 0;

    private boolean pag;
    private boolean CPDAG;

    private final Map<String, Object> attributes = new HashMap<>();

    //===============================CONSTRUCTORS=======================//

    /**
     * Constructs a new directed acyclic graph (DAG).
     */
    public Dag_n() {

        // Must use EdgeListGraph because property change events are correctly implemeted. Don't change it!
        // unless you fix that or the interface will break the interface! jdramsey 2015-6-5
        this.graph = new EdgeListGraph();

        reconstituteDpath();
    }

    public Dag_n(List<Node> nodes) {
        this.graph = new EdgeListGraph(nodes);
        reconstituteDpath();
    }

    /**
     * Constructs a new directed acyclic graph from the given graph object.
     *
     * @param graph the graph to base the new DAG on.
     * @throws IllegalArgumentException if the given graph cannot for some
     *                                  reason be converted into a DAG.
     */
    public Dag_n(Graph graph) throws IllegalArgumentException {
        if (graph.existsDirectedCycle()) {
            throw new IllegalArgumentException("That graph was not acyclic.");
        }

        this.graph = new EdgeListGraph();

        transferNodesAndEdges(graph);

        for (Node node : this.graph.getNodes()) {
            node.getAllAttributes().clear();
        }

        resetDPath();
        reconstituteDpath();

        for (Edge edge : graph.getEdges()) {
            if (graph.isHighlighted(edge)) {
                setHighlighted(edge, true);
            }
        }
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     */
    public static Dag_n serializableInstance() {
        Dag_n dag = new Dag_n();
        GraphNode node1 = new GraphNode("X");
        dag.addNode(node1);
        return dag;
    }

    //===============================PUBLIC METHODS======================//

    public boolean addBidirectedEdge(Node node1, Node node2) {
        throw new UnsupportedOperationException();
    }

    public boolean addEdge(Edge edge) {
        reconstituteDpath();
        Node _node1 = Edges.getDirectedEdgeTail(edge);
        Node _node2 = Edges.getDirectedEdgeHead(edge);

        int _index1 = this.nodesHash.get(_node1);
        int _index2 = this.nodesHash.get(_node2);

        if (dpath[_index2][_index1] == 1) {
            return false;
        }

        boolean added = getGraph().addEdge(edge);

        if (added) {
            dpathNewEdges().add(edge);
        }

        return added;
    }

    public boolean addDirectedEdge(Node node1, Node node2) {
        return addEdge(Edges.directedEdge(node1, node2));
    }

    public boolean addPartiallyOrientedEdge(Node node1, Node node2) {
        throw new UnsupportedOperationException();
    }

    public boolean addNode(Node node) {
        boolean added = getGraph().addNode(node);

        if (added) {
            resetDPath();
            reconstituteDpath();
        }

        return added;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        getGraph().addPropertyChangeListener(l);
    }

    public boolean addUndirectedEdge(Node node1, Node node2) {
        throw new UnsupportedOperationException();
    }

    public boolean addNondirectedEdge(Node node1, Node node2) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        getGraph().clear();
    }

    public boolean containsEdge(Edge edge) {
        return getGraph().containsEdge(edge);
    }

    public boolean containsNode(Node node) {
        return getGraph().containsNode(node);
    }

    public boolean defNonDescendent(Node node1, Node node2) {
        return getGraph().defNonDescendent(node1, node2);
    }

    public boolean existsDirectedCycle() {
        return false;
    }

    public boolean defVisible(Edge edge) {
        return getGraph().defVisible(edge);
    }

    public boolean isDefNoncollider(Node node1, Node node2, Node node3) {
        return getGraph().isDefNoncollider(node1, node2, node3);
    }

    public boolean isDefCollider(Node node1, Node node2, Node node3) {
        return getGraph().isDefCollider(node1, node2, node3);
    }

    public boolean existsTrek(Node node1, Node node2) {
        return getGraph().existsTrek(node1, node2);
    }

    public boolean equals(Object o) {
        return o instanceof Dag_n && getGraph().equals(o);
    }

    public boolean existsDirectedPathFromTo(Node node1, Node node2) {


        int index1 = this.nodesHash.get(node1);
        int index2 = this.nodesHash.get(node2);

        return this.dpath[index1][index2] == 1;
    }

    @Override
    public List<Node> findCycle() {
        return getGraph().findCycle();
    }

    public boolean existsUndirectedPathFromTo(Node node1, Node node2) {
        return false;
    }


    public boolean existsSemiDirectedPathFromTo(Node node1, Set<Node> nodes) {
        return getGraph().existsSemiDirectedPathFromTo(node1, nodes);
    }

    public boolean existsInducingPath(Node node1, Node node2) {
        return getGraph().existsInducingPath(node1, node2);
    }

    public void fullyConnect(Endpoint endpoint) {
        throw new UnsupportedOperationException();
        //graph.fullyConnect(endpoint);
    }

    public Endpoint getEndpoint(Node node1, Node node2) {
        return getGraph().getEndpoint(node1, node2);
    }

    public Endpoint[][] getEndpointMatrix() {
        return getGraph().getEndpointMatrix();
    }

    public List<Node> getAdjacentNodes(Node node) {
        return getGraph().getAdjacentNodes(node);
    }

    public List<Node> getNodesInTo(Node node, Endpoint endpoint) {
        return getGraph().getNodesInTo(node, endpoint);
    }

    public List<Node> getNodesOutTo(Node node, Endpoint n) {
        return getGraph().getNodesOutTo(node, n);
    }

    public List<Node> getNodes() {
        return getGraph().getNodes();
    }

    public Set<Edge> getEdges() {
        return getGraph().getEdges();
    }

    public List<Edge> getEdges(Node node) {
        return getGraph().getEdges(node);
    }

    public List<Edge> getEdges(Node node1, Node node2) {
        return getGraph().getEdges(node1, node2);
    }

    public Node getNode(String name) {
        return getGraph().getNode(name);
    }

    public int getNumEdges() {
        return getGraph().getNumEdges();
    }

    public int getNumNodes() {
        return getGraph().getNumNodes();
    }

    public int getNumEdges(Node node) {
        return getGraph().getNumEdges(node);
    }

    public List<Node> getChildren(Node node) {
        return getGraph().getChildren(node);
    }

    public int getConnectivity() {
        return getGraph().getConnectivity();
    }

    public List<Node> getDescendants(List<Node> nodes) {
        return getGraph().getDescendants(nodes);
    }

    public Edge getEdge(Node node1, Node node2) {
        return getGraph().getEdge(node1, node2);
    }

    public Edge getDirectedEdge(Node node1, Node node2) {
        return getGraph().getDirectedEdge(node1, node2);
    }

    public List<Node> getParents(Node node) {
        return getGraph().getParents(node);
    }

    public int getIndegree(Node node) {
        return getGraph().getIndegree(node);
    }

    @Override
    public int getDegree(Node node) {
        return getGraph().getDegree(node);
    }

    public int getOutdegree(Node node) {
        return getGraph().getOutdegree(node);
    }

    /**
     * This method returns the nodes of a digraph in such an order that as one
     * iterates through the list, the parents of each node have already been
     * encountered in the list.
     *
     * @return a tier ordering for the nodes in this graph.
     */
    public List<Node> getCausalOrdering() {
        return GraphUtils.getCausalOrdering(this, this.getNodes());
    }

    public void setHighlighted(Edge edge, boolean highlighted) {
        getGraph().setHighlighted(edge, highlighted);
    }

    public boolean isHighlighted(Edge edge) {
        return getGraph().isHighlighted(edge);
    }

    public boolean isParameterizable(Node node) {
        return getGraph().isParameterizable(node);
    }

    public boolean isTimeLagModel() {
        return getGraph().isTimeLagModel();
    }

    public TimeLagGraph getTimeLagGraph() {
        return getGraph().getTimeLagGraph();
    }

    @Override
    public void removeTriplesNotInGraph() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Node> getSepset(Node n1, Node n2) {
//        return graph.getSepset(n1, n2);
        return GraphUtils.getSepset(n1, n2, this);
    }

    @Override
    public void setNodes(List<Node> nodes) {
        this.graph.setNodes(nodes);
    }

    public boolean isAdjacentTo(Node nodeX, Node nodeY) {
        return getGraph().isAdjacentTo(nodeX, nodeY);
    }

    public boolean isAncestorOf(Node node1, Node node2) {
        return node1 == node2 || GraphUtils.existsDirectedPathFromTo(node1, node2, this);
    }

    public boolean isDirectedFromTo(Node node1, Node node2) {
        return getGraph().isDirectedFromTo(node1, node2);
    }

    public boolean isUndirectedFromTo(Node node1, Node node2) {
        return false;
    }

    public boolean isParentOf(Node node1, Node node2) {
        return getGraph().isParentOf(node1, node2);
    }

    public boolean isProperAncestorOf(Node node1, Node node2) {
        return node1 != node2 && isAncestorOf(node1, node2);
    }

    public boolean isProperDescendentOf(Node node1, Node node2) {
        return node1 != node2 && isDescendentOf(node1, node2);
    }

    public boolean isExogenous(Node node) {
        return getGraph().isExogenous(node);
    }

    public boolean isDConnectedTo(Node node1, Node node2,
                                  List<Node> conditioningNodes) {
        return getGraph().isDConnectedTo(node1, node2, conditioningNodes);
    }

    public boolean isDSeparatedFrom(Node node1, Node node2, List<Node> z) {
        return getGraph().isDSeparatedFrom(node1, node2, z);
    }

    public boolean isChildOf(Node node1, Node node2) {
        return getGraph().isChildOf(node1, node2);
    }

    public boolean isDescendentOf(Node node1, Node node2) {
        return node1 == node2 || GraphUtils.existsDirectedPathFromTo(node2, node1, this);
    }

    public boolean removeEdge(Node node1, Node node2) {
        boolean removed = getGraph().removeEdge(node1, node2);

        if (removed) {
            resetDPath();
            reconstituteDpath();
        }

        return removed;
    }

    public boolean removeEdges(Node node1, Node node2) {
        boolean removed = getGraph().removeEdges(node1, node2);

        if (removed) {
            resetDPath();
            reconstituteDpath();
        }

        return removed;
    }

    public boolean setEndpoint(Node node1, Node node2, Endpoint endpoint) {
        boolean ret = getGraph().setEndpoint(node1, node2, endpoint);

        resetDPath();
        reconstituteDpath();

        return ret;
    }

    public Graph subgraph(List<Node> nodes) {
        return getGraph().subgraph(nodes);
    }

    public boolean removeEdge(Edge edge) {
        boolean removed = getGraph().removeEdge(edge);
        resetDPath();
        reconstituteDpath();
        return removed;
    }

    public boolean removeEdges(Collection<Edge> edges) {
        boolean change = false;

        for (Edge edge : edges) {
            boolean _change = removeEdge(edge);
            change = change || _change;
        }

        return change;

        //return graph.removeEdges(edges);
    }

    public boolean removeNode(Node node) {
        boolean removed = getGraph().removeNode(node);

        if (removed) {
            resetDPath();
            reconstituteDpath();
        }

        return removed;
    }

    public boolean removeNodes(List<Node> nodes) {
        return getGraph().removeNodes(nodes);
    }

    public void reorientAllWith(Endpoint endpoint) {
        throw new UnsupportedOperationException();
        //graph.reorientAllWith(endpoint);
    }

    public boolean possibleAncestor(Node node1, Node node2) {
        return getGraph().possibleAncestor(node1, node2);
    }

    public List<Node> getAncestors(List<Node> nodes) {
        return getGraph().getAncestors(nodes);
    }

    public boolean possDConnectedTo(Node node1, Node node2, List<Node> z) {
        return getGraph().possDConnectedTo(node1, node2, z);
    }

    private void resetDPath() {
        this.dpath = null;
        dpathNewEdges().clear();
        dpathNewEdges().addAll(getEdges());
    }

    private void reconstituteDpath() {
        if (this.dpath == null) {
            List<Node> dpathNodes = getNodes();
            size = getNodes().size();
            this.dpath = new byte[size][size];
            
            for (int i = 0; i < size; i++) {
                nodesHash.put(dpathNodes.get(i), i);
            }
        }

        while (!dpathNewEdges().isEmpty()) {
            Edge edge = dpathNewEdges().removeFirst();
            Node _node1 = Edges.getDirectedEdgeTail(edge);
            Node _node2 = Edges.getDirectedEdgeHead(edge);
            int _index1 = this.nodesHash.get(_node1);
            int _index2 = this.nodesHash.get(_node2);
            
            for (int i = 0; i < size; i++) {
                if (dpath[i][_index1] == 1) {
                    dpath[i][_index2] = 1;
                }

                if (dpath[_index2][i] == 1) {
                    dpath[_index1][i] = 1;
                }
            }
        }
    }

    public void transferNodesAndEdges(Graph graph)
            throws IllegalArgumentException {
        this.getGraph().transferNodesAndEdges(graph);
        for (Node node : this.getGraph().getNodes()) {
            node.getAllAttributes().clear();
        }
    }

    public void transferAttributes(Graph graph)
            throws IllegalArgumentException {
        this.getGraph().transferAttributes(graph);
    }

    public Set<Triple> getAmbiguousTriples() {
        return getGraph().getAmbiguousTriples();
    }

    public Set<Triple> getUnderLines() {
        return getGraph().getUnderLines();
    }

    public Set<Triple> getDottedUnderlines() {
        return getGraph().getDottedUnderlines();
    }


    /**
     * States whether x-y-x is an underline triple or not.
     */
    public boolean isAmbiguousTriple(Node x, Node y, Node z) {
        return getGraph().isAmbiguousTriple(x, y, z);
    }

    /**
     * States whether x-y-x is an underline triple or not.
     */
    public boolean isUnderlineTriple(Node x, Node y, Node z) {
        return getGraph().isUnderlineTriple(x, y, z);
    }

    /**
     * States whether x-y-x is an underline triple or not.
     */
    public boolean isDottedUnderlineTriple(Node x, Node y, Node z) {
        return getGraph().isDottedUnderlineTriple(x, y, z);
    }

    public void addAmbiguousTriple(Node x, Node y, Node z) {
        getGraph().addAmbiguousTriple(x, y, z);
    }

    public void addUnderlineTriple(Node x, Node y, Node z) {
        getGraph().addUnderlineTriple(x, y, z);
    }

    public void addDottedUnderlineTriple(Node x, Node y, Node z) {
        getGraph().addDottedUnderlineTriple(x, y, z);
    }

    public void removeAmbiguousTriple(Node x, Node y, Node z) {
        getGraph().removeAmbiguousTriple(x, y, z);
    }

    public void removeUnderlineTriple(Node x, Node y, Node z) {
        getGraph().removeUnderlineTriple(x, y, z);
    }

    public void removeDottedUnderlineTriple(Node x, Node y, Node z) {
        getGraph().removeDottedUnderlineTriple(x, y, z);
    }


    public void setAmbiguousTriples(Set<Triple> triples) {
        getGraph().setAmbiguousTriples(triples);
    }

    public void setUnderLineTriples(Set<Triple> triples) {
        getGraph().setUnderLineTriples(triples);
    }


    public void setDottedUnderLineTriples(Set<Triple> triples) {
        getGraph().setDottedUnderLineTriples(triples);
    }

    public List<String> getNodeNames() {
        return getGraph().getNodeNames();
    }

    public String toString() {
        return getGraph().toString();
    }

    private LinkedList<Edge> dpathNewEdges() {
        if (this.dpathNewEdges == null) {
            this.dpathNewEdges = new LinkedList<>();
        }
        return this.dpathNewEdges;
    }

    /**
     * Adds semantic checks to the default deserialization method. This method
     * must have the standard signature for a readObject method, and the body of
     * the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from
     * version to version. A readObject method of this form may be added to any
     * class, even if Tetrad sessions were previously saved out using a version
     * of the class that didn't include it. (That's what the
     * "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for help.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        if (getGraph() == null) {
            throw new NullPointerException();
        }
    }

    private Graph getGraph() {
        return this.graph;
    }

    @Override
    public List<String> getTriplesClassificationTypes() {
        return null;
    }

    @Override
    public List<List<Triple>> getTriplesLists(Node node) {
        return null;
    }

    @Override
    public boolean isPag() {
        return this.pag;
    }

    @Override
    public void setPag(boolean pag) {
        this.pag = pag;
    }

    @Override
    public boolean isCPDAG() {
        return this.CPDAG;
    }

    @Override
    public void setCPDAG(boolean CPDAG) {
        this.CPDAG = CPDAG;
    }

    @Override
    public Map<String, Object> getAllAttributes() {
        return this.attributes;
    }

    @Override
    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    @Override
    public void removeAttribute(String key) {
        this.attributes.remove(key);
    }

    @Override
    public void addAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }
}





