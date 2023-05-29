package org.albacete.simd.cges.framework;


import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Graph;
import org.albacete.simd.cges.utils.Problem;

import java.util.ArrayList;

// IDEAS para el futuro
public abstract class Stage {

    protected Problem problem;

    Graph currentGraph;

    /**
     * {@link ArrayList ArrayList} of graphs. This contains the list of {@link Graph graphs} created for each stage,
     * just before the fusion is done.
     */
    protected ArrayList<Dag_n> graphs = null;

    public Stage(Problem problem){
        this.problem = problem;
        this.currentGraph = null;
    }

    public Stage(Problem problem, Graph currentGraph){
        this.problem = problem;
        this.currentGraph = currentGraph;
    }


    public abstract boolean run() throws InterruptedException;


    public ArrayList<Dag_n> getGraphs() {
        return graphs;
    }

    public Graph getCurrentGraph(){
        return currentGraph;
    }
}
