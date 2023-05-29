package org.albacete.simd.cges.framework;

import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.threads.BackwardsHillClimbingThread;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BHCStage extends BackwardStage {


    public BHCStage(Problem problem, Graph currentGraph, int nThreads, int itInterleaving, List<Set<Edge>> subsets) {
        super(problem, currentGraph, nThreads, itInterleaving, subsets);
    }

    private void config() {
        // Initializing Graphs structure
        this.graphs = new ArrayList<>();
        this.gesThreads = new GESThread[this.nThreads];

        // Rebuilding hashIndex
        //problem.buildIndexing(currentGraph);

        // Rearranging the subsets, so that the BES stage only deletes edges of the current graph.
        List<Set<Edge>> subsets_BHC = Utils.split(this.currentGraph.getEdges(), this.nThreads);
        for (int i = 0; i < this.nThreads; i++) {
            this.gesThreads[i] = new BackwardsHillClimbingThread(this.problem, this.currentGraph, subsets_BHC.get(i));
        }

        // Initializing thread config
        for(int i = 0 ; i< this.nThreads; i++){
            // Resetting the  search flag
            this.gesThreads[i].resetFlag();
            this.threads[i] = new Thread(this.gesThreads[i]);
        }
    }

    @Override
    public boolean run() {
        config();
        try {
            runThreads();
            flag = checkWorkingStatus();
            return flag;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}
