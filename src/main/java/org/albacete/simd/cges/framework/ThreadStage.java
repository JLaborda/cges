package org.albacete.simd.cges.framework;

import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.FESThread;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;

import java.util.List;
import java.util.Set;

public abstract class ThreadStage extends Stage{
    /**
     * The {@link GESThread GESThread} array that will be executed in each stage.
     * They can either be {@link FESThread ThFES} or {@link BESThread ThBES} threads.
     */
    protected GESThread[] gesThreads;

    /**
     * The {@link Thread Thread} array that encapsulate the {@link GESThread GESThread} for each stage.
     */
    protected Thread[] threads;

    protected boolean flag = false;

    protected int itInterleaving;

    protected int nThreads;

    protected List<Set<Edge>> subsets;

    public ThreadStage(Problem problem, int nThreads, int itInterleaving, List<Set<Edge>> subsets) {
        super(problem);
        this.nThreads = nThreads;
        this.threads = new Thread[nThreads];
        this.gesThreads = new GESThread[nThreads];
        this.itInterleaving = itInterleaving;
        this.subsets = subsets;
    }

    public ThreadStage(Problem problem, Graph currentGraph, int nThreads, int itInterleaving, List<Set<Edge>> subsets) {
        super(problem, currentGraph);
        this.currentGraph = currentGraph;
        this.nThreads = nThreads;
        this.threads = new Thread[nThreads];
        this.gesThreads = new GESThread[nThreads];
        this.itInterleaving = itInterleaving;
        this.subsets = subsets;
    }

    /**
     * Executing the threads for the corresponding stage
     * @throws InterruptedException Exception caused by an external interruption.
     */
    protected void runThreads() throws InterruptedException {
        // Starting the threads
        for (Thread thread: this.threads) {
            thread.start();
        }

        // Getting results
        double score_threads = 0;
        for(int i = 0 ; i< this.threads.length; i++){
            // Joining threads and getting currentGraph
            threads[i].join();
            Graph g = gesThreads[i].getCurrentGraph();

            // Thread Score
            score_threads = score_threads + gesThreads[i].getScoreBDeu();

            // Removing Inconsistencies and transforming it to a DAG
            // THIS THROWS AN ILLEGAL ARGUMENT EXCEPTION
            //pdagToDag(g);
            try {
                //g = SearchGraphUtils.dagFromCPDAG(g);
                Dag_n gdag = Utils.removeInconsistencies(g);
                this.graphs.add(gdag);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error in dagFromCPDAG");
                System.out.println("Trying to convert the graph " + i + " to a DAG");
                System.out.println("The original graph is: " + g);
                System.exit(-1);
            }
            //Dag_n gdag = new Dag_n(g);
            //Dag_n gdag = Utils.removeInconsistencies(g);

            // Adding the new dag to the graph list
            //this.graphs.add(gdag);

            //Debug
            //System.out.println("Graph of Thread " + (i +1) + ": \n" + gdag);

        }
    }



    public boolean checkWorkingStatus() throws InterruptedException {
        for (GESThread g: gesThreads) {
            if (g.getFlag() ){
                return true;
            }
        }
        return false;
    }

    protected GESThread getMaxBDeuThread() {
        GESThread best = gesThreads[0];
        double bdeu = gesThreads[0].getScoreBDeu();

        for (int i = 1; i < gesThreads.length; i++) {
            if (gesThreads[i].getScoreBDeu() > bdeu) {
                bdeu = gesThreads[i].getScoreBDeu();
                best = gesThreads[i];
            }
        }
        return best;
    }

}
