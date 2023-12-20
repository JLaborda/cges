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
import java.util.Set;
import static org.albacete.simd.cges.utils.Utils.pdagToDag;

public class CircularProcess {
    public Dag_n dag;
    public final int id;
    public boolean convergence = false;
    
    private double score = Double.NEGATIVE_INFINITY;
    private double lastScore;
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

    public static boolean saveStages = false;

    public CircularProcess(Problem problem, Set<Edge> subsetEdges, int nItInterleaving, int id) {
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

        //2. Apply pairCombinedFusion
        if(this.dag != null && !this.dag.getEdges().isEmpty()) {
            PairCombinedFusion pairCombinedFusion = new PairCombinedFusion(problem, this.dag, inputDags);
            dag = pairCombinedFusion.fusion();
        }
        //3. Apply GES Stage
        applyGES();

        // 4. Update bdeu value
        updateResults();

        // 5. Convergence
        checkConvergence();
    }

    
    private void setup() {
        lastScore = score;
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
        if(!saveStages)
            return;
        String savePath = EXPERIMENTS_FOLDER + "temp_" + id + ".csv";
        File file = new File(savePath);
        try (FileWriter csvWriter = new FileWriter(file, true)){
            csvWriter.append(String.valueOf(id)).append(",").append(stage).append(",").append(String.valueOf(BDeu)).append("\n");
            csvWriter.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
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

    public static Dag_n transformPDAGtoDAG(Graph besGraph) {
        pdagToDag(besGraph);
        return new Dag_n(besGraph);
    }

    /**
     * Checking which graph is better. The one from the cges or the one from the allFused graph.
     */
    private void checkBroadcasting(){
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
        score = GESThread.scoreGraph(dag, problem);
        printResults(id, "BES", score);
    }

    private void checkConvergence() {
        if (score <= lastScore) convergence = true;
    }
    
    public double getBDeu() {
        return score;
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
