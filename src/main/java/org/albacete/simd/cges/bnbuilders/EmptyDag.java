package org.albacete.simd.cges.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.BDeuScore;
import edu.cmu.tetrad.search.Fges;
import java.util.LinkedList;

import org.albacete.simd.cges.framework.BNBuilder;
import org.albacete.simd.cges.utils.Utils;

public class EmptyDag extends BNBuilder {
    
    public EmptyDag(DataSet data) {
        super(data, 1, -1, -1);
    }

    public EmptyDag(String path) {
        this(Utils.readData(path));
    }

    @Override
    public Graph search(){
        BDeuScore bdeu = new BDeuScore(this.getData());
        Fges fges = new Fges(bdeu);
        currentGraph = new EdgeListGraph(new LinkedList<>(problem.getVariables()));
        this.score = fges.scoreDag(currentGraph);
        return this.currentGraph;
    }

}
