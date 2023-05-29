package consensusBN;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.tetrad.graph.Dag_n;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Endpoint;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;



public class ConsensusUnion implements Runnable{
	
	ArrayList<Node> alpha = null;
	Dag_n outputDag = null;
	AlphaOrder heuristic = null;
	TransformDags imaps2alpha = null;
	ArrayList<Dag_n> setOfdags = null;
	Dag_n union = null;
	int numberOfInsertedEdges = 0;
	
	
	public ConsensusUnion(ArrayList<Dag_n> dags, ArrayList<Node> order){
		this.setOfdags = dags;
		this.alpha = order;
		
	}	
	
	
	
	public ConsensusUnion(ArrayList<Dag_n> dags){
		this.setOfdags = dags;
		this.heuristic = new AlphaOrder(this.setOfdags);
		
	}	
	
	public ConsensusUnion(){
		this.setOfdags = null;
	}
	
	public int getNumberOfInsertedEdges(){
		return this.numberOfInsertedEdges;
	}
	
	public Dag_n union(){
		
		if(this.alpha == null){
			
			this.heuristic.computeAlphaH2();
			this.alpha = this.heuristic.alpha;
		}
		
		this.imaps2alpha = new TransformDags(this.setOfdags,this.alpha);
		this.imaps2alpha.transform();
		this.numberOfInsertedEdges = this.imaps2alpha.getNumberOfInsertedEdges();
	
		this.union = new Dag_n(this.alpha);
		for(Node nodei: this.alpha){
			for(Dag_n d : this.imaps2alpha.setOfOutputDags){
				List<Node>parent = d.getParents(nodei);
				for(Node pa: parent){
					if(!this.union.isParentOf(pa, nodei)) this.union.addEdge(new Edge(pa,nodei,Endpoint.TAIL,Endpoint.ARROW));
				}
			}
			
		}
		return this.union;
		
	}
	
	public Dag_n getUnion(){
	
		return this.union;
		
	}
	
	void setDags(ArrayList<Dag_n> dags){
		this.setOfdags = dags;
		this.heuristic = new AlphaOrder(this.setOfdags);
		this.heuristic.computeAlphaH2();
		this.alpha = this.heuristic.alpha;
		this.imaps2alpha = new TransformDags(this.setOfdags,this.alpha);
		this.imaps2alpha.transform();
	}
	

	
    public static void main(String args[]) {
	

		System.out.println("Grafos de Partida:   ");

		// (seed, n. variables, n egdes aprox, n. dags, mutation)
		RandomBN setOfDags = new RandomBN(0, Integer.parseInt(args[0]), Integer.parseInt(args[1]),
				Integer.parseInt(args[2]),Integer.parseInt(args[3]));
		setOfDags.generate();
//
		for( Dag_n g: setOfDags.setOfRandomDags) System.out.print(g);
		ConsensusUnion conDag= new ConsensusUnion();
		conDag.setDags(setOfDags.setOfRandomDags);
		Graph g = conDag.union();
		System.out.println("grafo consenso: "+ g);
		
    }



	@Override
	public void run() {
		// TODO Auto-generated method stub
		this.union = this.union();
	}
		
		
		
	
}
