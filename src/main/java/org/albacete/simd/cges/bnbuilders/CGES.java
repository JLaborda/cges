package org.albacete.simd.cges.bnbuilders;

import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.framework.BNBuilder;

import java.util.*;

import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.FESThread;
import org.albacete.simd.cges.utils.Utils;


public class CGES extends BNBuilder {
    
    private final String typeConvergence;
    
    private List<Set<Edge>> subsetEdges;
    private final Clustering clustering;
    private final List<CircularProcess> cgesProcesses;
    private CircularProcess bestCircularProcess;
    private double lastBestBDeu = Double.NEGATIVE_INFINITY;
    private boolean convergence;

    public enum Broadcasting {NO_BROADCASTING, PAIR_BROADCASTING, ALL_BROADCASTING, RANDOM_BROADCASTING, BEST_BROADCASTING}
    private final Broadcasting typeBroadcasting;

    private Random random = new Random(getSeed());
    
    public CGES(DataSet data, Clustering clustering, int nThreads, int nItInterleaving, String typeConvergence, Broadcasting typeBroadcasting) {
        super(data, nThreads, -1, nItInterleaving);

        this.clustering = clustering;
        clustering.setProblem(problem);
        this.cgesProcesses = new ArrayList<>(nThreads);
        this.typeConvergence = typeConvergence;
        this.typeBroadcasting = typeBroadcasting;
        setHyperParamsHeader("clustering,nThreads,interleaving,typeConvergence,typeBroadcasting");
        setHyperParamsBody(clustering.getClass().getSimpleName() + "," + nThreads + "," + nItInterleaving + "," + typeConvergence + "," + typeBroadcasting);
    }

    public CGES(String path, Clustering clustering, int nThreads, int nItInterleaving, String typeConvergence, Broadcasting typeBroadcasting) {
        this(Utils.readData(path), clustering, nThreads, nItInterleaving, typeConvergence, typeBroadcasting);
    }

    public CGES(String path, Clustering clustering, int nThreads, String typeConvergence, Broadcasting typeBroadcasting) {
        super(path, nThreads, -1, -1);
        this.clustering = clustering;
        clustering.setProblem(problem);
        this.cgesProcesses = new ArrayList<>(nThreads);
        this.typeConvergence = typeConvergence;
        this.typeBroadcasting = typeBroadcasting;
        this.interleaving = (int) (10 / nThreads * Math.sqrt(problem.getVariables().size()));
        setHyperParamsHeader("clustering,nThreads,interleaving,typeConvergence,typeBroadcasting");
        setHyperParamsBody(clustering.getClass().getSimpleName() + "," + nThreads + "," + this.interleaving + "," + typeConvergence + "," + typeBroadcasting);
        
        this.interleaving = (int) (10 / nThreads * Math.sqrt(problem.getVariables().size()));

    }

    
    @Override
    public Graph search(){
        //1. Setup
        initialConfig();

        //2. Do circular fusion while convergence is false
        search(this.typeBroadcasting);

        //3. Generate the best graph from the search
        calculateBestGraph();
        currentGraph = bestCircularProcess.dag;
        
        //4. Do a final GES with all the data
        Utils.println("\n\n\n FINAL GES");
        finalGES();
        
        return currentGraph;
    }

    protected void initialConfig() {
        it = 0;
        repartition();
        initializeThreads();
    }

    /**
     * Splits the edges of the problem into the same amount of partitions as threads used for the algorithm.
     */
    protected void repartition() {
        // Splitting edges with the clustering algorithm and assigning them to the subsetEdges attribute.
        clustering.setProblem(this.problem);
        subsetEdges = clustering.generateEdgeDistribution(numberOfThreads);
    }

    /**
     * Initializes each CGES process with a starting empty graph
     */
    private void initializeThreads(){
        for (int i = 0; i < numberOfThreads; i++) {
            cgesProcesses.add(new CircularProcess(problem,subsetEdges.get(i), interleaving,i));
        }
    }

