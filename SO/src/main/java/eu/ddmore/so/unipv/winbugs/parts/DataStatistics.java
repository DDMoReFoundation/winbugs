/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.so.unipv.winbugs.parts;

import org.apache.commons.math3.stat.StatUtils;

/**
 *
 * @author cristiana larizza UNIPV
 */
public class DataStatistics {

    public static double[] perc = {2.5, 5, 25, 50, 75, 95, 97.5};
    private Parameter par;
    private double mean, median;
    private double[] percentiles = new double[perc.length];

    public DataStatistics(Parameter par) {
        this.par = par;
    }

    public void statistics() {
        mean = getMean();
        median = getMedian();
        percentiles = getPercentiles();
    }

    private double getMean() {
        return StatUtils.mean(par.getArrayValues());
    }

    private double getMedian() {
        return StatUtils.percentile(par.getArrayValues(), 50);
    }

    private double[] getPercentiles() {
        double[] vals = new double[perc.length];
        int i = 0;
        for (double pe : perc) {
            vals[i++] = StatUtils.percentile(par.getArrayValues(), pe);
        }
        return vals;
    }
    
    public String toString(){
        String s = "";
        s+= "mean: "+mean+"\n";
        s+= "median: "+median+"\n";
        for(int i=0; i<perc.length;i++){
            s+=perc[i]+" percentile: "+percentiles[i]+"\n";
        }
        return s;
    }
}
