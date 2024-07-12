package org.albacete.simd.cges.framework;

import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.utils.Problem;

import java.util.List;
import java.util.Set;

public abstract class BackwardStage extends ThreadStage{

    public BackwardStage(Problem problem, Graph currentGraph, int nThreads, int itInterleaving, List<Set<Edge>> subsets) {
        super(problem, currentGraph, nThreads, itInterleaving, subsets);
    }


}