    /**
     * This method selects the type of search to be performed by the CGES processes.
     * @param typeBroadcasting Type of broadcasting to be used in the search.
     */
    private void search(Broadcasting typeBroadcasting) {
        switch (typeBroadcasting) {
            case NO_BROADCASTING:
                noBroadcastingSearch();
                break;
            case PAIR_BROADCASTING:
                pairBroadcastingSearch();
                break;
            case ALL_BROADCASTING:
                allBroadcastingSearch();
                break;
            case RANDOM_BROADCASTING:
                randomBroadcastingSearch();
                break;
            case BEST_BROADCASTING:
                bestBroadcastingSearch();
                break;
        }
    }

    /**
     * This search loop fuses all the results of the CGES processes of the previous iteration, and passes it to processes
     * to compare the results of the process with the fusion of all the previous graphs. It then takes as result the best
     * of the two graphs, replacing the result of the CGES process with the fusion graph if it has a better score than the
     * original result of the process.
     */
    private void allBroadcastingSearch() {
        do{
            it++;
            putInputGraphs();
            Dag_n fusionDag = fuseAllInputDags();
            cgesProcesses.parallelStream().forEach((cdag) -> {
                try {
                    //Broadcasting
                    cdag.setAllFusedDag(fusionDag);
                    // Applying cges process
                    cdag.allFusedBroadcastingSearch();
                } catch (InterruptedException ex) {
                    Utils.println("Error with InterruptedException: " +
                            "\n Dag_n Id: " + cdag.id +
                            "\n Dag_n graph: " + cdag.dag);
                }
            });
        } while (notConverged());
    }

    private void putInputGraphs() {
        cgesProcesses.forEach((dag) -> {
            CircularProcess cd = getInputDag(dag.id);
            dag.setInputDag(cd.dag);
        });
    }

    private Dag_n fuseAllInputDags(){
        ArrayList<Dag_n> graphs = new ArrayList<>();
        for (CircularProcess cdag: cgesProcesses) {
            graphs.add(cdag.dag);
        }

        ConsensusUnion fusion = new ConsensusUnion(graphs);
        return fusion.union();
    }

    /**
     * Search loop that executes in parallel k processes of CGES processes. It only takes into account the results
     * of the CGES processes to pass to the posterior process, performing a cycle.
     */
    private void noBroadcastingSearch(){
        do{
            it++;
            putInputGraphs();
            cgesProcesses.parallelStream().forEach((cdag) -> {
                try {
                    // Applying cges process
                    cdag.noBroadcastingSearch();
                } catch (InterruptedException ex) {
                    Utils.println("Error with InterruptedException: " +
                            "\n Dag_n Id: " + cdag.id +
                            "\n Dag_n graph: " + cdag.dag);
                }
            });
        } while (notConverged());
    }

    private void pairBroadcastingSearch(){
        do{
            it++;
            // Add inputDag List
            addInputDagList();
            cgesProcesses.parallelStream().forEach(cdag -> {
                try {
                    cdag.pairBroadcastSearch();
                } catch (InterruptedException e) {
                    Utils.println("Error with InterruptedException: " +
                    "\n Dag_n Id: " + cdag.id +
                    "\n Dag_n graph: " + cdag.dag);
                    throw new RuntimeException(e);
                }
            });



        }while(notConverged());
    }

    private void randomBroadcastingSearch(){
        do{
            it++;
            // Add random input dags
            addRandomInput();
            cgesProcesses.parallelStream().forEach(cdag -> {
                try {
                    cdag.noBroadcastingSearch();
                } catch (InterruptedException e) {
                    Utils.println("Error with InterruptedException: " +
                            "\n Dag_n Id: " + cdag.id +
                            "\n Dag_n graph: " + cdag.dag);
                    throw new RuntimeException(e);
                }
            });
        }while(notConverged());
    }

