package org.albacete.simd.cges.threads;

import consensusBN.PowerSetFabric;
import consensusBN.SubSet;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.MeekRules;
import edu.cmu.tetrad.search.SearchGraphUtils;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;

import java.util.*;

import java.util.stream.Collectors;

public class FESThread extends GESThread {

    private static int threadCounter = 1;
    
    private final boolean speedUp;
    private final boolean update;
    private final boolean parallel;

    /**
     * Constructor of FESThread with an initial DAG
     *
     * @param problem object containing all the information of the problem
     * @param initialDag initial DAG with which the FES stage starts with, if
     * it's null, use the other constructor
     * @param subset subset of edges the fes stage will try to add to the
     * resulting graph
     * @param maxIt maximum number of iterations allowed in the fes stage
     * @param speedUp reduces the number of edges to check in the stages if true.
     */
    public FESThread(Problem problem, Graph initialDag, Set<Edge> subset, int maxIt, boolean speedUp, boolean update, boolean parallel) {
        this(problem, subset, maxIt, speedUp, update, parallel);
        this.initialDag = initialDag;
    }

    /**
     * Constructor of FESThread with an initial DataSet
     *
     * @param problem object containing information of the problem such as data
     * or variables.
     * @param subset subset of edges the fes stage will try to add to the
     * resulting graph
     * @param maxIt maximum number of iterations allowed in the fes stage
     * @param speedUp reduces the number of edges to check in the stages if true.
     */
    public FESThread(Problem problem, Set<Edge> subset, int maxIt, boolean speedUp, boolean update, boolean parallel) {
        this.problem = problem;
        this.initialDag = new EdgeListGraph_n(new LinkedList<>(getVariables()));
        setSubSetSearch(subset);
        setMaxIt(maxIt);
        this.id = threadCounter;
        threadCounter++;
        this.isForwards = true;
        this.speedUp = speedUp;
        this.update = update;
        this.parallel = parallel;
    }

    //==========================PUBLIC METHODS==========================//
    @Override
    /*
      Run method from {@link Thread} interface. The method executes the {@link #search()} search} method to add
      edges to the initial graph.
     */
    public void run() {
        this.currentGraph = search();
        Utils.pdagToDag(this.currentGraph);
    }

    //===========================PRIVATE METHODS========================//
    /**
     * Greedy equivalence search: Start from the empty graph, add edges till
     * model is significant. Then start deleting edges till a minimum is
     * achieved.
     *
     * @return the resulting Pattern.
     */
    private Graph search() {
        if (!S.isEmpty()) {
            startTime = System.currentTimeMillis();
            numTotalCalls = 0;
            numNonCachedCalls = 0;
            //localScoreCache.clear();

            Graph graph = new EdgeListGraph_n(this.initialDag);
            //buildIndexing(graph);

            // Method 1-- original.
            double scoreInitial = scoreDag(graph);

            // Do backward search.
            fes(graph, scoreInitial);

            long endTime = System.currentTimeMillis();
            this.elapsedTime = endTime - startTime;

            double newScore = scoreDag(graph);
            Utils.println(" [" + getId() + "] FES New Score: " + newScore + ", Initial Score: " + scoreInitial);
            // If we improve the score, return the new graph
            if (newScore > scoreInitial) {
                this.modelBDeu = newScore;
                this.flag = true;
                return graph;
            } else {
                //Utils.println("   [" + getId() + "] ELSE");
                this.modelBDeu = scoreInitial;
                this.flag = false;
                return this.initialDag;
            }
        }
        return this.initialDag;
    }

    /**
     * Forward equivalence search.
     *
     * @param graph The graph in the state prior to the forward equivalence
     * search.
     * @param score The score in the state prior to the forward equivalence
     * search
     * @return the score in the state after the FES method. Note that the graph
     * is changed as a side effect to its state after the forward equivalence
     * search.
     */
    @SuppressWarnings("UnusedReturnValue")
    private double fes(Graph graph, double score) {
        //Utils.println("** FORWARD EQUIVALENCE SEARCH");
        double bestScore = score;
        double bestInsert;

        iterations = 0;
        
        //Utils.println("Initial Score = " + nf.format(bestScore));
        // Calling fs to calculate best edge to add.
        enlaces = S;
        bestInsert = fs(graph);
        while ((x_i != null) && (iterations < this.maxIt)) {
            // Changing best score because x_i, and therefore, y_i is not null
            bestScore = bestInsert;

            // Inserting edge
            Utils.println("Thread " + getId() + " inserting: (" + x_i + ", " + y_i + ", " + t_0 + "), score: " + bestScore);
            insert(x_i, y_i, t_0, graph);

            // Checking cycles?
            //boolean cycles = graph.existsDirectedCycle();

            //PDAGtoCPDAG
            if (update)
                updateEdges(graph);
            else
                rebuildPattern(graph);
            
            // Printing score
            bestScore = bestInsert;

            // Checking that the maximum number of edges has not been reached
            if (getMaxNumEdges() != -1 && graph.getNumEdges() >= getMaxNumEdges()) {
                //Utils.println("Maximum edges reached");
                break;
            }

            // Executing FS function to calculate the best edge to be added
            bestInsert = fs(graph);

            // Indicating that the thread has added an edge to the graph
            this.flag = true;
            iterations++;
        }
        return bestScore;

    }

