package org.albacete.simd.cges.bnbuilders;

import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.framework.BNBuilder;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.FESThread;
import org.albacete.simd.cges.utils.Utils;


public class CGES extends BNBuilder {
    private List<Set<Edge>> subsetEdges;
    private final Clustering clustering;
    private final List<CircularProcess> cgesProcesses;
    private CircularProcess bestCircularProcess;
    private double lastBestBDeu = Double.NEGATIVE_INFINITY;

    public enum Broadcasting {NO_BROADCASTING, PAIR_BROADCASTING, ALL_BROADCASTING, RANDOM_BROADCASTING, BEST_BROADCASTING}
    private final Broadcasting typeBroadcasting;

    
    public CGES(DataSet data, Clustering clustering, int numberOfProcesses, Broadcasting typeBroadcasting) {
        super(data, numberOfProcesses);

        this.clustering = clustering;
        clustering.setProblem(problem);
        this.cgesProcesses = new ArrayList<>(numberOfProcesses);
        this.typeBroadcasting = typeBroadcasting;
        this.interleaving = (int) (10 / numberOfProcesses * Math.sqrt(problem.getVariables().size()));
        setHyperParamsHeader("clustering,numberOfProcesses,interleaving,typeBroadcasting");
        setHyperParamsBody(clustering.getClass().getSimpleName() + "," + numberOfProcesses + "," + interleaving + "," + typeBroadcasting.toString());
    }

    public CGES(String path, Clustering clustering, int numberOfProcesses, Broadcasting typeBroadcasting) {
        this(Utils.readData(path), clustering, numberOfProcesses, typeBroadcasting);
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
        subsetEdges = clustering.generateEdgeDistribution(numberOfPartitions);
    }

    /**
     * Initializes each CGES process with a starting empty graph
     */
    private void initializeThreads(){
        for (int i = 0; i < numberOfPartitions; i++) {
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
            case ALL_BROADCASTING:
                allBroadcastingSearch();
                break;
            case PAIR_BROADCASTING:
                pairBroadcastingSearch();
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

    private void putInputGraphs() {
        for (int i = 0; i < cgesProcesses.size(); i++) {
            CircularProcess inputProcess, currentProcess;

            currentProcess = cgesProcesses.get(i);
            if(i==0){
                inputProcess = cgesProcesses.get(cgesProcesses.size()-1);
            } else {
                inputProcess = cgesProcesses.get(i-1);
            }
            currentProcess.setInputDag(inputProcess.dag);
        }
        /*cgesProcesses.forEach((dag) -> {
            CircularProcess cd = getInputDag(dag.id);
            dag.setInputDag(cd.dag);
        });*/
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

    private Dag_n fuseAllInputDags(){
        ArrayList<Dag_n> graphs = new ArrayList<>();
        for (CircularProcess cdag: cgesProcesses) {
            graphs.add(cdag.dag);
        }

        ConsensusUnion fusion = new ConsensusUnion(graphs);
        return fusion.union();
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

    private void bestBroadcastingSearch() {
        do {
            // apply circular processes
            List<CircularProcess> bestInputs = cgesProcesses.parallelStream()
                    .map(cdag -> {
                        try {
                            cdag.bestBroadcastingSearch();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return cdag;
                    })
                    // Get the best input for each process
                    .map(process -> getBestInput(process))
                    .collect(Collectors.toList());

            // Set the best input for each process
            setBestInputs(bestInputs);

        } while (notConverged());
    }

    private CircularProcess getBestInput(CircularProcess process) {
        // Get the best input excluding the current process
        return cgesProcesses.stream()
                .filter(p -> !p.equals(process))
                .max(Comparator.comparingDouble(CircularProcess::getBDeu))
                .orElse(null);
    }

    private void setBestInputs(List<CircularProcess> bestInputs) {
        // Set the best input for each process
        IntStream.range(0, cgesProcesses.size())
                .forEach(i -> cgesProcesses.get(i).setInputDag(bestInputs.get(i).dag));
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
            return cgesProcesses.get(numberOfPartitions - 1);
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
        // When any DAG improves the previous best DAG, there is no convergence
        calculateBestGraph();
        boolean max = lastBestBDeu >= bestCircularProcess.getBDeu();        
        lastBestBDeu = bestCircularProcess.getBDeu();
        return !max;
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
