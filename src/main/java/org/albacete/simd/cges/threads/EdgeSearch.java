package org.albacete.simd.cges.threads;

import consensusBN.SubSet;
import edu.cmu.tetrad.graph.Edge;
import java.util.Objects;

public class EdgeSearch implements Comparable<EdgeSearch> {

    public double score;
    public SubSet hSubset;
    public Edge edge;

    public EdgeSearch(double score, SubSet hSubSet, Edge edge) {
        this.score = score;
        this.hSubset = hSubSet;
        this.edge = edge;
    }

    @Override
    public int compareTo(EdgeSearch o) {
        return Double.compare(this.score, (o).score);
    }

    @Override
    public boolean equals(Object other) {
        if(this == other)
            return true;

        if (other instanceof EdgeSearch) {
            EdgeSearch obj = (EdgeSearch) other;
            return obj.edge.equals(this.edge);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.edge);
    }

}
