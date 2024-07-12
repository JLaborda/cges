package org.albacete.simd.cges.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.framework.BNBuilder;
import org.albacete.simd.cges.threads.BESThread;
import org.albacete.simd.cges.threads.FESThread;
import org.albacete.simd.cges.utils.Utils;

import java.util.LinkedList;

public class GES extends BNBuilder {

    private final boolean parallel;

    public GES(DataSet data, boolean parallel) {
        super(data, 1);
        initialGraph = new EdgeListGraph(new LinkedList<>(problem.getVariables()));
        this.parallel = parallel;
    }

    public GES(String path, boolean parallel) {
        super(path, 1);
        initialGraph = new EdgeListGraph(new LinkedList<>(problem.getVariables()));
        this.parallel = parallel;
    }

    public GES(Graph initialDag, DataSet data, boolean parallel) {
        this(data, parallel);
        this.initialGraph = new EdgeListGraph(initialDag);
        this.currentGraph = new EdgeListGraph(initialDag);
    }

    public GES(Graph initialDag, String path, boolean parallel) {
        super(initialDag,path,1);
        this.parallel = parallel;
        this.currentGraph = new EdgeListGraph(initialDag);
    }

    private void forwardStage() throws InterruptedException {
        FESThread fes = new FESThread(problem, initialGraph, setOfArcs, Integer.MAX_VALUE, false, false, parallel);
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
