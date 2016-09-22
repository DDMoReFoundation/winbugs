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
public class IndivParameter extends Parameter{
    private Integer subjectId;

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public IndivParameter(Integer subjectId) {
        this.subjectId = subjectId;
    }
    
}
