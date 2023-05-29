package org.albacete.simd.cges.clustering;

import edu.cmu.tetrad.graph.Edge;
import org.albacete.simd.cges.utils.Problem;

import java.util.List;
import java.util.Set;

public abstract class Clustering {
    protected Problem problem;

    public Clustering(){

    }

    public Clustering(Problem problem){
        this.problem = problem;
    }

    public abstract List<Set<Edge>> generateEdgeDistribution(int numClusters);

    public void setProblem(Problem problem) {
        this.problem = problem;
    }
}
