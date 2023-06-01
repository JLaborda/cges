package org.albacete.simd.cges.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.framework.ForwardStage;
import org.albacete.simd.cges.framework.BNBuilder;
import org.albacete.simd.cges.framework.BackwardStage;
import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.FESThread;
import org.albacete.simd.cges.utils.Problem;
import org.albacete.simd.cges.utils.Utils;

import java.util.LinkedList;
import java.util.Set;

public class GES extends BNBuilder {
    private Graph initialDag;
    private final boolean parallel;

    public GES(DataSet data, boolean parallel) {
        super(data, 1, -1, -1);
        initialDag = new EdgeListGraph(new LinkedList<>(problem.getVariables()));
        this.parallel = parallel;
    }

    public GES(String path, boolean parallel) {
        super(path, 1, -1, -1);
        initialDag = new EdgeListGraph(new LinkedList<>(problem.getVariables()));
        this.parallel = parallel;
    }

    public GES(Graph initialDag, DataSet data, boolean parallel) {
        this(data, parallel);
        this.initialDag = new EdgeListGraph(initialDag);
        this.currentGraph = new EdgeListGraph(initialDag);
    }

    public GES(Graph initialDag, String path, boolean parallel) {
        this(path, parallel);
        this.initialDag = new EdgeListGraph(initialDag);
        this.currentGraph = new EdgeListGraph(initialDag);
    }

    private void forwardStage() throws InterruptedException {
        FESThread fes = new FESThread(problem, initialDag, setOfArcs, Integer.MAX_VALUE, false, false, parallel);
        fes.run();
        currentGraph = fes.getCurrentGraph();
        score = fes.getScoreBDeu();
    }

    private void backwardStage() throws InterruptedException {
        BESThread bes = new BESThread(problem, currentGraph, setOfArcs);
        bes.run();
        currentGraph = bes.getCurrentGraph();
        score = bes.getScoreBDeu();
        currentGraph = Utils.removeInconsistencies(currentGraph);
    }

    @Override
    public Graph search(){
        try {
            forwardStage();
            backwardStage();
        }catch(InterruptedException e){
            System.err.println("Interrupted Exception");
            e.printStackTrace();
        }
        return this.currentGraph;
    }

}
