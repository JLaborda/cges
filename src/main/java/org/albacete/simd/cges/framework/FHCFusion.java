package org.albacete.simd.cges.framework;

import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Edges;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.threads.ForwardHillClimbingThread;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Problem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FHCFusion extends FusionStage{

    public FHCFusion(Problem problem, Graph currentGraph, ArrayList<Dag_n> graphs) {
        super(problem, currentGraph, graphs);
    }

    @Override
    protected Dag_n fusion() throws InterruptedException {
        // Applying ConsensusUnion fusion
        ConsensusUnion fusion = new ConsensusUnion(this.graphs);
        Graph fusionGraph = fusion.union();

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

        System.out.println("FHC to obtain the fusion: ");


        Set<Edge> candidates = new HashSet<>();


        for (Edge e: fusionGraph.getEdges()){
            if(this.currentGraph.getEdge(e.getNode1(), e.getNode2())!=null || this.currentGraph.getEdge(e.getNode2(),e.getNode1())!=null ) continue;
            candidates.add(Edges.directedEdge(e.getNode1(),e.getNode2()));
            candidates.add(Edges.directedEdge(e.getNode2(),e.getNode1()));
        }


        //FESThread fuse = new FESThread(this.problem,this.currentGraph,candidates,candidates.size());
        ForwardHillClimbingThread fuse = new ForwardHillClimbingThread(problem, this.currentGraph, candidates, candidates.size());

        fuse.run();


        this.currentGraph = fuse.getCurrentGraph();
        System.out.println("Score Fusion: "+ ForwardHillClimbingThread.scoreGraph(this.currentGraph, problem));
        //this.currentGraph = Utils.removeInconsistencies(this.currentGraph);
        //System.out.println("Score Fusion sin inconsistencias: "+ ForwardHillClimbingThread.scoreGraph(this.currentGraph, problem));


        return new Dag_n(this.currentGraph);
    }
}
