/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Chars;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import static java.util.Objects.isNull;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;


        
/**
 *
 * @author jacomyma
 */
public class WebEntityPageTree implements WebEntityPageIndex {
    private static final WebEntityPageTree INSTANCE = new WebEntityPageTree();
    private final String rootPath = System.getProperty("user.dir") + File.separator + "data" + File.separator;
    private RandomAccessFile lruTreeFile;
    private RandomAccessFile linkTreeFile;
    private long nextnodeid = 1;
    private long nextlinkid = 1;
        
    private WebEntityPageTree() {
        try {
            // Create files
            lruTreeFile = new RandomAccessFile(rootPath + "lrus.dat", "rw");
            linkTreeFile = new RandomAccessFile(rootPath + "links.dat", "rw");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static WebEntityPageTree getInstance() { return INSTANCE; }
    
    public void init(boolean reset) throws FileNotFoundException {
        if (reset) {
            reset();
        } else {
            WebEntities.getInstance().read();
        }
    }
    
    @Override
    public void reset() {
        try {
            lruTreeFile.setLength(0);
            linkTreeFile.setLength(0);
            WebEntities.getInstance().reset();

            // Create a first node
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nextnodeid++);
            lruNode.setChar("s".charAt(0)); // Note: s is convenient for LRUs
            lruNode.write();
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void addPage(String page) {
        try {
            // Add the lru to the lruTree
            long nodeid = add(page).nodeid;
            
            // The last child has to get the ending marker.
            // It means "this branch is a string to retrieve"
            // as opposed to just traversing it as a part of a longer string
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
            lruNode.setEnding(true);
            lruNode.write();
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void associatePrefixWithWebentity(String lru, int weid) {
        try {
            // Add the lru to the lruTree
            long nodeid = add(lru).nodeid;
            
            // The last child has to get the ending marker
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
            lruNode.setWebEntity(weid);
            lruNode.write();
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void addPlink(PLink pLink) {
        addPlink(pLink.sourcePage, pLink.targetPage);
    }
    
    @Override
    public void addPlink(String sourcePage, String targetPage) {
        try {
            long sourcenodeid = add(sourcePage).nodeid;
            long targetnodeid = add(targetPage).nodeid;
            if (sourcenodeid < 0) {
                throw new java.lang.RuntimeException(
                        "Link add issue: " + sourcePage + " could not be found in the tree"
                );
            }
            if (targetnodeid < 0) {
                throw new java.lang.RuntimeException(
                        "Link add issue: " + targetPage + " could not be found in the tree"
                );
            }
            addLinkStub(sourcenodeid, targetnodeid, true);
            addLinkStub(targetnodeid, sourcenodeid, false);
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void addPlinks(List<PLink> pLinks) {
        Multimap<String, String> stubsString = ArrayListMultimap.create();
        Multimap<String, String> stubsReverseString = ArrayListMultimap.create();
        Set<String> pages = new HashSet<>();
        
        pLinks.forEach(pLink->{
            pages.add(pLink.sourcePage);
            pages.add(pLink.targetPage);
            stubsString.put(pLink.sourcePage, pLink.targetPage);
            stubsReverseString.put(pLink.targetPage, pLink.sourcePage);
        });
        
        // Add each different page and index their nodeid
        HashMap<String, Long> nodeIdIndex = new HashMap<>();
        Iterator pit = pages.iterator();
        while (pit.hasNext()) {
            try {
                String p = (String) pit.next();
                long nodeid = add(p).nodeid;
                if (nodeid < 0) {
                    throw new java.lang.RuntimeException(
                            "Link add issue: " + p + " could not be found in the tree"
                    );
                }
                nodeIdIndex.put(p, nodeid);
            } catch (IOException ex) {
                Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        stubsString.keySet().forEach(lru1->{
            ArrayList<Long> node2ids = new ArrayList<>();
            stubsString.get(lru1).forEach(lru2->{
                node2ids.add(nodeIdIndex.get(lru2));
            });
            addLinkStubs(nodeIdIndex.get(lru1), node2ids, true);
        });
        
        stubsReverseString.keySet().forEach(lru1->{
            ArrayList<Long> node2ids = new ArrayList<>();
            stubsReverseString.get(lru1).forEach(lru2->{
                node2ids.add(nodeIdIndex.get(lru2));
            });
            addLinkStubs(nodeIdIndex.get(lru1), node2ids, false);
        });

    }
    
    @Override
    public List<PLink> getPlinks(String page) {
        ArrayList<PLink> result = new ArrayList<>();
        result.addAll(getPlinksInbound(page));
        result.addAll(getPlinksOutbound(page));
        return result;
    }
        
    @Override
    public List<PLink> getPlinksInbound(String page) {
        ArrayList<PLink> result = new ArrayList<>();
        ArrayList<Long> sourceNodeIds = new ArrayList<>();
        try {
            long nodeid = followLru(page).nodeid;
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
            // Follow the links
            LinkTreeNode linkNode = new LinkTreeNode(linkTreeFile, lruNode.getInLinks());
            sourceNodeIds.add(linkNode.getLru());
            long next = linkNode.getNext();
            while(next > 0) {
                linkNode.read(next);
                sourceNodeIds.add(linkNode.getLru());
                next = linkNode.getNext();
            }
            sourceNodeIds.forEach(tnodeid->{   
                try {
                    String sourceLru = windupLru(tnodeid);
                    PLink pLink = new PLink(sourceLru, page);
                    result.add(pLink);
                } catch (IOException ex) {
                    Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    @Override
    public List<PLink> getPlinksOutbound(String page) {
        ArrayList<PLink> result = new ArrayList<>();
        ArrayList<Long> targetNodeIds = new ArrayList<>();
        try {
            long nodeid = followLru(page).nodeid;
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
            // Follow the links
            LinkTreeNode linkNode = new LinkTreeNode(linkTreeFile, lruNode.getOutLinks());
            targetNodeIds.add(linkNode.getLru());
            long next = linkNode.getNext();
            while(next > 0) {
                linkNode.read(next);
                targetNodeIds.add(linkNode.getLru());
                next = linkNode.getNext();
            }
            targetNodeIds.forEach(tnodeid->{   
                try {
                    String targetLru = windupLru(tnodeid);
                    PLink pLink = new PLink(page, targetLru);
                    result.add(pLink);
                } catch (IOException ex) {
                    Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    // Return all LRUs - walks all the tree, SLOW, mostly for monitoring
    public ArrayList<String> _getAllLrus_SLOW() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        
        // Let's casually walk in the tree depth first and store the lrus
        
        // Current string
        Stack<Character> chars = new Stack<>();
        
        // Init
        long nodeid = 0;
        LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
        chars.push(lruNode.getChar());
        long child;
        long nextSibling;
        long parent;
        boolean parentIsSibling;
        boolean descending = true;
        
        // Walk: recursively inspect nodes depth first
        while (chars.size() > 0) {
            // Add LRU to the list
            if (descending && lruNode.isEnding()) {
                StringBuilder sb = new StringBuilder(chars.size());
                chars.forEach((c) -> {
                    sb.append(c);
                });
                String lru = sb.toString();
                result.add(lru);
            }
            
            child = lruNode.getChild();
            nextSibling = lruNode.getNextSibling();
            if (descending && child > 0) {
                nodeid = child;
                lruNode.read(nodeid);
                chars.push(lruNode.getChar());
            } else if (nextSibling > 0) {
                descending = true;
                nodeid = nextSibling;
                lruNode.read(nodeid);
                chars.pop();
                chars.push(lruNode.getChar());
            } else {
                descending = false;
                parent = lruNode.getParent();
                parentIsSibling = lruNode.parentIsSibling();
                while(parentIsSibling) {
                    nodeid = parent;
                    lruNode.read(nodeid);
                    parent = lruNode.getParent();
                    parentIsSibling = lruNode.parentIsSibling();
                }
                nodeid = parent;
                lruNode.read(nodeid);
                chars.pop();
            }
            
        }
        
        return result;
    }
    
    // Return all web entities - walks all the tree, SLOW, mostly for monitoring
    public HashMap<Integer, ArrayList<String>> _getAllWebEntities_SLOW() throws IOException {
        HashMap<Integer, ArrayList<String>> result = new HashMap<>();
        
        // Let's casually walk in the tree depth first and store the lrus
        
        // Current string
        Stack<Character> chars = new Stack<>();
        
        // Init
        long nodeid = 0;
        LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
        chars.push(lruNode.getChar());
        long child;
        long nextSibling;
        long parent;
        int weid;
        boolean parentIsSibling;
        boolean descending = true;
        
        // Walk: recursively inspect nodes depth first
        while (chars.size() > 0) {
            // Add to results if needed
            weid = lruNode.getWebEntity();
            if (descending && weid > 0) {
                StringBuilder sb = new StringBuilder(chars.size());
                chars.forEach((c) -> {
                    sb.append(c);
                });
                String lru = sb.toString();
 
                if (result.containsKey(weid)){
                    result.get(weid).add(lru);
                } else {
                    ArrayList<String> lrus = new ArrayList<>();
                    lrus.add(lru);
                    result.put(weid, lrus);
                }
            }
            
            child = lruNode.getChild();
            nextSibling = lruNode.getNextSibling();
            if (descending && child > 0) {
                nodeid = child;
                lruNode.read(nodeid);
                chars.push(lruNode.getChar());
            } else if (nextSibling > 0) {
                descending = true;
                nodeid = nextSibling;
                lruNode.read(nodeid);
                chars.pop();
                chars.push(lruNode.getChar());
            } else {
                descending = false;
                parent = lruNode.getParent();
                parentIsSibling = lruNode.parentIsSibling();
                while(parentIsSibling) {
                    nodeid = parent;
                    lruNode.read(nodeid);
                    parentIsSibling = lruNode.parentIsSibling();
                    parent = lruNode.getParent();
                }
                nodeid = parent;
                lruNode.read(nodeid);
                chars.pop();
            }
            
        }
        
        return result;
    }    
    
    public ArrayList<String[]> _geAllLruLinks_SLOW() throws IOException {
        ArrayList<String[]> result = new ArrayList<>();
        
        // Let's casually walk in the tree depth first and store the lrus
        
        // Current string
        Stack<Character> chars = new Stack<>();
        
        // Init
        long nodeid = 0;
        LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
        chars.push(lruNode.getChar());
        long child;
        long nextSibling;
        long parent;
        boolean parentIsSibling;
        boolean descending = true;
        
        // Walk: recursively inspect nodes depth first
        while (chars.size() > 0) {
            // Add LRU to the list
            if (descending && lruNode.isEnding()) {
                long outLinks = lruNode.getOutLinks();
                if (outLinks > 0) {
                    StringBuilder sb = new StringBuilder(chars.size());
                    chars.forEach((c) -> {
                        sb.append(c);
                    });
                    String sourceLru = sb.toString();
                    // Follow the links
                    ArrayList<Long> targetNodeIds = new ArrayList<>();
                    LinkTreeNode linkNode = new LinkTreeNode(linkTreeFile, outLinks);
                    targetNodeIds.add(linkNode.getLru());
                    long next = linkNode.getNext();
                    while(next > 0) {
                        linkNode.read(next);
                        targetNodeIds.add(linkNode.getLru());
                        next = linkNode.getNext();
                    }
                    targetNodeIds.forEach(tnodeid->{
                        try {
                            String targetLru = windupLru(tnodeid);
                            String[] link = new String[2];
                            link[0] = sourceLru;
                            link[1] = targetLru;
                            result.add(link);
                        } catch (IOException ex) {
                            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });                    
                }
            }
            
            child = lruNode.getChild();
            nextSibling = lruNode.getNextSibling();
            if (descending && child > 0) {
                nodeid = child;
                lruNode.read(nodeid);
                chars.push(lruNode.getChar());
            } else if (nextSibling > 0) {
                descending = true;
                nodeid = nextSibling;
                lruNode.read(nodeid);
                chars.pop();
                chars.push(lruNode.getChar());
            } else {
                descending = false;
                parent = lruNode.getParent();
                parentIsSibling = lruNode.parentIsSibling();
                while(parentIsSibling) {
                    nodeid = parent;
                    lruNode.read(nodeid);
                    parent = lruNode.getParent();
                    parentIsSibling = lruNode.parentIsSibling();
                }
                nodeid = parent;
                lruNode.read(nodeid);
                chars.pop();
            }
            
        }
        
        return result;
    }
    
    @Override
    public ArrayList<String> getPages(String prefix) {
        ArrayList<String> result = new ArrayList<>();
        WalkHistory wh;
        try {
            wh = followLru(prefix);
            if (!wh.success) {
                throw new java.lang.RuntimeException(
                    "getPages: Prefix '" + prefix + "' could not be found in the tree"
                );
            } else {
                ArrayList<String> suffixes = walkWebEntityForLrus(wh.nodeid);
                suffixes.forEach(suffix->{
                    result.add(prefix + suffix);
                });
            }
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    @Override
    public ArrayList<String> getPages(int weid) {
        WebEntity we = WebEntities.getInstance().get(weid);
        if (isNull(we)) {
            throw new java.lang.RuntimeException(
                "getPages: Web Entity '" + weid + "' could not be found"
            );
        }
        return getPages(we.getPrefixes());
    }
    
    // Return LRUs of a known web entity
    private ArrayList<String> getPages(List<String> prefixes) {
        ArrayList<String> result = new ArrayList<>();
        prefixes.forEach(lru->{
            WalkHistory wh;
            try {
                wh = followLru(lru);
                if (!wh.success) {
                    throw new java.lang.RuntimeException(
                        "getPages: Prefix '" + lru + "' could not be found in the tree"
                    );
                } else {
                    ArrayList<String> suffixes = walkWebEntityForLrus(wh.nodeid);
                    suffixes.forEach(suffix->{
                        result.add(lru + suffix);
                    });
                }
            } catch (IOException ex) {
                Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return result;
    }
    
    @Override
    public List<WELink> getWelinksInbound(int weid) {
        WebEntity we = WebEntities.getInstance().get(weid);
        ArrayList<Integer> we2ids = getWebEntityLinks(we.getPrefixes(), false);
        ArrayList<WELink> result = new ArrayList<>();
        we2ids.forEach(we2id->{
            result.add(new WELink(weid, we2id));
        });
        return result;
    }
    
     @Override
    public List<WELink> getWelinksOutbound(int weid) {
        WebEntity we = WebEntities.getInstance().get(weid);
        ArrayList<Integer> we2ids = getWebEntityLinks(we.getPrefixes(), true);
        ArrayList<WELink> result = new ArrayList<>();
        we2ids.forEach(we2id->{
            result.add(new WELink(weid, we2id));
        });
        return result;
    }
    
    @Override
    public List<WELink> getWelinks(int weid) {
        ArrayList<WELink> result = new ArrayList<>();
        result.addAll(getWelinksInbound(weid));
        result.addAll(getWelinksOutbound(weid));
        return result;
    }
    
    // Return LRUs of a known web entity
    private ArrayList<Integer> getWebEntityLinks(List<String> prefixes, boolean out) {
        ArrayList<Integer> result = new ArrayList<>();
        prefixes.forEach(lru->{
            result.addAll(getWebEntityLinks(lru, out));
        });
        return result;
    }
    
    private ArrayList<Integer> getWebEntityLinks(String lru, boolean out) {
        HashMap<Integer, Integer> weidMap = new HashMap<>();
        WalkHistory wh;
        try {
            wh = followLru(lru);
            if (!wh.success) {
                throw new java.lang.RuntimeException(
                    "Prefix '" + lru + "' could not be found in the tree"
                );
            } else {
                ArrayList<Long> nodeids = walkWebEntityForLruNodeIds(wh.nodeid);
                nodeids.forEach(nid->{
                    try {
                        LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nid);
                        long links = out ? lruNode.getOutLinks() : lruNode.getInLinks();
                        if (links > 0) {
                            LinkTreeNode linkNode = new LinkTreeNode(linkTreeFile, links);

                            int we2id = windupLruForWebEntityId(linkNode.getLru());
                            weidMap.put(we2id, weidMap.getOrDefault(we2id, 0));

                            long next = linkNode.getNext();
                            while(next > 0) {
                                linkNode.read(next);

                                we2id = windupLruForWebEntityId(linkNode.getLru());
                                weidMap.put(we2id, weidMap.getOrDefault(we2id, 0));

                                next = linkNode.getNext();
                            }
                        }


                    } catch (IOException ex) {
                        Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        ArrayList<Integer> result = new ArrayList<>();
        weidMap.keySet().forEach(weid->{ result.add(weid);});
        return result;
    }
    
    // Add a string to the tree (period)
    private WalkHistory add(String lru) throws IOException {
        char[] chars = lru.toCharArray();
        WalkHistory wh = new WalkHistory();
        wh.nodeid = 0;
        int i = 0;
        while (i < chars.length) {
            char c = chars[i];
            byte[] charbytes = Chars.toByteArray(c);
            
            // First we require the char on current level
            // (require = get if exists, create if not)
            wh.nodeid = requireCharFromNextSiblings(wh.nodeid, charbytes);
            
            i++;
            
            // Is there a child?
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, wh.nodeid);
            long child = lruNode.getChild();
            if (child > 0 && i < chars.length) {
                // There's a child: search him and its siblings
                wh.nodeid = child;
            } else {
                // There is no child: we jump to the next loop
                // where we store the rest of the letters in new children
                break;
            }
        }
        
        while (i < chars.length) {
            // Get the letter and check it
            char c = chars[i];
            byte[] charbytes = Chars.toByteArray(c);
            
            // We're here if the last letter had no child
            // Let's create children until the string is stored
            wh.nodeid = createChild(wh.nodeid, charbytes);
            
            i++;
        }
        
        return wh;
    }
    
    // Add a link stub
    private void addLinkStub(long node1id, long node2id, boolean direction) {
        try {
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, node1id);
            long linksPointer = direction ? lruNode.getOutLinks() : lruNode.getInLinks();
            if (linksPointer > 0) {
                LinkTreeNode existingLinkNode = new LinkTreeNode(linkTreeFile, linksPointer);
                long next = existingLinkNode.getNext();
                while(next > 0) {
                    existingLinkNode.read(next);
                    next = existingLinkNode.getNext();
                }
                // Register the stub
                existingLinkNode.setNext(nextlinkid);
                existingLinkNode.write();
            } else {
                // Register the stub
                if (direction) {
                    lruNode.setOutLinks(nextlinkid);
                } else {
                    lruNode.setInLinks(nextlinkid);
                }
                lruNode.write();
            }
            // Create the stub
            LinkTreeNode linkNode = new LinkTreeNode(linkTreeFile, nextlinkid);
            linkNode.setLru(node2id);
            linkNode.write();
            nextlinkid++;
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    // Add a batch of link stubs
    private void addLinkStubs(long node1id, Collection<Long> node2ids, boolean direction) {
        try {
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, node1id);
            long linksPointer = direction ? lruNode.getOutLinks() : lruNode.getInLinks();
            if (linksPointer > 0) {
                LinkTreeNode existingLinkNode = new LinkTreeNode(linkTreeFile, linksPointer);
                long next = existingLinkNode.getNext();
                while(next > 0) {
                    existingLinkNode.read(next);
                    next = existingLinkNode.getNext();
                }
                // Register the stub
                existingLinkNode.setNext(nextlinkid);
                existingLinkNode.write();
            } else {
                // Register the stub
                if (direction) {
                    lruNode.setOutLinks(nextlinkid);
                } else {
                    lruNode.setInLinks(nextlinkid);
                }
                lruNode.write();
            }
            Iterator it = node2ids.iterator();
            while (it.hasNext()) {
                long node2id = (Long) it.next();
                
                // Create the stub
                LinkTreeNode linkNode = new LinkTreeNode(linkTreeFile, nextlinkid);
                linkNode.setLru(node2id);
                // If not last, register the next
                if (it.hasNext()) {
                    linkNode.setNext(nextlinkid+1);
                }
                linkNode.write();
                nextlinkid++;
                
            }
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public String getPrefix(String lru) {
        try {
            WalkHistory wh = followLru(lru);
            return wh.lastWebEntityPrefix;
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
    @Override
    public int getWebentity(String lru) {
        try {
            WalkHistory wh = followLru(lru);
            return wh.lastWebEntityId;
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    @Override
    public int getWebentity_fromPrefix(String prefix) {
        WalkHistory wh;
        try {
            wh = followLru(prefix);
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, wh.nodeid);
            return lruNode.getWebEntity();
        } catch (IOException ex) {
            Logger.getLogger(WebEntityPageTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }

    // Returns the node id of the lru if it exists, -1 else
    private WalkHistory followLru(String lru) throws IOException {
        WalkHistory wh = new WalkHistory();
        char[] chars = lru.toCharArray();
        if (chars.length == 0) {
            wh.success = false;
            wh.nodeid = -1;
            return wh;
        }
        wh.nodeid = 0;
        int i = 0;
        
        while (i < chars.length) {
            char c = chars[i];
            byte[] charbytes = Chars.toByteArray(c);
            
            // Search for i-th char from nodeid and its next siblings
            walkNextSiblingsForText(wh, charbytes);
            if (wh.nodeid < 0) {
                wh.success = false;
                return wh;
            }
            
            // Update history
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, wh.nodeid);
            int weid = lruNode.getWebEntity();
            if (weid > 0) {
                wh.lastWebEntityId = weid;
                wh.lastWebEntityPrefix = lru.substring(0, i+1);
            }
            
            // The char has been found.
            // If we need to go deeper, then we must ensure there is a child.
            i++;
            if (i < chars.length) {
                // Is there a child?
                long child = lruNode.getChild();
                if (child > 0) {
                    wh.nodeid = child;
                } else {
                    // There is no child while there should be
                    wh.success = false;
                    wh.nodeid = -1;
                    return wh;
                }
            }
        }
        wh.success = true;
        return wh;
    }
    
    // Returns the lru of a node id
    private String windupLru(long nodeid) throws IOException {
        LruTreeNode lruNode;
        StringBuilder sb;
        String lru = "";
        
        do {
            lruNode = new LruTreeNode(lruTreeFile, nodeid);
            
            sb = new StringBuilder(1);
            sb.append(lruNode.getChar());
            lru = sb.toString() + lru;

            while (lruNode.parentIsSibling()) {
                lruNode.read(lruNode.getParent());
            }
            
            // Jump to parent
            nodeid = lruNode.getParent();
        } while(nodeid > 0);
        
        return lru;
    }
    
    // Returns the web entity containing of a node id (or -1 if not found)
    private int windupLruForWebEntityId(long nodeid) throws IOException {
        LruTreeNode lruNode;
        int weid;
        
        do {
            lruNode = new LruTreeNode(lruTreeFile, nodeid);
            weid = lruNode.getWebEntity();
            
            if (weid > 0) {
                return weid;
            }

            while (lruNode.parentIsSibling()) {
                lruNode.read(lruNode.getParent());
            }
            
            // Jump to parent
            nodeid = lruNode.getParent();
        } while(nodeid > 0);
        
        return -1;
    }
    
    // Walk the tree from a given node id not following nodes with web entities
    // Note: does not return the full strings but only starting from the nodeid
    //       ie. it returns the suffixes of the webentity prefix
    private ArrayList<String> walkWebEntityForLrus(long nodeid) throws IOException {
        ArrayList<String> result = new ArrayList<>();
        
        // A custom walk in the tree:
        // We do not follow the children of any node with a web entity id
        
        // Current string
        Stack<Character> chars = new Stack<>();
        
        // Init
        LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
        long child;
        long nextSibling;
        long parent;
        boolean parentIsSibling;
        boolean descending = true;
        boolean ignore;
        
        // The starting LRU may be a word
        if (lruNode.isEnding()) {
            StringBuilder sb = new StringBuilder(chars.size());
            chars.forEach(sb::append);
            String lru = sb.toString();
            result.add(lru);
        }
        
        // If there is no child, it stops there
        child = lruNode.getChild();
        if (child <= 0) {
            return result;
        }
        
        // Let's start the walk with the child
        nodeid = child;
        lruNode.read(nodeid);
        chars.push(lruNode.getChar());

        
        // Walk: recursively inspect nodes depth first
        while (chars.size() > 0) {
            
            // We ignore nodes that have a web entity registered
            ignore = lruNode.getWebEntity() > 0;
                
            // Add LRU to the list
            if (descending && lruNode.isEnding() && !ignore) {
               StringBuilder sb = new StringBuilder(chars.size());
               chars.forEach((c) -> {
                   sb.append(c);
                });
                String lru = sb.toString();
                result.add(lru);
            }
            
            StringBuilder sb = new StringBuilder(chars.size());
            chars.forEach((c) -> {
                sb.append(c);
            });
            String lru = sb.toString();
            
            
            child = lruNode.getChild();
            nextSibling = lruNode.getNextSibling();
            if (descending && child > 0 && !ignore) {
                nodeid = child;
                lruNode.read(nodeid);
                chars.push(lruNode.getChar());
            } else if (nextSibling > 0) {
                descending = true;
                nodeid = nextSibling;
                lruNode.read(nodeid);
                chars.pop();
                chars.push(lruNode.getChar());
            } else {
                descending = false;
                parent = lruNode.getParent();
                parentIsSibling = lruNode.parentIsSibling();
                while(parentIsSibling) {
                    nodeid = parent;
                    lruNode.read(nodeid);
                    parent = lruNode.getParent();
                    parentIsSibling = lruNode.parentIsSibling();
                }
                nodeid = parent;
                lruNode.read(nodeid);
                chars.pop();
            }
            
        }
        
        return result;
    }
    
    // Walk the tree from a given node id not following nodes with web entities
    private ArrayList<Long> walkWebEntityForLruNodeIds(long nodeid) throws IOException {
        ArrayList<Long> result = new ArrayList<>();
        
        // A custom walk in the tree:
        // We do not follow the children of any node with a web entity id
        
        int depth = 0;
        
        // Init
        LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
        long child;
        long nextSibling;
        long parent;
        boolean parentIsSibling;
        boolean descending = true;
        boolean ignore;
        
        // The starting LRU may be a word
        if (lruNode.isEnding()) {
            result.add(nodeid);
        }
        
        // If there is no child, it stops there
        child = lruNode.getChild();
        if (child <= 0) {
            return result;
        }
        
        // Let's start the walk with the child
        nodeid = child;
        lruNode.read(nodeid);
        depth++;
        
        // Walk: recursively inspect nodes depth first
        while (depth > 0) {
            
            // We ignore nodes that have a web entity registered
            ignore = lruNode.getWebEntity() > 0;
                
            // Add the nodeid to the list
            if (descending && lruNode.isEnding() && !ignore) {
                result.add(nodeid);
            }
            
            child = lruNode.getChild();
            nextSibling = lruNode.getNextSibling();
            if (descending && child > 0 && !ignore) {
                nodeid = child;
                lruNode.read(nodeid);
                depth++;
            } else if (nextSibling > 0) {
                descending = true;
                nodeid = nextSibling;
                lruNode.read(nodeid);
            } else {
                descending = false;
                parent = lruNode.getParent();
                parentIsSibling = lruNode.parentIsSibling();
                while(parentIsSibling) {
                    nodeid = parent;
                    lruNode.read(nodeid);
                    parent = lruNode.getParent();
                    parentIsSibling = lruNode.parentIsSibling();
                }
                nodeid = parent;
                lruNode.read(nodeid);
                depth--;
            }
        }
        
        return result;
    }
    
    // Walks next siblings for the specified text.
    // Create it if it does not exist.
    // The search is only at the same level (looking for next siblings only).
    private long requireCharFromNextSiblings(long nodeid, byte[] charbytes) throws IOException {
        while (true) {
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
            long nextSibling = lruNode.getNextSibling();
            if (compareCharByteArrays(charbytes, lruNode.getCharBytes())) {
                // We found a matching node, nodeid is the good one.
                return nodeid;
            } else if (nextSibling > 0) {
                // Not matching, but there are siblings so we keep walking
                nodeid = nextSibling;
            } else {
                // We're at the end of the level (no more siblings).
                // We create the required node and we bind it.
                nodeid = chainNewSibling(nodeid, charbytes);
                return nodeid;
            }
        }
    }
    
    // Walks starting point to next siblings for specified text
    private void walkNextSiblingsForText(WalkHistory wh, byte[] charbytes) throws IOException {
        while (true) {
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, wh.nodeid);
            long nextSibling = lruNode.getNextSibling();
            if (compareCharByteArrays(charbytes, lruNode.getCharBytes())) {
                // We found a matching node, nodeid is the good one.
                return;
            } else if (nextSibling > 0) {
                // Not matching, but there are siblings so we keep walking
                wh.nodeid = nextSibling;
            } else {
                // We're at the end of the level (no more siblings).
                wh.nodeid = -1;
                return;
            }
        }
    }
    
    // Get child or create it if no child
    private long createChild(long nodeid, byte[] charbytes) throws IOException {
        LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
        long child = lruNode.getChild();
        if (child > 0) {
            throw new java.lang.RuntimeException(
                    "Node " + nodeid + " should not have a child already"
                );
        } else {
            nodeid = chainNewChild(nodeid, charbytes);
            return nodeid;
        }
    }
    
    private long chainNewSibling(long nodeid, byte[] charbytes) throws IOException {
        return chainNewChildOrSibling(nodeid, charbytes, true);
    }
    
    private long chainNewChild(long nodeid, byte[] charbytes) throws IOException {
        return chainNewChildOrSibling(nodeid, charbytes, false);
    }
    
    private long chainNewChildOrSibling(long nodeid, byte[] charbytes, boolean isSibling) throws IOException {
        // Register chid/sibling
        LruTreeNode lruNode = new LruTreeNode(lruTreeFile, nodeid);
        if (isSibling) {
            lruNode.setNextSibling(nextnodeid);
        } else {
            lruNode.setChild(nextnodeid);
        }
        lruNode.write();
        
        // Create new node
        long newnodeid = nextnodeid;
        LruTreeNode newLruNode = new LruTreeNode(lruTreeFile, newnodeid);
        newLruNode.setCharBytes(charbytes);
        newLruNode.setParent(nodeid);
        newLruNode.setParentIsSibling(isSibling);
        newLruNode.write();
        nextnodeid++;
        return newnodeid;
    }
    
    // Other
    
    private boolean compareCharByteArrays(byte[] ba1, byte[] ba2) {
        return Arrays.equals(ba1, ba2);
    }
    
    // A raw way to monitor the content of the tree
    public void log() throws IOException {
        for(int i=0; i<nextnodeid; i++) {
            LruTreeNode lruNode = new LruTreeNode(lruTreeFile, i);
            char c = lruNode.getChar();
            System.out.print(c + "." + i);
            
            if (lruNode.isEnding()) {
                System.out.print(" END");
            }
            
            long child = lruNode.getChild();
            if (child > 0) {
                LruTreeNode childLruNode = new LruTreeNode(lruTreeFile, child);
                char childC = childLruNode.getChar();
                System.out.print(" -child->" + childC + "." + child);
            }
            
            long nextSibling = lruNode.getNextSibling();
            if (nextSibling > 0) {
                LruTreeNode nsLruNode = new LruTreeNode(lruTreeFile, nextSibling);
                char nextSiblingC = nsLruNode.getChar();
                System.out.print(" -sibling->" + nextSiblingC + "." + nextSibling);
            }
            
            long parent = lruNode.getParent();
            if (parent > 0) {
                LruTreeNode parentLruNode = new LruTreeNode(lruTreeFile, parent);
                char parentC = parentLruNode.getChar();
                System.out.print(" <-" + (lruNode.parentIsSibling() ? ("sibling") : ("parent")) + "-" + parentC + "." + parent);
            }
            
            System.out.println("");
            
            // Links
            
            long outLinks = lruNode.getOutLinks();
            if (outLinks > 0) {
                LinkTreeNode linkNode = new LinkTreeNode(linkTreeFile, outLinks);
                System.out.println("  ==(" + outLinks + ")=> " + linkNode.getLru());
                long next = linkNode.getNext();
                while(next > 0) {
                    linkNode.read(next);
                    System.out.println("  ==(" + next + ")=> " + linkNode.getLru());
                    next = linkNode.getNext();
                }
            }
            
            
            long inLinks = lruNode.getInLinks();
            if (inLinks > 0) {
                LinkTreeNode linkNode = new LinkTreeNode(linkTreeFile, inLinks);
                System.out.println("  <=(" + inLinks + ")== " + linkNode.getLru());
                long next = linkNode.getNext();
                while(next > 0) {
                    linkNode.read(next);
                    System.out.println("  <=(" + next + ")== " + linkNode.getLru());
                    next = linkNode.getNext();
                }
            }

        }
    }
    
    private static class WalkHistory {
        public int lastWebEntityId = 0;
        private String lastWebEntityPrefix;
        public long nodeid = -1;
        public boolean success = false;
        public WalkHistory() {}
    }
}
