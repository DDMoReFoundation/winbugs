/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.so.unipv.winbugs.parts;

/**
 *
 * @author cristiana larizza UNIPV
 */
public class Sample {
    private double val;

    public Sample(double val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return "" + val + '\t';
    }

    public double getVal() {
        return val;
    }
    
}
