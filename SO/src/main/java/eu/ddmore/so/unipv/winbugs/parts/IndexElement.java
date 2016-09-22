/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.so.unipv.winbugs.parts;

import java.util.List;

/**
 *
 * @author cristiana larizza UNIPV
 */
public class IndexElement {

    private String name, type;
    private int start;
    private int end;

    public IndexElement(String name, int start, int end) {
        this.name = name;
        this.start = start;
        this.end = end;
        String tmp = name;
        if (name.contains("[")) {
            tmp = name.substring(0, name.indexOf("["));
        }
    }

    public String getName() {
        return name;
    }

    public void setType(List<String> popList, List<String>  residual, List<String>  prediction) {

        if (Util.isInList(residual, Util.removeIndexes(this.name))
                || Util.isInList(prediction, Util.removeIndexes(this.name))) {
            this.type = "OBS";
        } else if (Util.isInList(popList, Util.removeIndexes(this.name))) {
            this.type = "POP";
        } else {
            type = "INDIV";
        }
    }

    public String getType() {
        return type;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getSamplesNumber() {
        return end - start + 1;
    }

    @Override
    public String toString() {
        return "" + name + " [" + start + "," + end + ']';
    }

}
