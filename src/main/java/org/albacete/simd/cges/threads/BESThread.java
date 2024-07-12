package org.albacete.simd.cges.threads;

import consensusBN.PowerSet;
import consensusBN.PowerSetFabric;
import consensusBN.SubSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class BESThread extends GESThread {


    private static int threadCounter = 1;

    /**
     * Constructor of ThFES with an initial DAG
     *
     * @param problem    object containing information of the problem such as data or variables.
     * @param initialDag initial DAG with which the BES stage starts with.
     * @param subset     subset of edges the fes stage will try to remove 
     */
    public BESThread(Problem problem, Graph initialDag, Set<Edge> subset) {

        this.problem = problem;
        setInitialGraph(initialDag);
        setSubSetSearch(subset);

        // Setting structure prior and sample prior
        setStructurePrior(0.001);
        setSamplePrior(10.0);
        this.id = threadCounter;
        threadCounter++;
        this.isForwards = false;
    }

    /**
    Run method from {@link Runnable Runnable} interface. The method executes the {@link #search()} search method to remove
    edges from the initial graph.
     */
    @Override
    public void run() {
        this.currentGraph = search();
        Utils.pdagToDag(this.currentGraph);
    }

    /**
     * Search method that explores the data and currentGraph to return a better Graph
     * @return PDAG that contains either the result of the BES or FES method.
     */
    private Graph search() {
        if (!S.isEmpty()) {
            startTime = System.currentTimeMillis();
            numTotalCalls=0;
            numNonCachedCalls=0;
            //localScoreCache.clear();

            Graph graph = new EdgeListGraph_n(this.initialDag);
            //buildIndexing(graph);

            // Method 1-- original.
            double scoreInitial = scoreGraph(graph);

            // Do backward search.
            bes(graph, scoreInitial);

            long endTime = System.currentTimeMillis();
            this.elapsedTime = endTime - startTime;

            double newScore = scoreGraph(graph);
            Utils.println(" ["+getId()+"] BES New Score: " + newScore + ", Initial Score: " + scoreInitial);
            // If we improve the score, return the new graph
            if (newScore > scoreInitial) {
                this.modelBDeu = newScore;
                this.flag = true;
                return graph;
            } else {
                //Utils.println("   ["+getId()+"] ELSE");
                this.modelBDeu = scoreInitial;
                this.flag = false;
                return this.initialDag;
            }
        } else return this.initialDag;
    }

    /**
     * Backward equivalence search.
     *
     * @param graph The graph in the state prior to the backward equivalence
     *              search.
     * @param score The score in the state prior to the backward equivalence
     *              search
     * @return the score in the state after the BES method.
     *         Note that the graph is changed as a side-effect to its state after
     *         the backward equivalence search.
     */
    @SuppressWarnings("UnusedReturnValue")
    private double bes(Graph graph, double score) {
        //Utils.println("** BACKWARD EQUIVALENCE SEARCH");
        double bestScore = score;
        double bestDelete;

        x_d = null;
        y_d = null;
        h_0 = null;

        //Utils.println("Initial Score = " + nf.format(bestScore));
        // Calling fs to calculate best edge to add.
        bestDelete = bs(graph,bestScore);

        while(x_d != null){
            // Changing best score because x_d, and y_d are not null
            bestScore = bestDelete;

            // Deleting edge
            //Utils.println("Thread " + getId() + " deleting: (" + x_d + ", " + y_d + ", " + h_0+ ")");
            delete(x_d,y_d,h_0, graph);
            
            // Checking cycles?
            //Utils.println("  Cycles: " + graph.existsDirectedCycle());

            //PDAGtoCPDAG
            rebuildPattern(graph);
            
            // Printing score
            //bestScore = bestDelete;
            //Utils.println("    Real Score" + scoreGraph(graph, problem));

            // Checking that the maximum number of edges has not been reached
            if (getMaxNumEdges() != -1 && graph.getNumEdges() > getMaxNumEdges()) {
                //Utils.println("Maximum edges reached");
                break;
            }

            // Executing BS function to calculate the best edge to be deleted
            bestDelete = bs(graph,bestScore);

            // Indicating that the thread has deleted an edge to the graph
            this.flag = true;

        }
        return bestScore;

    }

    /**
     * BS method of the BES algorithm. It finds the best possible edge, alongside with the subset h_0 that is best suited
     * for deletion in the current graph.
     * @param graph current graph of the thread.
     * @param initialScore score the current graph has.
     * @return score of the best possible deletion found.
     */
    private double bs(Graph graph, double initialScore){

        PowerSetFabric.setMode(PowerSetFabric.MODE_BES);

        x_d = y_d = null;
        h_0 = null;
        
        Set<Edge> edgesInGraph = graph.getEdges();
        
        if (!edgesInGraph.isEmpty()) {
            EdgeSearch[] arrScores = new EdgeSearch[edgesInGraph.size()];
            List<Edge> edges = new ArrayList<>(edgesInGraph);

            Arrays.parallelSetAll(arrScores, e-> scoreEdge(graph, edges.get(e), initialScore));

            List<EdgeSearch> list = Arrays.asList(arrScores);
            EdgeSearch max = Collections.max(list);

            if (max.score > initialScore) {
                x_d = max.edge.getNode1();
                y_d = max.edge.getNode2();
                h_0 = max.hSubset;
            }
            
            return max.score;
        }
        
        return initialScore;
    }
    
    private EdgeSearch scoreEdge(Graph graph, Edge edge, double initialScore) {
        // Checking if the edge is actually inside the graph
        if(S.contains(edge)) {
            Node _x = Edges.getDirectedEdgeTail(edge);
            Node _y = Edges.getDirectedEdgeHead(edge);

            List<Node> hNeighbors = getSubsetOfNeighbors(_x, _y, graph);
            PowerSet hSubsets = PowerSetFabric.getPowerSet(_x, _y, hNeighbors);
            
            double changeEval;
            double evalScore;
            double bestScore = initialScore; 
            SubSet bestSubSet = new SubSet();
            
            while(hSubsets.hasMoreElements()) {
                SubSet hSubset = hSubsets.nextElement();
                changeEval = deleteEval(_x, _y, hSubset, graph);
                
                evalScore = initialScore + changeEval;

                if (evalScore > bestScore) {
                    // START TEST 1
                    List<Node> naYXH = findNaYX(_x, _y, graph);
                    naYXH.removeAll(hSubset);
                    if (isClique(naYXH, graph)) {
                        // END TEST 1
                        bestScore = evalScore;
                        bestSubSet = hSubset;
                    }
                }
            }
            return new EdgeSearch(bestScore, bestSubSet, edge);
        }
        return new EdgeSearch(initialScore, new SubSet(), edge);
    }

}
