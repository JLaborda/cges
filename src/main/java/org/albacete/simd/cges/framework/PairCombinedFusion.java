package org.albacete.simd.cges.framework;

import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.bnbuilders.CircularProcess;
import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Problem;

import java.util.ArrayList;

public class PairCombinedFusion extends FusionStage{

    public PairCombinedFusion(Problem problem, Graph currentGraph, ArrayList<Dag_n> graphs) {
        super(problem, currentGraph, graphs);
    }

    @Override
    public Dag_n fusion() throws InterruptedException {

        Dag_n result = graphs.parallelStream()
                // Apply a fusion to each pair of dags.
                .map(dag -> {
                    ArrayList<Dag_n> pairDags = new ArrayList<>();
                    pairDags.add(dag);
                    pairDags.add(new Dag_n(currentGraph));
                    ConsensusUnion fusion = new ConsensusUnion(pairDags);
                    return fusion.union();})
                // Calculate the score of each graph and keep the best graph
                .reduce(((dag1, dag2) -> {
                    double score1 = GESThread.scoreGraph(dag1, problem);
                    double score2 = GESThread.scoreGraph(dag2, problem);
                    if(score1 > score2)
                        return dag1;
                    else
                        return dag2;
                })).orElse(null);

        if(result == null)
            return null;
        // Do the BESThread to complete the GES of the fusion
        BESThread bes = new BESThread(problem, result, result.getEdges());
        bes.run();

        return CircularProcess.transformPDAGtoDAG(bes.getCurrentGraph());

    }

}