    private void addRandomInput(){
        // Shuffle cgesProcesses
        Utils.shuffleCollection(cgesProcesses);
        // Add input as always
        this.putInputGraphs();
    }

    private void bestBroadcastingSearch(){

        do{
            // apply circular processes
            bestCircularProcess = cgesProcesses.parallelStream()
            .map(cdag -> {
                try {
                    cdag.bestBroadcastingSearch();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return new AbstractMap.SimpleEntry<>(cdag, cdag.getBDeu());
            })
                    // Get the one with the largest bdeu score
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);

          // Add best dag to each process  
            addBestInput();

        }while(notConverged());
        
        /*do{
            it++;
            // Add inputDag List
            addBestInput();
            cgesProcesses.parallelStream().forEach(cdag -> {
                try {
                    //Adding best
                    cdag.bestBroadcastingSearch();
                } catch (InterruptedException e) {
                    Utils.println("Error with InterruptedException: " +
                            "\n Dag_n Id: " + cdag.id +
                            "\n Dag_n graph: " + cdag.dag);
                    throw new RuntimeException(e);
                }
            });
        }while(notConverged());
        */
    }

    private void addBestInput(){
        // Calculate best graph
        // calculateBestGraph();
        // Add best input to all cgesProcesses
        cgesProcesses.forEach((dag) -> {
            dag.setInputDag(bestCircularProcess.dag);
        });

    }

    private void addInputDagList() {
        ArrayList<Dag_n> inputDags = getInputDags();
        cgesProcesses.forEach(cges -> cges.setInputDags(inputDags));
    }

    private ArrayList<Dag_n> getInputDags() {
        ArrayList<Dag_n> dags = new ArrayList<>(cgesProcesses.size());
        for (CircularProcess cges: cgesProcesses) {
            dags.add(new Dag_n(cges.dag));
        }
        return dags;
    }


    private CircularProcess getInputDag(int i) {
        if(i == 0) {
            return cgesProcesses.get(numberOfThreads - 1);
        } else {
            return cgesProcesses.get(i - 1);
        }
    }

    public void calculateBestGraph(){
        cgesProcesses.forEach(this::calculateBestGraph);
    }
    
    public void calculateBestGraph(CircularProcess dag){
        if (bestCircularProcess == null)
            bestCircularProcess = dag;
        else{
            if (dag.getBDeu() > bestCircularProcess.getBDeu())
                bestCircularProcess = dag;
        }
    }

    /**
     * Returns true when the search loop can continue (not converged), false otherwise.
     * @return boolean value stating if the search loop can continue (true) or not (false).
     */
    private boolean notConverged() {
        switch (typeConvergence) {
            // When any DAG changes in the iteration, there is no convergence
            case "c1":
            default:
                convergence = true;
                
                cgesProcesses.forEach(dag -> convergence = convergence && dag.convergence);
                
                return !convergence;
                
            // When any DAG improves the previous best DAG, there is no convergence
            case "c2":
                calculateBestGraph();

                boolean max = lastBestBDeu >= bestCircularProcess.getBDeu();
                
                lastBestBDeu = bestCircularProcess.getBDeu();
                        
                return !max;
        }
    }
    
    private void finalGES() {
        try {
            FESThread fes = new FESThread(problem, this.currentGraph, setOfArcs, Integer.MAX_VALUE, false, true, true);
            fes.run();
            currentGraph = fes.getCurrentGraph();
            
            BESThread bes = new BESThread(problem, currentGraph, setOfArcs);
            bes.run();
            currentGraph = bes.getCurrentGraph();
            score = bes.getScoreBDeu();
            currentGraph = Utils.removeInconsistencies(currentGraph);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            Utils.println("Error in FinalGES step");
        }
    }

    @Override
    public long getSeed() {
        // TODO Auto-generated method stub
        return super.getSeed();
    }

    @Override
    public void setSeed(long seed) {
        // TODO Auto-generated method stub
        super.setSeed(seed);
    }

}
