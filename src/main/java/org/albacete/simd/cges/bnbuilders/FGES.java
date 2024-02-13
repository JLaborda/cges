package org.albacete.simd.cges.bnbuilders;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.BDeuScore;
import edu.cmu.tetrad.search.Fges;
import edu.cmu.tetrad.search.Fges2;
import org.albacete.simd.cges.framework.BNBuilder;
import org.albacete.simd.cges.utils.Utils;

public class FGES extends BNBuilder {
    
    public final boolean setFaithfulnessAssumed;
    public final boolean ges;

    public FGES(DataSet data, boolean setFaithfulnessAssumed, boolean ges) {
        super(data, 1);
        this.setFaithfulnessAssumed = setFaithfulnessAssumed;
        this.ges = ges;
    }

    public FGES(String path, boolean setFaithfulnessAssumed, boolean ges) {
        this(Utils.readData(path), setFaithfulnessAssumed, ges);
    }

    @Override
    public Graph search(){
        BDeuScore bdeu = new BDeuScore(this.getData());
        if (!ges) {
            Fges fges = new Fges(bdeu);
            fges.setFaithfulnessAssumed(setFaithfulnessAssumed);
            this.currentGraph = fges.search();
            this.score = fges.scoreDag(currentGraph);
        } else {
            Fges2 fges = new Fges2(bdeu);
            fges.setFaithfulnessAssumed(true);
            this.currentGraph = fges.search();
            this.score = fges.scoreDag(currentGraph);
        }

        return this.currentGraph;
    }




}
