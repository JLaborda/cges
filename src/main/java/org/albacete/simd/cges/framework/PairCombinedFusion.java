package org.albacete.simd.cges.framework;

import consensusBN.ConsensusUnion;
import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.bnbuilders.CircularProcess;
import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.GESThread;
import org.albacete.simd.cges.utils.Problem;
import java.util.AbstractMap;

import java.util.ArrayList;
import java.util.Map;

public class PairCombinedFusion extends FusionStage{

    public PairCombinedFusion(Problem problem, Graph currentGraph, ArrayList<Dag_n> graphs) {
        super(problem, currentGraph, graphs);
    }

    @Override
    public Dag_n fusion() throws InterruptedException {

        Dag_n result = graphs.parallelStream()
                // Check if the graph is the current one
                .filter(dag -> !dag.equals(currentGraph))
                // Apply the fusion and calculate its complexity
                .map(dag -> {
                    ArrayList<Dag_n> pairDags = new ArrayList<>();
                    pairDags.add(dag);
                    pairDags.add(new Dag_n(currentGraph));
                    ConsensusUnion fusion = new ConsensusUnion(pairDags);
                    Dag_n fusedDag = fusion.union();
                    double complexity = fusion.getNumberOfInsertedEdges();
                    return new AbstractMap.SimpleEntry<>(fusedDag, complexity);//fusion.union();
                })
                // Get the one with the lowest complexity
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if(result == null)
            return null;
        // Do the BESThread to complete the GES of the fusion
        BESThread bes = new BESThread(problem, result, result.getEdges());
        bes.run();

        return CircularProcess.transformPDAGtoDAG(bes.getCurrentGraph());

    }

}
