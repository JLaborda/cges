package org.albacete.simd.cges.threads;

import consensusBN.SubSet;
import edu.cmu.tetrad.graph.*;
import org.albacete.simd.cges.utils.Problem;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ForwardHillClimbingThread extends GESThread {

    private static int threadCounter = 1;


    /**
     * Constructor of ThFES with an initial DAG
     *
     * @param problem    object containing all the information of the problem
     * @param initialDag initial DAG with which the FES stage starts with, if it's null, use the other constructor
     * @param subset     subset of edges the fes stage will try to add to the resulting graph
     * @param maxIt      maximum number of iterations allowed in the fes stage
     */
    public ForwardHillClimbingThread(Problem problem, Graph initialDag, Set<Edge> subset, int maxIt) {
        this.problem = problem;
        setInitialGraph(initialDag);
        setSubSetSearch(subset);
        this.maxIt = maxIt;
        this.id = threadCounter;
        threadCounter++;
        this.isForwards = true;
    }

    /**
     * Constructor of FESThread with an initial DataSet
     *
     * @param problem object containing information of the problem such as data or variables.
     * @param subset  subset of edges the fes stage will try to add to the resulting graph
     * @param maxIt   maximum number of iterations allowed in the fes stage
     */
    public ForwardHillClimbingThread(Problem problem, Set<Edge> subset, int maxIt) {
        this.problem = problem;
        this.initialDag = new EdgeListGraph(new LinkedList<>(getVariables()));
        setSubSetSearch(subset);
        this.maxIt = maxIt;
        this.id = threadCounter;
        threadCounter++;
    }


    @Override
    public void run() {
        this.currentGraph = search();
    }

    private Graph search() {
        startTime = System.currentTimeMillis();
        numTotalCalls=0;
        numNonCachedCalls=0;


        Graph graph = new EdgeListGraph(this.initialDag);

        double score = scoreGraph(graph, problem);

        // Do Forwards HillClimbing
        score = fhc(graph, score);

        long endTime = System.currentTimeMillis();
        this.elapsedTime = endTime - startTime;
        this.modelBDeu = score;
        return graph;


    }

    private double fhc(Graph graph, double score){


        double bestScore = score;
        List<Edge> edges = new ArrayList<>(S);



        System.out.println("[FHC " + getId() + "]" + " Number of edges to check: " + edges.size());

        // Hillclimbing algorithm
        boolean improvement;
        int iteration = 1;
        do{
            System.out.println("[FHC " + getId() + "]" + "iteration: " + iteration);
            improvement = false;
            Node bestX = null;
            Node bestY = null;
            Edge bestEdge = null;
            SubSet bestSubSet = null;
            for(Edge edge : edges) {

                //Checking Time
                if(isTimeout())
                    break;

                //System.out.println("[FHC " + getId() + "]" + "Checking edge: " + edge);

                if (graph.containsEdge(edge)) {
                    //System.out.println("[FHC " + getId() + "]" + " contains" + edge);
                    continue;
                }


                Node _x = Edges.getDirectedEdgeTail(edge);
                Node _y = Edges.getDirectedEdgeHead(edge);

                if (graph.isAdjacentTo(_x, _y)) {
                    //System.out.println("[FHC " + getId() + "]" + edge + " is already adjacent to the graph.");
                    continue;
                }

                // Comprobar ciclos dirigidos aquí?
                if(graph.existsDirectedPathFromTo(_y, _x)) {
                    continue;
                }

                // Selecting parents of the head (_y)
                SubSet subset = new SubSet();

                double insertEval = insertEval(_x, _y, subset, graph, problem);
                double evalScore = score + insertEval;

                if (evalScore > bestScore) {
                    //insert(_x, _y, subset, graph);
                    bestX = _x;
                    bestY = _y;
                    bestEdge = edge;
                    bestScore = evalScore;
                    bestSubSet = subset;
                    improvement = true;
                }
            }

            if(improvement){
                // Checking directed cycles
                if(!graph.existsDirectedPathFromTo(bestY, bestX)) {
                    // Inserting edge
                    System.out.println("Thread FHC " + getId() + " inserting: (" + bestX + ", " + bestY + ", " + bestSubSet+ ")");
                    insert(bestX, bestY, bestSubSet, graph);
                    score = bestScore;
                    System.out.println("[FHC "+getId() + "] Score: " + nf.format(bestScore) + "\tOperator: " + graph.getEdge(bestX, bestY));
                    // Rebuilding pattern from cpdag to pdag
                    //rebuildPattern(graph);

                    this.flag = true;
                    //Updating score
                }
                edges.remove(bestEdge);
            }
            iteration++;
        }while(improvement && iteration <= this.maxIt);

        return bestScore;
    }
}
