package org.albacete.simd.cges.framework;

import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Edges;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BESFusion extends FusionStage{
    
    final ThreadStage besStage;
    
    public BESFusion(Problem problem, Graph currentGraph, ArrayList<Dag_n> graphs, BESStage besStage) {
        super(problem, currentGraph, graphs);
        this.besStage = besStage;
    }
    
    public boolean flag = false;

    @Override
    protected Dag_n fusion() {
        Dag_n fusionGraph = this.fusionIntersection();

        System.out.println("BES to obtain the fusion: ");

        Set<Edge> candidates = new HashSet<>();

        for (Edge e: this.currentGraph.getEdges()){
            if(fusionGraph.getEdge(e.getNode1(), e.getNode2())==null && fusionGraph.getEdge(e.getNode2(),e.getNode1())==null ) {
                candidates.add(Edges.directedEdge(e.getNode1(),e.getNode2()));
                candidates.add(Edges.directedEdge(e.getNode2(),e.getNode1()));
            }
        }

        BESThread fuse = new BESThread(this.problem,this.currentGraph,candidates);

        fuse.run();
        
        // We obtain the flag of the BES. If true, BESThread has improved the result.
        try {
            flag = fuse.getFlag();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        
        // If the BESThread has not improved the previous result, we check if the fusion improves it.
        if (!flag) {
            double fusionScore = GESThread.scoreGraph(fusionGraph, problem);
            double currentScore = GESThread.scoreGraph(this.currentGraph, problem);
            
            if (fusionScore > currentScore) {
                flag = true;
                this.currentGraph = fusionGraph;
                return (Dag_n) this.currentGraph;
            } 
            
            // If the fusion doesn't improve the result, we check if any previous BESThread has improved the results.
            else {
                GESThread thread = besStage.getMaxBDeuThread();
                System.out.println("thread"  + thread);
                if (thread.getScoreBDeu() != 0 && thread.getScoreBDeu() > currentScore) {
                    try {
                        this.currentGraph = thread.getCurrentGraph();
                        System.out.println(this.currentGraph);
                        flag = true;
                    } catch (InterruptedException ex) {
                        System.out.println("\n\n\n EXCEPTION º\n\n\n");}
                    return (Dag_n) this.currentGraph;
                }
            }
        }
        
        try {
            this.currentGraph = fuse.getCurrentGraph();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        Utils.pdagToDag(this.currentGraph);
        return new Dag_n(this.currentGraph);
        //return Utils.removeInconsistencies(this.currentGraph);
    }
}
