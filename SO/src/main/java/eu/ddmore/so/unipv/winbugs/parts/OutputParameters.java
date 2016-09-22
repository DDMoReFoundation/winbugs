/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.ddmore.so.unipv.winbugs.parts;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author cristiana larizza UNIPV
 */
public class OutputParameters {

    private Map<String, Parameter> pars;

    public OutputParameters() {
        pars = new HashMap<>();
    }

    public void putParameter(String name, Parameter p) {
        pars.put(name, p);
    }


    public void putAll(Map<String, Parameter> p) {
        pars.putAll(p);
    }

    public Map<String, Parameter> getPars() {
        return pars;
    }

    public Parameter getParByName(String name) {
        return pars.get(name);
    }
}
