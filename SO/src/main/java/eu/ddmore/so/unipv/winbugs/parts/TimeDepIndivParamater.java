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
public class TimeDepIndivParamater extends IndivParameter {
    private Integer time;

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    public TimeDepIndivParamater(Integer time, Integer subjectId) {
        super(subjectId);
        this.time = time;
    }
    
}
