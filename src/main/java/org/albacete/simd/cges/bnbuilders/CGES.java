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
    private final List<CircularDag> cgesProcesses;
    private CircularDag bestDag;
    private double lastBestBDeu = Double.NEGATIVE_INFINITY;
    private boolean convergence;

    public enum Broadcasting {NO_BROADCASTING, PAIR_BROADCASTING, ALL_BROADCASTING};

    private Broadcasting typeBroadcasting;
    
    public CGES(DataSet data, Clustering clustering, int nThreads, int nItInterleaving, String typeConvergence, Broadcasting typeBroadcasting) {
        super(data, nThreads, -1, nItInterleaving);

        this.clustering = clustering;
        clustering.setProblem(problem);
        this.cgesProcesses = new ArrayList<>(nThreads);
        this.typeConvergence = typeConvergence;
        this.typeBroadcasting = typeBroadcasting;
    }

    public CGES(String path, Clustering clustering, int nThreads, int nItInterleaving, String typeConvergence, Broadcasting typeBroadcasting) {
        this(Utils.readData(path), clustering, nThreads, nItInterleaving, typeConvergence, typeBroadcasting);
    }
    
    
    @Override
    public Graph search(){
        //1. Setup
        initialConfig();

        //2. Do circular fusion while convergence is false
         switch(typeBroadcasting) {
             case NO_BROADCASTING:
                 this.noBroadcastingSearch();
                 break;
             case PAIR_BROADCASTING:
                 this.pairBroadcastingSearch();
                 break;
             case ALL_BROADCASTING:
                 this.allBroadcastingSearch();
                 break;
             default:
                 System.out.println("Unknown broadcasting type. Ending program");
                 return null;
         }

        //3. Print and return last graph
        //printResults();
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
        initializeThreads();
    }

    /**
     * Splits the edges of the problem into the same amount of partitions as threads used for the algorithm.
     */
    protected void repartition() {
        // Splitting edges with the clustering algorithm and assigning them to the subsetEdges attribute.
        clustering.setProblem(this.problem);
        subsetEdges = clustering.generateEdgeDistribution(nThreads);
    }

    /**
     * Initializes each CGES process with a starting empty graph
     */
    private void initializeThreads(){
        for (int i = 0; i < nThreads; i++) {
            cgesProcesses.add(new CircularDag(problem,subsetEdges.get(i),nItInterleaving,i));
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
                    System.out.println("Error with InterruptedException: " +
                            "\n Dag_n Id: " + cdag.id +
                            "\n Dag_n graph: " + cdag.dag);
                }
            });
        } while (!convergence());
    }

    private void putInputGraphs() {
        cgesProcesses.forEach((dag) -> {
            CircularDag cd = getInputDag(dag.id);
            dag.setInputDag(cd.dag);
        });
    }

    private Dag_n fuseAllInputDags(){
        ArrayList<Dag_n> graphs = new ArrayList<>();
        for (CircularDag cdag: cgesProcesses) {
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
                    System.out.println("Error with InterruptedException: " +
                            "\n Dag_n Id: " + cdag.id +
                            "\n Dag_n graph: " + cdag.dag);
                }
            });
        } while (!convergence());
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
                    System.out.println("Error with InterruptedException: " +
                    "\n Dag_n Id: " + cdag.id +
                    "\n Dag_n graph: " + cdag.dag);
                    throw new RuntimeException(e);
                }
            });



        }while(!convergence());
    }

    private void addInputDagList() {
        ArrayList<Dag_n> inputDags = getInputDags();
        cgesProcesses.forEach((cges) -> {
            cges.setInputDags(inputDags);
        });
    }

    private ArrayList<Dag_n> getInputDags() {
        ArrayList<Dag_n> dags = new ArrayList<>(cgesProcesses.size());
        for (CircularDag cges: cgesProcesses) {
            dags.add(new Dag_n(cges.dag));
        }
        return dags;
    }


    private CircularDag getInputDag(int i) {
        if(i == 0) {
            return cgesProcesses.get(nThreads - 1);
        } else {
            return cgesProcesses.get(i - 1);
        }
    }

    public void calculateBestGraph(){
        cgesProcesses.forEach(this::calculateBestGraph);
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
                
                cgesProcesses.forEach((dag) -> {
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

    public Broadcasting getTypeBroadcasting() {
        return typeBroadcasting;
    }

    public void setTypeBroadcasting(Broadcasting typeBroadcasting) {
        this.typeBroadcasting = typeBroadcasting;
    }
}
