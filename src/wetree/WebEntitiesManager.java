/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.google.common.primitives.Chars;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;


        
/**
 *
 * @author jacomyma
 */
public class WebEntitiesManager {
    private final String rootPath;
    private final RandomAccessFile lruTreeFile;
    private final RandomAccessFile linkTreeFile;
    private final String webentitiesFileName;
    private long nextnodeid = 1;
    private long nextlinkid = 1;
    
    // Web Entity related stuff (for convenience, but should be done elswhere)
    private List<WebEntity> webEntities = new ArrayList<>();
    private int currentWebEntityId = 1;
    
    public WebEntitiesManager(String p) throws IOException{
        rootPath = p;
 
        // Create files
        lruTreeFile = new RandomAccessFile(rootPath + "lrus.dat", "rw");
        linkTreeFile = new RandomAccessFile(rootPath + "links.dat", "rw");
        webentitiesFileName = rootPath + "webentities.json";

        init();
    }
    
    private void init() throws IOException {        
        // Keep init only if file empty
        if (lruTreeFile.length() == 0) {
            
            // Create a first node
            lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nextnodeid++);
            lruNode.setChar("s".charAt(0)); // Note: s is convenient for LRUs
            lruNode.write();
            
            webEntities = new ArrayList<>();
            currentWebEntityId = 1;
        } else {
            readWebEntities();
        }
    }
    
    public void reset() throws IOException {
        lruTreeFile.setLength(0);
        init();
    }
    
    public long addLru(String lru) throws IOException {
        // Add the lru to the lruTree
        long nodeid = add(lru);

        // The last child has to get the ending marker.
        // It means "this branch is a string to retrieve"
        // as opposed to just traversing it as a part of a longer string
        lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
        lruNode.setEnding(true);
        lruNode.write();
        
        return nodeid;
    }
     
    public long addWebEntityPrefix(String lru, int weid) throws IOException {
        // Add the lru to the lruTree
        long nodeid = add(lru);

        // The last child has to get the ending marker
        lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
        lruNode.setWebEntity(weid);
        lruNode.write();
        
        return nodeid;
    }
    
    public void addLink(String sourcelru, String targetlru) throws IOException {
        long sourcenodeid = followLru(sourcelru);
        long targetnodeid = followLru(targetlru);
        if (sourcenodeid < 0) {
            throw new java.lang.RuntimeException(
                "Link add issue: " + sourcelru + " could not be found in the tree"
            );
        }
        if (targetnodeid < 0) {
            throw new java.lang.RuntimeException(
                "Link add issue: " + sourcelru + " could not be found in the tree"
            );
        }
        addLinkStub(sourcenodeid, targetnodeid, true);
        addLinkStub(targetnodeid, sourcenodeid, false);
    }
    
    // Return all LRUs (walk all the tree)
    public ArrayList<String> getLrus() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        
        // Let's casually walk in the tree depth first and store the lrus
        
        // Current string
        Stack<Character> chars = new Stack<>();
        
        // Init
        long nodeid = 0;
        lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
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
    
    // Return all web entities (SLOW, mostly for monitoring)
    public HashMap<Integer, ArrayList<String>> getWebEntities_SLOW() throws IOException {
        HashMap<Integer, ArrayList<String>> result = new HashMap<>();
        
        // Let's casually walk in the tree depth first and store the lrus
        
        // Current string
        Stack<Character> chars = new Stack<>();
        
        // Init
        long nodeid = 0;
        lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
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
    
    // Return LRUs of a known web entity
    public ArrayList<String> getLrusFromWebEntity(String[] prefixes, int weid) throws IOException {
        ArrayList<String> result = new ArrayList<>();
        ArrayList<String> suffixes;
        for (String lru : prefixes) {
            long nodeid = followLru(lru);
            if (nodeid < 0) {
                throw new java.lang.RuntimeException(
                    "Prefix " + lru + " could not be found in the tree"
                );
            } else {
                suffixes = walkWebEntityForLrus(nodeid);
                suffixes.forEach(suffix->{
                    result.add(lru + suffix);
                });
            }
        }
        return result;
    }
    
    // Add a string to the tree (period)
    private long add(String lru) throws IOException {
        char[] chars = lru.toCharArray();
        long nodeid = 0;
        int i = 0;
        while (i < chars.length) {
            char c = chars[i];
            byte[] charbytes = Chars.toByteArray(c);
            
            // First we require the char on current level
            // (require = get if exists, create if not)
            nodeid = requireCharFromNextSiblings(nodeid, charbytes);
            
            i++;
            
            // Is there a child?
            lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
            long child = lruNode.getChild();
            if (child > 0 && i < chars.length) {
                // There's a child: search him and its siblings
                nodeid = child;
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
            nodeid = createChild(nodeid, charbytes);
            
            i++;
        }
        
        return nodeid;
    }
    
    // Add a link stub
    private void addLinkStub(long node1id, long node2id, boolean direction) throws IOException {
        System.out.println("Link stub from " + node1id + " to " + node2id + " ("+(direction?"straight":"reverse")+")");
        lruTreeNode lruNode = new lruTreeNode(lruTreeFile, node1id);
        long linksPointer = direction ? lruNode.getLinksFrom() : lruNode.getLinksTo();
        if (linksPointer > 0) {
            linkTreeNode existingLinkNode = new linkTreeNode(linkTreeFile, linksPointer);
            long next = existingLinkNode.getNext();
            while(next > 0) {
                existingLinkNode.read(next);
                next = existingLinkNode.getNext();
            }
            // Register the stub
            existingLinkNode.setNext(nextlinkid);
            // Create the stub
            linkTreeNode linkNode = new linkTreeNode(linkTreeFile, nextlinkid);
            linkNode.setLru(node2id);
            linkNode.write();
            nextlinkid++;
        } else {
            // Register the stub
            if (direction) {
                lruNode.setLinksTo(nextlinkid);
            } else {
                lruNode.setLinksFrom(nextlinkid);
            }
            // Create the stub
            linkTreeNode linkNode = new linkTreeNode(linkTreeFile, nextlinkid);
            linkNode.setLru(node2id);
            linkNode.write();
            nextlinkid++;
        }
    }
    
    // Returns the node id of the lru if it exists, -1 else
    private long followLru(String lru) throws IOException {
        char[] chars = lru.toCharArray();
        if (chars.length == 0) return -1;
        long nodeid = 0;
        int i = 0;
        while (i < chars.length) {
            char c = chars[i];
            byte[] charbytes = Chars.toByteArray(c);
            
            nodeid = walkNextSiblingsForText(nodeid, charbytes);
            
            if (nodeid < 0) {
                return -1;
            }
            
            i++;

            // Is there a child?
            lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
            long child = lruNode.getChild();
            if (child > 0) {
                // There's a child: search him and its siblings
                nodeid = child;
            } else {
                // There is no child while there should be
                if (i == chars.length) return nodeid;
                else return -1;
            }
        }
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
        lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
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
    
    // Walks next siblings for the specified text.
    // Create it if it does not exist.
    // The search is only at the same level (looking for next siblings only).
    private long requireCharFromNextSiblings(long nodeid, byte[] charbytes) throws IOException {
        while (true) {
            lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
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
    private long walkNextSiblingsForText(long nodeid, byte[] charbytes) throws IOException {
        while (true) {
            lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
            long nextSibling = lruNode.getNextSibling();
            if (compareCharByteArrays(charbytes, lruNode.getCharBytes())) {
                // We found a matching node, nodeid is the good one.
                return nodeid;
            } else if (nextSibling > 0) {
                // Not matching, but there are siblings so we keep walking
                nodeid = nextSibling;
            } else {
                // We're at the end of the level (no more siblings).
                return -1;
            }
        }
    }
    
    // Get child or create it if no child
    private long createChild(long nodeid, byte[] charbytes) throws IOException {
        lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
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
        lruTreeNode lruNode = new lruTreeNode(lruTreeFile, nodeid);
        if (isSibling) {
            lruNode.setNextSibling(nextnodeid);
        } else {
            lruNode.setChild(nextnodeid);
        }
        lruNode.write();
        
        // Create new node
        long newnodeid = nextnodeid;
        lruTreeNode newLruNode = new lruTreeNode(lruTreeFile, newnodeid);
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
            lruTreeNode lruNode = new lruTreeNode(lruTreeFile, i);
            char c = lruNode.getChar();
            System.out.print(c + "." + i);
            
            if (lruNode.isEnding()) {
                System.out.print(" END");
            }
            
            long linksFrom = lruNode.getLinksFrom();
            if (linksFrom > 0) {
                System.out.print(" =link=>" + linksFrom);
            }
            
            long linksTo = lruNode.getLinksTo();
            if (linksTo > 0) {
                System.out.print(" <=link=" + linksTo);
            }

            long child = lruNode.getChild();
            if (child > 0) {
                lruTreeNode childLruNode = new lruTreeNode(lruTreeFile, child);
                char childC = childLruNode.getChar();
                System.out.print(" -child->" + childC + "." + child);
            }
            
            long nextSibling = lruNode.getNextSibling();
            if (nextSibling > 0) {
                lruTreeNode nsLruNode = new lruTreeNode(lruTreeFile, nextSibling);
                char nextSiblingC = nsLruNode.getChar();
                System.out.print(" -sibl->" + nextSiblingC + "." + nextSibling);
            }
            
            long parent = lruNode.getParent();
            if (parent > 0) {
                lruTreeNode parentLruNode = new lruTreeNode(lruTreeFile, parent);
                char parentC = parentLruNode.getChar();
                System.out.print(" <-parent-" + parentC + "." + parent + "(" + (lruNode.parentIsSibling() ? ("sibl") : ("par")) + ")");
            }
            
            System.out.println("");
        }
    }
    
    // Pure web entity helpers that should not really be part of this
    public void createWebEntity(String[] prefixes) throws IOException {
        WebEntity we = new WebEntity();
        we.setId(currentWebEntityId++);
        we.setPrefixes(Arrays.asList(prefixes));
        webEntities.add(we);
        writeWebEntities();
    }
    
    private void writeWebEntities() throws IOException {
        try (Writer writer = new FileWriter(webentitiesFileName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(webEntities, writer);
        }
    }
    
    private void readWebEntities() throws FileNotFoundException {
        File f = new File(webentitiesFileName);
        if(f.exists() && !f.isDirectory()) { 
            Gson gson = new GsonBuilder().create();
            BufferedReader br = new BufferedReader(new FileReader(webentitiesFileName));
            Type type = new TypeToken<List<WebEntity>>(){}.getType();
            webEntities = gson.fromJson(br, type);
            webEntities.forEach(we->{
                currentWebEntityId = Math.max(currentWebEntityId, we.getId());
            });
            currentWebEntityId++;
        }
    }
    
}
