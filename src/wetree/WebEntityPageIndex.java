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
//    public void associatePrefixWithWebentity(String lru, WebEntityId weid);
//    public void dissociatePrefix(String lru);
//    public List<String> getPages(String prefix);
//    public List<String> getPages(WebEntityId weid);
//    public List<String> getPages(String prefix, QueryParameters params);
//    public List<String> getPages(WebEntityId weid, QueryParameters params);
//    public String getPrefix(String lru);
//    public WebEntityId getWebentity(String lru);
//    public WebEntityId getWebentity_fromPrefix(String prefix);
//    public void addPlink(PLink pLink);
//    public List<PLink> getPlinks(String page);
//    public List<PLink> getPlinksInbound(String page);
//    public List<PLink> getPlinksOutbound(String page);
//    public List<WELink> getWelinks(String page);
//    public List<WELink> getWelinksInbound(String page);
//    public List<WELink> getWelinksOutbound(String page);
//    public List<WebEntityId> getWebEntityParent(WebEntityId weid);
//    public List<WebEntityId> getWebEntityChild(WebEntityId weid);
//    public int getPageDegree(String page);
//    public int getPageIndegree(String page);
//    public int getPageOutdegree(String page);
//    public List<WELink> getWelinks(WebEntityId weid);
//    public List<WELink> getWelinksInbound(WebEntityId weid);
//    public List<WELink> getWelinksOutbound(WebEntityId weid);
//    public List<PLink> getPlinksInternal(WebEntityId weid);
//    public List<PLink> getPlinksInbound(WebEntityId weid);
//    public List<PLink> getPlinksOutbound(WebEntityId weid);
//    public void addWecreationrule(WebEntityCreationRule wecr);
//    public void removeWecreationrule(WebEntityCreationRule wecr);
//    public List<WebEntityCreationRule> getWecreationrules();
//    public void setDefaultWecreationrule(/* to define */);
//    public String getPrefixFromWecreationrule(WebEntityCreationRule wecr, String lru);
//    public List<WELink> getWelinks();
//    public List<String> slow_getPages();
//    public List<WebEntityId> slow_getWebentities();
//    public List<PLink> slow_getPlinks();
//    public List<String> slow_getPrefixes(WebEntityId weid);

    // Internal classes
    public static class WebEntityId {
        public WebEntityId() {}
    }
    public static class WebEntityCreationRule {
        public WebEntityCreationRule() {}
    }
    public static class QueryParameters {
        public QueryParameters() {}
    }
    // Page Link
    public static class PLink {
        public PLink() {}
    }
    // Web Entity Link
    public static class WELink {
        public WELink() {}
    }
}
