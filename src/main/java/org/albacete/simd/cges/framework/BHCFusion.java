package org.albacete.simd.cges.framework;

import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Edges;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.threads.BackwardsHillClimbingThread;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Problem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class BHCFusion extends FusionStage{

    public BHCFusion(Problem problem, Graph currentGraph, ArrayList<Dag_n> graphs) {
        super(problem, currentGraph, graphs);
    }

    @Override
    protected Dag_n fusion() throws InterruptedException {

        Dag_n fusionGraph = this.fusionIntersection();

        // Getting Scores
        double fusionScore = GESThread.scoreGraph(fusionGraph, problem);
        double currentScore = GESThread.scoreGraph(this.currentGraph, problem);

        System.out.println("Fusion Score: " + fusionScore);
        System.out.println("Current Score: " + currentScore);


        // Checking if the score has improved
        if (fusionScore > currentScore) {
            this.currentGraph = fusionGraph;
            return (Dag_n) this.currentGraph;
        }

        System.out.println("BHC to obtain the fusion: ");

        Set<Edge> candidates = new HashSet<>();

        for (Edge e: this.currentGraph.getEdges()){
            if(fusionGraph.getEdge(e.getNode1(), e.getNode2())==null && fusionGraph.getEdge(e.getNode2(),e.getNode1())==null ) {
                candidates.add(Edges.directedEdge(e.getNode1(),e.getNode2()));
                candidates.add(Edges.directedEdge(e.getNode2(),e.getNode1()));
            }
        }
        // Quizás sea mejor poner el BES
        //BESThread fuse = new BESThread(this.problem, this.currentGraph, candidates);
        BackwardsHillClimbingThread fuse = new BackwardsHillClimbingThread(this.problem,this.currentGraph,candidates);

        fuse.run();

        this.currentGraph = fuse.getCurrentGraph();
        System.out.println("Resultado del BHC de la fusion: "+ BackwardsHillClimbingThread.scoreGraph(this.currentGraph, problem));
        //this.currentGraph = Utils.removeInconsistencies(this.currentGraph);
        //System.out.println("Resultado del BHC de la fusion tras removeInconsistencies: "+ BackwardsHillClimbingThread.scoreGraph(this.currentGraph, problem));

        return new Dag_n(this.currentGraph);

    }
}
