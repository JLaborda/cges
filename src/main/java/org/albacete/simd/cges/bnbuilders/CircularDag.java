package org.albacete.simd.cges.bnbuilders;

import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.framework.FESFusion;
import org.albacete.simd.cges.framework.PairCombinedFusion;
import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.FESThread;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Problem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.albacete.simd.cges.utils.Utils.pdagToDag;

public class CircularDag {
    public Dag_n dag;
    public final int id;
    public boolean convergence = false;
    
    private double bdeu = Double.NEGATIVE_INFINITY;
    private double lastBdeu;
    private final Problem problem;
    private final Set<Edge> subsetEdges;
    private final int nItInterleaving;

    /**
     * Input Dag used in the original implementation of CGES. This is the graph that comes from the previous process.
     */
    private Dag_n inputDag;

    private ArrayList<Dag_n> inputDags;

    private Dag_n allFusedDag = null;

    public static int fusionWinCounter = 0;

    
    public static final String EXPERIMENTS_FOLDER = "./experiments/";

    public CircularDag(Problem problem, Set<Edge> subsetEdges, int nItInterleaving, int id) {
        this.id = id;
        this.problem = problem;
        this.subsetEdges = subsetEdges;
        this.nItInterleaving = nItInterleaving;
        this.dag = new Dag_n(problem.getVariables());
    }

    public void noBroadcastingSearch() throws InterruptedException {
        // Setup
        // 1. Update bdeu and convergence variables
        setup();
        
        // 2. Check if the input dag is empty
        if (!inputDag.getEdges().isEmpty()) {
            // 3. Merge dags into an arraylist
            ArrayList<Dag_n> dags = mergeBothDags(inputDag);

            // 4. FES Fusion (Consensus Fusion + FES)
            applyFESFusion(dags);
        }
        
        // 5. GES Stage
        applyGES();

        // 6. Update bdeu value
        updateResults();

        // 7. Convergence
        checkConvergence();
    }

    public void allFusedBroadcastingSearch() throws InterruptedException{
        // Setup
        // 1. Update bdeu and convergence variables
        setup();

        // 2. Check if the input dag is empty
        if (!inputDag.getEdges().isEmpty()) {
            // 3. Merge dags into an arraylist
            ArrayList<Dag_n> dags = mergeBothDags(inputDag);

            // 4. FES Fusion (Consensus Fusion + FES)
            applyFESFusion(dags);
        }

        // 5. GES Stage
        applyGES();

        // 6. Checking if there is broadcasting
        checkBroadcasting();

        // 7. Update bdeu value
        updateResults();

        // 8. Convergence
        checkConvergence();
    }

    public void pairBroadcastSearch() throws InterruptedException {
        // 1. Update bdeu and restart convergence flag.
        setup();

        // 2. Check if there are input dags
        if(!this.inputDags.isEmpty()){
            //3. Apply pairCombinedFusion
            PairCombinedFusion pairCombinedFusion = new PairCombinedFusion(problem, this.dag, inputDags);
            dag = pairCombinedFusion.fusion();

        }
        //4. Apply GES Stage
        applyGES();

        // 6. Update bdeu value
        updateResults();

        // 7. Convergence
        checkConvergence();

    }

    
    private void setup() {
        lastBdeu = bdeu;
        convergence = false;
    }
    
    private ArrayList<Dag_n> mergeBothDags(Dag_n dag2) {
        ArrayList<Dag_n> dags = new ArrayList<>();
        dags.add(dag);
        dags.add(dag2);
        return dags;
    }
    
    private void applyFESFusion(ArrayList<Dag_n> dags) throws InterruptedException {
        FESFusion fusion = new FESFusion(problem, dag, dags, true);
        dag = fusion.fusion();
        printResults(id, "Fusion", GESThread.scoreGraph(dag, problem));
        
        // Do the BESThread to complete the GES of the fusion
        BESThread bes = new BESThread(problem, dag, dag.getEdges());
        bes.run();

        dag = transformPDAGtoDAG(bes.getCurrentGraph());
    }

    private void printResults(int id, String stage, double BDeu) {
        String savePath = EXPERIMENTS_FOLDER + "temp_" + id + ".csv";
        File file = new File(savePath);
        FileWriter csvWriter = null;
        try {
            csvWriter = new FileWriter(file, true);
            csvWriter.append(id + "," + stage + "," + BDeu + "\n");
            csvWriter.flush();
        } catch (IOException ex) {
        }
    }

    private void applyGES() throws InterruptedException {
        // Do the FESThread
        FESThread fes = new FESThread(problem, dag, subsetEdges, this.nItInterleaving, false, true, true);
        fes.run();
        Graph fesGraph = fes.getCurrentGraph();
        fesGraph = transformPDAGtoDAG(fesGraph);
        printResults(id, "FES", GESThread.scoreGraph(fesGraph, problem));
    
        // Do the BESThread to complete the GES of the fusion
        BESThread bes = new BESThread(problem, fesGraph, subsetEdges);
        bes.run();

        dag = transformPDAGtoDAG(bes.getCurrentGraph());
    }

    private Dag_n transformPDAGtoDAG(Graph besGraph) {
        pdagToDag(besGraph);
        return new Dag_n(besGraph);
    }

    /**
     * Checking which graph is better. The one from the cges or the one from the allFused graph.
     */
    private void checkBroadcasting(){
        //Checking if there is broadcasting
        if(allFusedDag == null)
            return;
        // Calculating scores
        double bdeuCDAG = GESThread.scoreGraph(dag, problem);
        double bdeuAllFusedDag = GESThread.scoreGraph(allFusedDag, problem);

        // Changing dag to the best dag
        if(bdeuAllFusedDag > bdeuCDAG){
            this.dag = new Dag_n(allFusedDag);
            incrementFusionWinCounter();
        }

    }

    public synchronized void incrementFusionWinCounter(){
        fusionWinCounter++;
    }
    
    private void updateResults() {
        bdeu = GESThread.scoreGraph(dag, problem);
        printResults(id, "BES", bdeu);
    }

    private void checkConvergence() {
        if (bdeu <= lastBdeu) convergence = true;
    }
    
    public double getBDeu() {
        return bdeu;
    }

    public void setInputDag(Dag_n inputDag) {
        this.inputDag = inputDag;
    }

    public void setAllFusedDag(Dag_n allFusedDag) {
        this.allFusedDag = allFusedDag;
    }

    public void setInputDags(ArrayList<Dag_n> inputDags) {
        this.inputDags = inputDags;
    }
}
