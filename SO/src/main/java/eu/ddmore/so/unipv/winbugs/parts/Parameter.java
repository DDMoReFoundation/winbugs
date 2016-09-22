/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.so.unipv.winbugs.parts;

import eu.ddmore.libpharmml.dom.dataset.ColumnType;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.stat.StatUtils;
import static org.apache.commons.math3.stat.StatUtils.mean;
import static org.apache.commons.math3.stat.StatUtils.min;

/**
 *
 * @author cristiana larizza UNIPV
 */
public class Parameter {

    private String name;
    private String symbId;
    private String type;
    private ColumnType cType;
    private List<Sample> values;
    private DataStatistics ds;

    public Parameter() {
        values = new ArrayList<>();
    }

    public Parameter(String type) {
        this();
        this.type = type;

    }

    public void add(String s) {
        String[] f = s.split("\t");
        add(Double.parseDouble(f[1]));
    }

    public void add(double v) {
        values.add(new Sample(v));
    }

    public String toString() {
        String ss = name + "\n";
        for (Sample s : values) {
            ss += s + "\n";
        }
        return ss;
    }

    public void setValues(List<Sample> values) {
        this.values = values;
    }

    public ColumnType getcType() {
        return cType;
    }

    public void setName(String name, List<String> popList, List<String> resildual, List<String> prediction) {
        this.name = name;

        if (Util.isInList(resildual, Util.removeIndexes(name))
                || Util.isInList(prediction, Util.removeIndexes(name))) {
            this.cType = ColumnType.UNDEFINED;
        } else if (Util.isInList(popList, Util.removeIndexes(name))) {
            this.cType = ColumnType.POP_PARAMETER;
            if (name.indexOf("[") > -1) {
                this.symbId = name.substring(0, name.indexOf("["));
            } else {
                this.symbId = name;
            }
        } else {
            this.cType = ColumnType.INDIV_PARAMETER;
            if (name.indexOf("[") > -1) {
                this.symbId = name.substring(0, name.indexOf("["));
            } else {
                this.symbId = name;
            }
        }
    }

    private double[] getArray() {
        double[] vet = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vet[i] = values.get(i).getVal();
        }
        return vet;
    }

    public double getMean() {
        double[] vals = getArray();
        return mean(vals);
    }

    public double getVar() {
        double[] vals = getArray();
        return StatUtils.variance(vals);
    }

    public double getSD() {
        double[] vals = getArray();
        return Math.sqrt(StatUtils.variance(vals));
    }

    public double getPerc(double p) {
        double[] vals = getArray();
        return StatUtils.percentile(vals, p);
    }

    public double getMin() {
        double[] vals = getArray();
        return min(vals);
    }

    public String getName() {
        return name;
    }

    public String getSymbId() {
        return symbId;
    }

    public List<Sample> getValues() {
        return values;
    }

    public String getType() {
        return type;
    }

    public double[] getArrayValues() {
        double[] vals = new double[values.size()];
        int i = 0;
        for (Sample s : values) {
            vals[i] = s.getVal();
            i++;
        }
        return vals;
    }

    public DataStatistics statistics() {
        ds = new DataStatistics(this);
        ds.statistics();
        return ds;
    }
}
