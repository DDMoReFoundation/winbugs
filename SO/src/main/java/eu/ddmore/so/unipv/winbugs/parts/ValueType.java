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
public enum ValueType {

    /**
     * A property that is described using an attribute in the XML.
     */
    VALUE("VALUE"),
    MEAN("MEAN"),
    POSTERIORMEAN("POSTERIORMEAN"),
    VAR("VAR"),
    SD("SD"),
    PERC("PERC"),
    MEDIAN("MEDIAN"),
    POSTERIORMEDIAN("POSTERIORMEDIAN"),
    RESIDUAL("RESIDUAL");

    private final String type; 

    private ValueType(final String name) {
        this.type = name;
    }

    public String toString() {
        return this.type;
    }

    public String getType() {
        return type;
    }

}