    /**
     * Forward search. Finds the best possible edge to be added into the current
     * graph and returns its score.
     *
     * @param graph The graph in the state prior to the forward equivalence
     * search.
     * @return the score in the state after the forward equivalence search. Note
     * that the graph is changed as a side effect to its state after the forward
     * equivalence search.
     */
    private double fs(Graph graph) {

        PowerSetFabric.setMode(PowerSetFabric.MODE_FES);

        x_i = y_i = null;
        t_0 = null;

        Set<EdgeSearch> newScores;
        if (parallel)
            newScores = enlaces.parallelStream()
                .map(e -> scoreEdge(graph, e))
                .collect(Collectors.toSet());
        else
            newScores = enlaces.stream()
                .map(e -> scoreEdge(graph, e))
                .collect(Collectors.toSet());
        
        HashSet<EdgeSearch> temp = new HashSet<>();
        temp.addAll(newScores);
        temp.addAll(this.scores);
        this.scores = temp;
        
        EdgeSearch max = Collections.max(this.scores);

        if (max.score > 0) {
            //Assigning values to x_i, y_i and t_0
            x_i = max.edge.getNode1();
            y_i = max.edge.getNode2();
            t_0 = max.hSubset;

            // Deleting the selected edge from enlaces
            enlaces.remove(max.edge);
            this.scores.remove(max);
        }

        return max.score;
    }

    private void updateEdges(Graph graph){
        // Modo normal
        if (!speedUp) {
            // Getting the common adjacents of x_i and y_i
            Set<Node> process = revertToCPDAG(graph);
            removeEdgesNotNeighbors(graph, process);
        }
        // Modo heurístico. No comprobamos los enlaces invertidos en revertToCPDAG
        else {
            // Getting the common adjacents of x_i and y_i
            Set<Node> process = new HashSet<>();
            removeEdgesNotNeighbors(graph, process);
        }
    }

    private void removeEdgesNotNeighbors(Graph graph, Set<Node> process) {
        process.add(x_i);
        process.add(y_i);

        process.addAll(graph.getAdjacentNodes(x_i));
        process.addAll(graph.getAdjacentNodes(y_i));

        enlaces = new HashSet<>(S);
        enlaces.removeIf(edge -> {
            Node x = edge.getNode1();
            Node y = edge.getNode2();
            return !process.contains(x) && !process.contains(y);
        });
        //Utils.println("TAMAÑO DE enlaces: " + enlaces.size() + ", S: " + S.size() + ". \t Process: " + process.size()  + ", revert: " + tam);
    }

    private Set<Node> revertToCPDAG(Graph graph) {
        SearchGraphUtils.basicCPDAG(graph);
        MeekRules rules = new MeekRules();
        rules.setAggressivelyPreventCycles(true);
        return rules.orientImplied(graph);
    }

    private EdgeSearch scoreEdge(Graph graph, Edge edge) {
        Node _x = Edges.getDirectedEdgeTail(edge);
        Node _y = Edges.getDirectedEdgeHead(edge);

        if (!graph.isAdjacentTo(_x, _y)) {
            List<Node> tNeighbors = getSubsetOfNeighbors(_x, _y, graph);

            SubSet tSubset = new SubSet();
            double insertEval = insertEval(_x, _y, tSubset, graph, problem);
            //Utils.println("InsertEval: " + insertEval);
            if (insertEval > 0) {
                List<Node> naYXT = new LinkedList<>(tSubset);
                List<Node> naYX = findNaYX(_x, _y, graph);
                naYXT.addAll(naYX);

                boolean passTests = true;

                // START TEST 1
                if (tSubset.firstTest == SubSet.TEST_NOT_EVALUATED) {
                    if (!isClique(naYXT, graph)) {
                        passTests = false;
                    }
                } else if (tSubset.firstTest == SubSet.TEST_FALSE) {
                    passTests = false;
                }
                // END TEST 1

                // START TEST 2
                if (tSubset.secondTest == SubSet.TEST_NOT_EVALUATED) {
                    if (!isSemiDirectedBlocked(_x, _y, naYXT, graph, new HashSet<>())) {
                        passTests = false;
                    }
                } else if (tSubset.secondTest == SubSet.TEST_FALSE) { // This can't happen
                    passTests = false;
                }
                // END TEST 2

                if (passTests) {
                    double greedyScore = insertEval;
                    int bestNodeIndex;
                    Node bestNode = null;

                    do {
                        bestNodeIndex = -1;
                        for (int k = 0; k < tNeighbors.size(); k++) {
                            Node node = tNeighbors.get(k);
                            SubSet newT = new SubSet(tSubset);
                            newT.add(node);
                            insertEval = insertEval(_x, _y, newT, graph, problem);

                            if (insertEval <= greedyScore) {
                                continue;
                            }

                            naYXT = new LinkedList<>(newT);
                            naYXT.addAll(naYX);

                            // START TEST 1
                            if (tSubset.firstTest == SubSet.TEST_NOT_EVALUATED) {
                                if (!isClique(naYXT, graph)) {
                                    continue;
                                }
                            } else if (tSubset.firstTest == SubSet.TEST_FALSE) {
                                continue;
                            }
                            // END TEST 1

                            // START TEST 2
                            if (tSubset.secondTest == SubSet.TEST_NOT_EVALUATED) {
                                if (!isSemiDirectedBlocked(_x, _y, naYXT, graph, new HashSet<>())) {
                                    continue;
                                }
                            } else if (tSubset.secondTest == SubSet.TEST_FALSE) { // This can't happen
                                continue;
                            }

                            bestNodeIndex = k;
                            bestNode = node;
                            greedyScore = insertEval;
                        }
                        if (bestNodeIndex != -1) {
                            tSubset.add(bestNode);
                            tNeighbors.remove(bestNodeIndex);
                        }

                    } while ((bestNodeIndex != -1) && (tSubset.size() <= 1));

                    if (greedyScore > insertEval) {
                        insertEval = greedyScore;
                    }
                    //Utils.println("InsertEval: " + insertEval);
                    return new EdgeSearch(insertEval, tSubset, edge);

                }
            }
        }
        return new EdgeSearch(0, new SubSet(), edge);

    }




}
