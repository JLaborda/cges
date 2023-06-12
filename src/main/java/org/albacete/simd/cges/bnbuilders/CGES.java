package org.albacete.simd.cges.bnbuilders;

import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.clustering.Clustering;
import org.albacete.simd.cges.framework.BNBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.FESThread;
import org.albacete.simd.cges.utils.Utils;


public class CGES extends BNBuilder {
    
    public static final String EXPERIMENTS_FOLDER = "./experiments/";
    private final String typeConvergence;
    
    private List<Set<Edge>> subsetEdges;
    private final Clustering clustering;
    private final HashMap<Integer, CircularDag> circularFusionThreadsResults;
    private CircularDag bestDag;
    private double lastBestBDeu = Double.NEGATIVE_INFINITY;
    private boolean convergence;

    private boolean broadcasting = true;

    /**
     * Resulting fusion from all the graphs of the processes for the current iteration
     */
    private Dag_n fusionDag;

    
    public CGES(DataSet data, Clustering clustering, int nThreads, int nItInterleaving, String typeConvergence) {
        super(data, nThreads, -1, nItInterleaving);

        this.clustering = clustering;
        clustering.setProblem(problem);
        this.circularFusionThreadsResults = new HashMap<>(nThreads);
        this.typeConvergence = typeConvergence;
    }

    public CGES(String path, Clustering clustering, int nThreads, int nItInterleaving, String typeConvergence) {
        this(Utils.readData(path), clustering, nThreads, nItInterleaving, typeConvergence);
    }
    
    
    @Override
    public Graph search(){
        //1. Setup
        initialConfig();

        //2. Do circular fusion while convergence is false
        if (broadcasting) {
            broadcastingLoop();
        } else {
            noBroadcastingLoop();
        }

        //3. Print and return last graph
        printResults();
        calculateBestGraph();
        currentGraph = bestDag.dag;
        
        //4. Do a final GES with all the data
        System.out.println("\n\n\n FINAL GES");
        finalGES();
        
        return currentGraph;
    }

    protected void initialConfig() {
        it = 0;
        repartition();
        initializeValuesInResultsMap();
    }

    /**
     * Splits the edges of the problem into the same amount of partitions as threads used for the algorithm.
     */
    protected void repartition() {
        // Splitting edges with the clustering algorithm and assigning them to the subsetEdges attribute.
        clustering.setProblem(this.problem);
        subsetEdges = clustering.generateEdgeDistribution(nThreads);
    }

    private void initializeValuesInResultsMap(){
        for (int i = 0; i < nThreads; i++) {
            circularFusionThreadsResults.put(i, new CircularDag(problem,subsetEdges.get(i),nItInterleaving,i));
        }
    }

    private void noBroadcastingLoop(){
        do{
            it++;
            putInputGraphs();
            circularFusionThreadsResults.values().parallelStream().forEach((dag) -> {
                try {
                    // Applying cges process
                    dag.fusionGES();
                } catch (InterruptedException ex) {
                    System.out.println("Error with InterruptedException: " +
                            "\n Dag_n Id: " + dag.id +
                            "\n Dag_n graph: " + dag.dag);
                }
            });
        } while (!convergence());
    }

    private void broadcastingLoop() {
        do{
            it++;
            putInputGraphs();
            fuseAllInputDags();
            circularFusionThreadsResults.values().parallelStream().forEach((dag) -> {
                try {
                    //Broadcasting
                    dag.setAllFusedDag(this.fusionDag);
                    // Applying cges process
                    dag.fusionGES();
                } catch (InterruptedException ex) {
                    System.out.println("Error with InterruptedException: " +
                            "\n Dag_n Id: " + dag.id +
                            "\n Dag_n graph: " + dag.dag);
                }
            });
        } while (!convergence());
    }
    
    private void putInputGraphs() {
        circularFusionThreadsResults.values().forEach((dag) -> {
            CircularDag cd = getInputDag(dag.id);
            dag.setInputDag(cd.dag);
        });
    }

    private void fuseAllInputDags(){
        ArrayList<Dag_n> graphs = new ArrayList<>();
        for (CircularDag cdag: circularFusionThreadsResults.values()) {
            graphs.add(cdag.dag);
        }

        ConsensusUnion fusion = new ConsensusUnion(graphs);
        this.fusionDag = fusion.union();
    }

    private CircularDag getInputDag(int i) {
        if(i == 0) {
            return circularFusionThreadsResults.get(nThreads - 1);
        } else {
            return circularFusionThreadsResults.get(i - 1);
        }
    }

    public void calculateBestGraph(){
        circularFusionThreadsResults.values().forEach(this::calculateBestGraph);
    }
    
    public void calculateBestGraph(CircularDag dag){
        if (bestDag == null)
            bestDag = dag;
        else{
            if (dag.getBDeu() > bestDag.getBDeu())
                bestDag = dag;
        }
    }

    private boolean convergence() {
        switch (typeConvergence) {
            // When any DAG changes in the iteration, there is no convergence
            case "c1":
            default:
                convergence = true;
                
                circularFusionThreadsResults.values().forEach((dag) -> {
                    convergence = convergence && dag.convergence;
                });
                
                return convergence;
                
            // When any DAG improves the previous best DAG, there is no convergence
            case "c2":
                calculateBestGraph();

                boolean max = lastBestBDeu >= bestDag.getBDeu();
                
                lastBestBDeu = bestDag.getBDeu();
                        
                return max;
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
        } catch (InterruptedException ex) {}
    }
    
    public void printResults() {
        String savePath = EXPERIMENTS_FOLDER + "results.csv";
        File file = new File(savePath);
        FileWriter csvWriter = null;
        try {
            csvWriter = new FileWriter(file,true);
            csvWriter.append("id,stage,BDeu\n");
            
            for (int i = 0; i < nThreads; i++) {
                File doc = new File(EXPERIMENTS_FOLDER + "temp_" + i + ".csv");
                Scanner obj = new Scanner(doc);

                while (obj.hasNextLine()) {
                    csvWriter.append(obj.nextLine() + "\n");
                }
            }

            csvWriter.flush();
        } catch (IOException ex) {}
    }

    public boolean isBroadcasting() {
        return broadcasting;
    }

    public void setBroadcasting(boolean broadcasting) {
        this.broadcasting = broadcasting;
    }
}
