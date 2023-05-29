package org.albacete.simd.cges.framework;

import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.threads.ForwardHillClimbingThread;
import org.albacete.simd.cges.utils.Problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FHCStage extends ForwardStage {

    public FHCStage(Problem problem, int nThreads, int itInterleaving, List<Set<Edge>> subsets) {
        super(problem, nThreads, itInterleaving, subsets);
    }

    public FHCStage(Problem problem, Graph currentGraph, int nThreads, int itInterleaving, List<Set<Edge>> subsets) {
        super(problem, currentGraph, nThreads, itInterleaving, subsets);
    }

    private void config() {
        // Initializing Graphs structure
        this.graphs = new ArrayList<>();

        // Rebuilding hashIndex
        //problem.buildIndexing(currentGraph);

        // Creating each ThFES runnable
        if (this.currentGraph == null) {
            for (int i = 0; i < this.nThreads; i++) {
                //System.out.println("Index: " + i);
                this.gesThreads[i] = new ForwardHillClimbingThread(this.problem,this.subsets.get(i), this.itInterleaving);
            }
        }
        else{
            for (int i = 0; i < this.nThreads; i++) {
                this.gesThreads[i] = new ForwardHillClimbingThread(this.problem, this.currentGraph, this.subsets.get(i), this.itInterleaving);
            }
        }

        // Initializing thread config
        for(int i = 0 ; i< this.nThreads; i++){
            // Resetting the search flag
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
