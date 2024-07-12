package org.albacete.simd.cges.framework;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.EdgeListGraph_n;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.FESThread;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class BNBuilder {
    /**
     * {@link Problem Problem} that contains all the information of the data.
     */
    protected final Problem problem;
    /**
     * The number of threads the algorithm is going to use.
     */
    protected int numberOfPartitions = 1;

    /**
     * Seed for the random number generator.
     */
    private long seed = 42;

    /**
     * Number of iterations allowed inside the FES stage. This is a parameter used in experimentation.
     */
    protected int interleaving = -1;


    /**
     * The maximum number of iterations allowed for the algorithm.
     */
    protected int maxIterations = -1;

    /**
     * The {@link GESThread GESThread} array that will be executed in each stage.
     * They can either be {@link FESThread ThFES} or {@link BESThread ThBES} threads.
     */
    protected GESThread[] gesThreads = null;

    /**
     * The {@link Thread Thread} array that encapsulate the {@link GESThread GESThread} for each stage.
     */
    protected Thread[] threads = null;

    /**
     * Subset of {@link Edge Edges}. Each subset will be assigned to {@link GESThread GESThread}
     */
    protected List<Set<Edge>> subSets = null;


    /**
     * {@link Graph Graph} containing the current bayesian network that has been constructed so far.
     */
    protected Graph currentGraph = null;

    /**
     * Previous {@link Graph Graph} containing the previous bayesian network that has been constructed so far.
     */
    protected double prevScore = Double.NEGATIVE_INFINITY;

    /**
     * An initial graph to start from.
     */
    protected Graph initialGraph;

    /**
     * Score of the currentGraph
     */
    protected double score = 0;

    /**
     * Iteration counter. It stores the current iteration of the algorithm.
     */
    protected int it = 1;

    /**
     * {@link Set Edge} set containing the possible edges of the resulting bayesian network.
     */
    protected Set<Edge> setOfArcs;

    private String hyperParamsBody;

    private String hyperParamsHeader;

    protected String algorithmName = this.getClass().getSimpleName();

    protected String networkName;

    protected String databaseName;


    public BNBuilder(DataSet data, int numberOfPartitions){
        this.problem = new Problem(data);
        initialize(numberOfPartitions);
    }

    public BNBuilder(String path, int numberOfPartitions) {
        this(Utils.readData(path), numberOfPartitions);
    }

    public BNBuilder(Graph initialGraph, DataSet data, int numberOfPartitions) {
        this(data, numberOfPartitions);
        this.initialGraph = new EdgeListGraph_n(initialGraph);
        checkForConsistenciesInInitialGraphWithProblem(initialGraph);
    }


    public BNBuilder(Graph initialGraph, String path, int numberOfPartitions) {
        this(initialGraph, Utils.readData(path), numberOfPartitions);
    }

    public BNBuilder(Graph initialGraph, Problem problem, int numberOfPartitions) {
        this.problem = problem;
        this.initialGraph = initialGraph;
        initialize(numberOfPartitions);

        checkForConsistenciesInInitialGraphWithProblem(initialGraph);

    }

    private void initialize(int nThreads) {
        this.numberOfPartitions = nThreads;
        this.gesThreads = new FESThread[this.numberOfPartitions];
        this.threads = new Thread[this.numberOfPartitions];
        this.subSets = new ArrayList<>(this.numberOfPartitions);

        //The total number of arcs of a graph is n*(n-1)/2, where n is the number of nodes in the graph.
        this.setOfArcs = new HashSet<>(this.problem.getData().getNumColumns() * (this.problem.getData().getNumColumns() - 1));
        this.setOfArcs = Utils.calculateArcs(this.problem.getData());
    }

    private void checkForConsistenciesInInitialGraphWithProblem(Graph initialGraph) {
        initialGraph = GraphUtils.replaceNodes(initialGraph, problem.getVariables());
        if (!new HashSet<>(initialGraph.getNodes()).equals(new HashSet<>(problem.getVariables()))) {
            throw new IllegalArgumentException("Variables aren't the same.");
        }
        this.initialGraph = initialGraph;
        this.currentGraph = GraphUtils.replaceNodes(initialGraph, problem.getVariables());

    }


    /**
     * Search method for all BNBuilders
     * @return Resulting graph (Bayesian Network) of performing the search with this BNBuilder algorithm.
     */
    public abstract Graph search();

    /**
     * Sets the seed for the random generator.
     * @param seed seed used for the random number generator.
     */
    public void setSeed(long seed) {
        this.seed = seed;
        Utils.setSeed(seed);
    }

    /**
     * Gets the used seed for the random number generator.
     * @return seed used in the random number generator
     */
    public long getSeed(){
        return this.seed;
    }

    /**
     * Gets the list of possible edges of the problem
     *
     * @return List of {@link Edge Edges} representing all the possible edges of the problem.
     */
    public Set<Edge> getSetOfArcs() {
        return setOfArcs;
    }

    /**
     * Gets the current subsets of edges.
     *
     * @return List of Lists of {@link Edge Edges} containing the edges of each subset.
     */
    public List<Set<Edge>> getSubSets() {
        return subSets;
    }

    /**
     * Gets the {@link DataSet DataSet} of the problem.
     * @return {@link DataSet DataSet} with the data of the problem.
     */
    public DataSet getData() {
        return problem.getData();
    }

    /**
     * Gets the maximum number of iterations.
     * @return number of maximum iterations.
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Sets the maximum number of iterations
     * @param maxIterations new value of the maximum number of iterations
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Sets the maximum number of iterations for each {@link FESThread ThFES}.
     * @param interleaving maximum number of iterations used in each {@link FESThread ThFES}.
     */
    public void setInterleaving(int interleaving) {
        this.interleaving = interleaving;
    }


    /**
     * Gets the {@link #currentGraph currentGraph} constructed so far.
     *
     * @return Dag_n of the currentGraph.
     */
    public Graph getCurrentGraph() {
        return this.currentGraph;
    }

    public Dag_n getCurrentDag() {
        if(this.currentGraph == null)
            return null;
        return Utils.removeInconsistencies(this.currentGraph);
    }

    /**
     * Gets the current iteration number.
     *
     * @return iteration the algorithm is in.
     */
    public int getIterations() {
        return it;
    }

    public Problem getProblem() {
        return problem;
    }


    public int getNumberOfThreads() {
        return numberOfPartitions;
    }

    public int getItInterleaving() {
        return interleaving;
    }


    public String getHyperParamsBody() {
        return hyperParamsBody;
    }

    public void setHyperParamsBody(String hyperParamsBody) {
        this.hyperParamsBody = hyperParamsBody;
    }

    public String getHyperParamsHeader() {
        return hyperParamsHeader;
    }

    public void setHyperParamsHeader(String hyperParamsHeader) {
        this.hyperParamsHeader = hyperParamsHeader;
    }


}
