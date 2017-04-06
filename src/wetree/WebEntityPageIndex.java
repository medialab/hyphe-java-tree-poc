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
    public void associatePrefixWithWebentity(String lru, int weid);
//    public void dissociatePrefix(String lru);
    public List<String> getPages(String prefix);
    public List<String> getPages(int weid);
//    public List<String> getPages(String prefix, QueryParameters params);
//    public List<String> getPages(int weid, QueryParameters params);
    public String getPrefix(String lru);
    public int getWebentity(String lru);
    public int getWebentity_fromPrefix(String prefix);
    public void addPlink(String sourcePage, String targetPage);
    public void addPlink(PLink pLink);
    public List<PLink> getPlinks(String page);
    public List<PLink> getPlinksInbound(String page);
    public List<PLink> getPlinksOutbound(String page);
//    public List<WELink> getWelinks(String page);
//    public List<WELink> getWelinksInbound(String page);
//    public List<WELink> getWelinksOutbound(String page);
//    public List<int> getWebEntityParent(int weid);
//    public List<int> getWebEntityChild(int weid);
//    public int getPageDegree(String page);
//    public int getPageIndegree(String page);
//    public int getPageOutdegree(String page);
//    public List<WELink> getWelinks(int weid);
//    public List<WELink> getWelinksInbound(int weid);
//    public List<WELink> getWelinksOutbound(int weid);
//    public List<PLink> getPlinksInternal(int weid);
//    public List<PLink> getPlinksInbound(int weid);
//    public List<PLink> getPlinksOutbound(int weid);
//    public void addWecreationrule(WebEntityCreationRule wecr);
//    public void removeWecreationrule(WebEntityCreationRule wecr);
//    public List<WebEntityCreationRule> getWecreationrules();
//    public void setDefaultWecreationrule(/* to define */);
//    public String getPrefixFromWecreationrule(WebEntityCreationRule wecr, String lru);
//    public List<WELink> getWelinks();
//    public List<String> slow_getPages();
//    public List<int> slow_getWebentities();
//    public List<PLink> slow_getPlinks();
//    public List<String> slow_getPrefixes(int weid);

    // Internal classes
    public static class QueryParameters {
        public QueryParameters() {}
    }
}
