package org.albacete.simd.cges.clustering;

import edu.cmu.tetrad.graph.Edge;
import org.albacete.simd.cges.utils.Utils;

import java.util.List;
import java.util.Set;

public class RandomClustering extends Clustering{

    public RandomClustering(){
        this(42);
    }

    public RandomClustering( long seed){
        Utils.setSeed(seed);
    }

    @Override
    public List<Set<Edge>> generateEdgeDistribution(int numClusters) {
        return Utils.split(Utils.calculateArcs(problem.getData()), numClusters);
    }
}
