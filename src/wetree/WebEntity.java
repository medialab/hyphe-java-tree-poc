/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import java.util.List;

/**
 *
 * @author jacomyma
 */
public class WebEntity {
    private Integer id;
    private List<String> prefixes;

    public Integer getTreeId() {
        return id;
    }

    public void setTreeId(Integer id) {
        this.id = id;
    }

    public List<String> getPrefixes() {
        return prefixes;
    }

    public void setPrefixes(List<String> prefixes) {
        this.prefixes = prefixes;
    }
    
    public WebEntity() {}
    
    
}
