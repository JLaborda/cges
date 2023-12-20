package org.albacete.simd.cges.utils;

import edu.cmu.tetrad.bayes.BayesPm;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.graph.*;
import weka.classifiers.bayes.BayesNet;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Utils {


    private static Random random = new Random();

    private static boolean verbose = false;
    
    /**
     * Transforms a maximally directed pattern (PDAG) represented in graph
     * <code>g</code> into an arbitrary DAG by modifying <code>g</code> itself.
     * Based on the algorithm described in </p> Chickering (2002) "Optimal
     * structure identification with greedy search" Journal of Machine Learning
     * Research. </p> R. Silva, June 2004
     */
    public static void pdagToDag(Graph g) {
        Graph p = new EdgeListGraph_n(g);
        List<Edge> undirectedEdges = new ArrayList<>();

        for (Edge edge : g.getEdges()) {
            if (edge.getEndpoint1() == Endpoint.TAIL
                    && edge.getEndpoint2() == Endpoint.TAIL
                    && !undirectedEdges.contains(edge)) {
                undirectedEdges.add(edge);
            }
        }
        g.removeEdges(undirectedEdges);
        List<Node> pNodes = p.getNodes();

        do {
            Node x = null;

            for (Node pNode : pNodes) {
                x = pNode;

                if (p.getChildren(x).size() > 0) {
                    continue;
                }

                Set<Node> neighbors = new HashSet<>();

                for (Edge edge : p.getEdges()) {
                    if (edge.getNode1() == x || edge.getNode2() == x) {
                        if (edge.getEndpoint1() == Endpoint.TAIL
                                && edge.getEndpoint2() == Endpoint.TAIL) {
                            if (edge.getNode1() == x) {
                                neighbors.add(edge.getNode2());
                            } else {
                                neighbors.add(edge.getNode1());
                            }
                        }
                    }
                }
                if (neighbors.size() > 0) {
                    Collection<Node> parents = p.getParents(x);
                    Set<Node> all = new HashSet<>(neighbors);
                    all.addAll(parents);
                    if (!GraphUtils.isClique(all, p)) {
                        continue;
                    }
                }

                for (Node neighbor : neighbors) {
                    Node node1 = g.getNode(neighbor.getName());
                    Node node2 = g.getNode(x.getName());

                    g.addDirectedEdge(node1, node2);
                }
                p.removeNode(x);
                break;
            }
            pNodes.remove(x);
        } while (pNodes.size() > 0);
    }

    /**
     * Separates the set of possible arcs into as many subsets as threads we use to solve the problem.
     *
     * @param listOfArcs List of {@link Edge Edges} containing all the possible edges for the actual problem.
     * @param numSplits  The number of splits to do in the listOfArcs.
     * @return The subsets of the listOfArcs in an ArrayList of TupleNode.
     */
    public static List<Set<Edge>> split(Set<Edge> listOfArcs, int numSplits) {


        List<Set<Edge>> subSets = new ArrayList<>(numSplits);

        // Shuffling arcs
        List<Edge> shuffledArcs = new ArrayList<>(listOfArcs);
        Collections.shuffle(shuffledArcs, random);

        // Splitting Arcs into subsets
        int n = 0;
        for(int s = 0; s< numSplits-1; s++){
            Set<Edge> sub = new HashSet<>();
            for(int i = 0; i < Math.floorDiv(shuffledArcs.size(),numSplits) ; i++){
                sub.add(shuffledArcs.get(n));
                n++;
            }
            subSets.add(sub);
        }

        // Adding leftovers
        Set<Edge> sub = new HashSet<>();
        for(int i = n; i < shuffledArcs.size(); i++ ){
            sub.add(shuffledArcs.get(i));
        }
        subSets.add(sub);

        return subSets;

    }

    public static void setSeed(long seed){
        random = new Random(seed);
    }

    /**
     * Calculates the amount of possible arcs between the variables of the dataset and stores it.
     *
     * @param data DataSet used to calculate the arcs between its columns (nodes).
     */
    public static Set<Edge> calculateArcs(DataSet data) {
        //0. Accumulator
        Set<Edge> setOfArcs = new HashSet<>(data.getNumColumns() * (data.getNumColumns() - 1));
        //1. Get edges (variables)
        List<Node> variables = data.getVariables();
        //int index = 0;
        //2. Iterate over variables and save pairs
        for (int i = 0; i < data.getNumColumns() - 1; i++) {
            for (int j = i + 1; j < data.getNumColumns(); j++) {
                // Getting a pair of variables (Each variable is different)
                Node var_A = variables.get(i);
                Node var_B = variables.get(j);

                //3. Storing both pairs
                setOfArcs.add(Edges.directedEdge(var_A, var_B));
                setOfArcs.add(Edges.directedEdge(var_B, var_A));
            }
        }
        return setOfArcs;
    }


    /**
     * Stores the data from a csv as a DataSet object.
     * @param path
     * Path to the csv file.
     * @return DataSet containing the data from the csv file.
     */
    public static DataSet readData(String path){
        // Initial Configuration
        DataReader reader = new DataReader();
        reader.setDelimiter(DelimiterType.COMMA);
        reader.setMaxIntegralDiscrete(100);
        DataSet dataSet = null;
        // Reading data
        try {
            dataSet = reader.parseTabular(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataSet;
    }


    public static Node getNodeByName(List<Node> nodes, String name){
        for(Node n : nodes){
            if (n.getName().equals(name)){
                return n;
            }
        }
        return null;
    }

    public static int getIndexOfNodeByName(List<Node> nodes, String name){
        for(int i = 0; i < nodes.size(); i++){
            Node n = nodes.get(i);
            if(n.getName().equals(name)){
                return i;
            }
        }
        return -1;
    }

    private static List<Dag_n> ensureVariables(List<Dag_n> setofbns){
        List<Dag_n> ensuredDags = new ArrayList<>();
        List<Node> nodes = setofbns.get(0).getNodes();
        ensuredDags.add(new Dag_n(setofbns.get(0)));
        for(int i = 1 ; i< setofbns.size(); i++) {
            Dag_n oldDag = setofbns.get(i);
            Set<Edge> oldEdges = oldDag.getEdges();
            Dag_n newDag = new Dag_n(nodes);
            for(Edge e: oldEdges){

                int tailIndex = getIndexOfNodeByName(nodes, e.getNode1().getName());
                int headIndex = getIndexOfNodeByName(nodes, e.getNode2().getName());

                Edge newEdge = new Edge(nodes.get(tailIndex),nodes.get(headIndex), Endpoint.TAIL, Endpoint.ARROW);
                newDag.addEdge(newEdge);
            }
            ensuredDags.add(newDag);
        }
        return ensuredDags;
    }


    public static int SHD (Dag_n bn1, Dag_n bn2) {
        if(bn1 == null || bn2 == null)
            return -1;
        List<Dag_n> dags = new ArrayList<>();
        dags.add(bn1);
        dags.add(bn2);
        dags = ensureVariables(dags);

        Graph g1 = new EdgeListGraph_n(dags.get(0));
        Graph g2 = new EdgeListGraph_n(dags.get(1));

        moralizeGraph(g1);
        moralizeGraph(g2);

        int sum = 0;
        for(Edge e: g1.getEdges()) {
            Edge e2 = g2.getEdge(e.getNode1(), e.getNode2());
            Edge e3 = g2.getEdge(e.getNode2(), e.getNode1());
            if(e2 == null && e3 == null) sum++;
        }

        for(Edge e: g2.getEdges()) {
            Edge e2 = g1.getEdge(e.getNode1(), e.getNode2());
            Edge e3 = g1.getEdge(e.getNode2(), e.getNode1());
            if(e2 == null && e3 == null) sum++;
        }
        return sum;
    }

    public static void moralizeGraph(Graph graph) {
        for(Node n: graph.getNodes()) {
            List<Node> p = graph.getParents(n);
            for (int i=0; i<p.size()-1;i++)
                for(int j=i+1; j<p.size();j++) {
                    Edge e1 = graph.getEdge(p.get(i), p.get(j));
                    Edge e2 = graph.getEdge(p.get(j), p.get(i));
                    if(e1==null && e2 == null) {
                        Edge e = new Edge(p.get(i),p.get(j),Endpoint.TAIL,Endpoint.TAIL);
                        graph.addEdge(e);
                    }
                }
        }
    }


    public static List<Node> getMarkovBlanket(Dag_n bn, Node n){
        List<Node> mb = new ArrayList<>();

        // Adding children and parents to the Markov's Blanket of this node
        List<Node> children = bn.getChildren(n);
        List<Node> parents = bn.getParents(n);

        mb.addAll(children);
        mb.addAll(parents);

        for(Node child : children){
            for(Node father : bn.getParents(child)){
                if (!father.equals(n)){
                    mb.add(father);
                }
            }
        }

        return mb;
    }

    /**
     * Gives back the percentages of markov's blanket difference with the original bayesian network. It gives back the
     * percentage of difference with the blanket of the original bayesian network, the percentage of extra nodes added
     * to the blanket and the percentage of missing nodes in the blanket compared with the original.
     * @param original Original graph
     * @param created Resulting graph of the structural learning algorithm
     * @return scores of the average markov blanket
     */
    public static double [] avgMarkovBlanketDelta(Dag_n original, Dag_n created) {

        if (original.getNodes().size() != created.getNodes().size())
            return null;

        for (String originalNodeName : original.getNodeNames()) {
            if (!created.getNodeNames().contains(originalNodeName))
                return null;
        }

        // First number is the average dfMB, the second one is the amount of more variables in each MB, the last number is the amount of missing variables in each MB
        double[] result = new double[3];
        double differenceNodes = 0;
        double plusNodes = 0;
        double minusNodes = 0;


        for (Node e1 : original.getNodes()) {
            Node e2 = created.getNode(e1.getName());

            // Creating Markov's Blanket
            List<Node> mb1 = getMarkovBlanket(original, e1);
            List<Node> mb2 = getMarkovBlanket(created, e2);


            ArrayList<String> names1 = new ArrayList<>();
            ArrayList<String> names2 = new ArrayList<>();
            // Nodos de más en el manto creado
            for (Node n1 : mb1) {
                String name1 = n1.getName();
                names1.add(name1);
            }
            for (Node n2 : mb2) {
                String name2 = n2.getName();
                names2.add(name2);
            }

            //Variables de más
            for(String s2: names2) {
                if(!names1.contains(s2)) {
                    differenceNodes++;
                    plusNodes++;
                }
            }
            // Variables de menos
            for(String s1: names1) {
                if(!names2.contains(s1)) {
                    differenceNodes++;
                    minusNodes++;
                }
            }
        }

        // Differences of MM

        result[0] = differenceNodes;
        result[1] = plusNodes;
        result[2] = minusNodes;

        return result;

    }

    /**
     * Transforms a graph to a DAG, and removes any possible inconsistency found throughout its structure.
     * @param g Graph to be transformed.
     * @return Resulting DAG of the inserted graph.
     */
    public static Dag_n removeInconsistencies(Graph g){
        // Transforming the current graph into a DAG
        pdagToDag(g);

        // Checking Consistency
        Node nodeT, nodeH;
        for (Edge e : g.getEdges()){
            if(!e.isDirected()) continue;
            Endpoint endpoint1 = e.getEndpoint1();
            if (endpoint1.equals(Endpoint.ARROW)){
                nodeT = e.getNode1();
                nodeH = e.getNode2();
            }else{
                nodeT = e.getNode2();
                nodeH = e.getNode1();
            }


            if(g.existsDirectedPathFromTo(nodeT, nodeH)){
                if(verbose)
                    Utils.println("Directed path from " + nodeT + " to " + nodeH +"\t Deleting Edge...");
                g.removeEdge(e);
            }
        }
        // Adding graph from each thread to the graphs array
        return new Dag_n(g);

    }

    /**
     * Transforms a BayesNet read from a xbif file into a BayesPm object for tetrad
     *
     * @param wekabn BayesNet read from a xbif file
     * @return The BayesPm of the BayesNet
     */
    public static BayesPm transformBayesNetToBayesPm(BayesNet wekabn) {
        Dag_n graph = new Dag_n();

        // Getting nodes from weka network and adding them to a GraphNode
        for (int indexNode = 0; indexNode < wekabn.getNrOfNodes(); indexNode++) {
            GraphNode node = new GraphNode(wekabn.getNodeName(indexNode));
            graph.addNode(node);
        }
        // Adding all the edges from the wekabn into the new Graph
        for (int indexNode = 0; indexNode < wekabn.getNrOfNodes(); indexNode++) {
            int nParent = wekabn.getNrOfParents(indexNode);
            for (int np = 0; np < nParent; np++) {
                int indexp = wekabn.getParent(indexNode, np);
                Edge ed = new Edge(graph.getNode(wekabn.getNodeName(indexp)), graph.getNode(wekabn.getNodeName(indexNode)), Endpoint.TAIL, Endpoint.ARROW);
                graph.addEdge(ed);
            }
        }
        return new BayesPm(graph);

    }

    /**
     * Shuffle collection using the random object of Utils class.
     */
    public static <T> void shuffleCollection(Collection<T> collection){
        List<T> list = new ArrayList<>(collection);
        Collections.shuffle(list, random);
        collection.clear();
        collection.addAll(list);
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static void setVerbose(boolean verbose) {
        Utils.verbose = verbose;
    }

    public static void println(String output){
        if(verbose)
            System.out.println(output);
    }

}
