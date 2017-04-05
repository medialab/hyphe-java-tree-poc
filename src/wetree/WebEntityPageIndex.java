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
public interface WebEntityPageIndex {
    public void reset();
    public void addPage(String page);
    public void associatePrefixWithWebEntity(String lru, WebEntityId weid);
    public void dissociatePrefix(String lru);
    public List<String> getPages(String prefix);
    public List<String> getPages(WebEntityId weid);
    public List<String> getPages(String prefix, QueryParameters params);
    public List<String> getPages(WebEntityId weid, QueryParameters params);
    public String getPrefix(String lru);
    public WebEntityId getWebEntity(String lru);
    public WebEntityId getWebEntity_fromPrefix(String prefix);
    public void addPageLink(String page);

    // Internal classes
    public static class WebEntityId {
        public WebEntityId() {}
    }
    public static class QueryParameters {
        public QueryParameters() {}
    }
}
