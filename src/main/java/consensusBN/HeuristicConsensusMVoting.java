package consensusBN;

import java.util.ArrayList;
import java.util.LinkedList;

import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.EdgeListGraph_n;
import edu.cmu.tetrad.graph.Endpoint;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import static org.albacete.simd.cges.utils.Utils.pdagToDag;

public class HeuristicConsensusMVoting {

	ArrayList<Node> variables = null;
	Dag_n outputDag = null;
	ArrayList<Dag_n> setOfdags = null;
	double percentage = 1.0;
	double [][] weight = null;
	
	
public HeuristicConsensusMVoting(ArrayList<Dag_n> setOfdags, double percentage) {
		super();
		this.variables = (ArrayList<Node>) setOfdags.get(0).getNodes();
		this.outputDag = null;
		this.setOfdags = setOfdags;
		this.percentage = percentage;
		this.weight = new double[this.variables.size()][this.variables.size()];
		ArrayList<Graph> pdags = new ArrayList<Graph>();
		for(Dag_n g: this.setOfdags){
			Graph pd = new EdgeListGraph_n(new LinkedList<Node>(g.getNodes()));
			for(Edge e: g.getEdges()){
				pd.addEdge(e);
			}
			pdagToDag(pd);
			pdags.add(pd);
		}
	
		for(Graph pd: pdags){
			for(Edge e: pd.getEdges()){
				Node n1 = e.getNode1();
				Node n2 = e.getNode2();
				if(e.isDirected()){
					if(e.getEndpoint1() == Endpoint.ARROW){
						this.weight[variables.indexOf(n2)][variables.indexOf(n1)]+= (double) (1.0/this.setOfdags.size());
					}else{
						this.weight[variables.indexOf(n1)][variables.indexOf(n2)]+= (double) (1.0/this.setOfdags.size());
					}
				}else{
					this.weight[variables.indexOf(n2)][variables.indexOf(n1)]+= (double) (1.0/this.setOfdags.size());
					this.weight[variables.indexOf(n1)][variables.indexOf(n2)]+= (double) (1.0/this.setOfdags.size());
				}
			}
		}
	}

public Dag_n fusion(){
	
	this.outputDag = new Dag_n(variables);
	boolean procced = true;
	int bestEdgei = 0;
	int bestEdgej = 0;
	double maxW = 0.0;
	while(procced){
		for(int i = 0; i<this.variables.size(); i++)
			for(int j = 0; j<this.variables.size(); j++){
				if(this.weight[i][j] >= maxW){
					if((this.weight[i][j] > maxW) || ((this.weight[i][j]==maxW) && (Math.random()>0.5))){
						bestEdgei = i;
						bestEdgej = j;
						maxW = this.weight[i][j];
					}
				}
			}
		if(maxW >= this.percentage){
			if(!this.outputDag.existsDirectedPathFromTo(variables.get(bestEdgej), variables.get(bestEdgei))){
				this.outputDag.addEdge(new Edge(variables.get(bestEdgei),variables.get(bestEdgej),Endpoint.TAIL,Endpoint.ARROW));
				this.weight[bestEdgei][bestEdgej] = 0;
			}else this.weight[bestEdgei][bestEdgej] = 0;
			if(maxW==0) procced = false;
			maxW = 0.0;
		}else procced = false;
	}
	
	return this.outputDag;
}

		
}
