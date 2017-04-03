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
import java.io.PrintWriter;
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
    private final RandomAccessFile linksFile;
    private final String webentitiesFileName;
    private long lastnodeid = 0;
    
    // Web Entity related stuff (not very important)
    private List<WebEntity> webEntities = new ArrayList<>();
    private int currentWebEntityId = 1;
    
    public WebEntitiesManager(String p) throws IOException{
        rootPath = p;
 
        // Create files
        lruTreeFile = new RandomAccessFile(rootPath + "lrus.dat", "rw");
        linksFile = new RandomAccessFile(rootPath + "links.dat", "rw");
        webentitiesFileName = rootPath + "webentities.json";

        init();
    }
    
    private void init() throws IOException {        
        // Keep init only if file empty
        if (lruTreeFile.length() == 0) {
            
            // Create a first node
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, lastnodeid++);
            ltn.setChar("s".charAt(0)); // Note: s is convenient for LRUs
            ltn.write();
            
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
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
        ltn.setEnding(true);
        ltn.write();
        
        return nodeid;
    }
    
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
     
    public long addWebEntityPrefix(String lru, int weid) throws IOException {
        // Add the lru to the lruTree
        long nodeid = add(lru);

        // The last child has to get the ending marker
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
        ltn.setWebEntity(weid);
        ltn.write();
        
        return nodeid;
    }
    
    public void addLink(String sourcelru, String targetlru) throws IOException {
        long sourcenodeid = follow(sourcelru);
        long targetnodeid = follow(targetlru);
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
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
        chars.push(ltn.getChar());
        long child;
        long nextSibling;
        long parent;
        boolean parentIsSibling;
        boolean descending = true;
        
        // Walk: recursively inspect nodes depth first
        while (chars.size() > 0) {
            // Add LRU to the list
            if (descending && ltn.isEnding()) {
                StringBuilder sb = new StringBuilder(chars.size());
                chars.forEach((c) -> {
                    sb.append(c);
                });
                String lru = sb.toString();
                result.add(lru);
            }
            
            child = ltn.getChild();
            nextSibling = ltn.getNextSibling();
            if (descending && child > 0) {
                nodeid = child;
                ltn.read(nodeid);
                chars.push(ltn.getChar());
            } else if (nextSibling > 0) {
                descending = true;
                nodeid = nextSibling;
                ltn.read(nodeid);
                chars.pop();
                chars.push(ltn.getChar());
            } else {
                descending = false;
                parent = ltn.getParent();
                parentIsSibling = ltn.parentIsSibling();
                while(parentIsSibling) {
                    nodeid = parent;
                    ltn.read(nodeid);
                    parent = ltn.getParent();
                    parentIsSibling = ltn.parentIsSibling();
                }
                nodeid = parent;
                ltn.read(nodeid);
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
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
        chars.push(ltn.getChar());
        long child;
        long nextSibling;
        long parent;
        int weid;
        boolean parentIsSibling;
        boolean descending = true;
        
        // Walk: recursively inspect nodes depth first
        while (chars.size() > 0) {
            // Add to results if needed
            weid = ltn.getWebEntity();
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
            
            child = ltn.getChild();
            nextSibling = ltn.getNextSibling();
            if (descending && child > 0) {
                nodeid = child;
                ltn.read(nodeid);
                chars.push(ltn.getChar());
            } else if (nextSibling > 0) {
                descending = true;
                nodeid = nextSibling;
                ltn.read(nodeid);
                chars.pop();
                chars.push(ltn.getChar());
            } else {
                descending = false;
                parent = ltn.getParent();
                parentIsSibling = ltn.parentIsSibling();
                while(parentIsSibling) {
                    nodeid = parent;
                    ltn.read(nodeid);
                    parentIsSibling = ltn.parentIsSibling();
                    parent = ltn.getParent();
                }
                nodeid = parent;
                ltn.read(nodeid);
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
            long nodeid = follow(lru);
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
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
            long child = ltn.getChild();
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
    private void addLinkStub(long nodeid, long linknodeid, boolean direction) {
        // TODO:
        
        // Go to nodeid
        // Look if there is a link mode node
        // Create it if not
        // Loop to children until end
        // Add the pointer
    }
    
    // Returns the node id if the lru if it exists, -1 else
    private long follow(String lru) throws IOException {
        char[] chars = lru.toCharArray();
        long nodeid = 0;
        int i = 0;
        while (i < chars.length - 1) {
            char c = chars[i];
            byte[] charbytes = Chars.toByteArray(c);
            
            nodeid = walkNextSiblingsForText(nodeid, charbytes);
            
            if (nodeid < 0) {
                return -1;
            }
            
            i++;

            // Is there a child?
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
            long child = ltn.getChild();
            if (child > 0) {
                // There's a child: search him and its siblings
                nodeid = child;
            } else {
                // There is no child while there should be
                return -1;
            }
        }
        
        return nodeid;
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
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
        long child;
        long nextSibling;
        long parent;
        boolean parentIsSibling;
        boolean descending = true;
        boolean ignore;
        
        // The starting LRU may be a word
        if (ltn.isEnding()) {
            StringBuilder sb = new StringBuilder(chars.size());
            chars.forEach(sb::append);
            String lru = sb.toString();
            result.add(lru);
        }
        
        // If there is no child, it stops there
        child = ltn.getChild();
        if (child <= 0) {
            return result;
        }
        
        // Let's start the walk with the child
        nodeid = child;
        ltn.read(nodeid);
        chars.push(ltn.getChar());

        
        // Walk: recursively inspect nodes depth first
        while (chars.size() > 0) {
            
            // We ignore nodes that have a web entity registered
            ignore = ltn.getWebEntity() > 0;
                
            // Add LRU to the list
            if (descending && ltn.isEnding() && !ignore) {
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
            
            
            child = ltn.getChild();
            nextSibling = ltn.getNextSibling();
            if (descending && child > 0 && !ignore) {
                nodeid = child;
                ltn.read(nodeid);
                chars.push(ltn.getChar());
            } else if (nextSibling > 0) {
                descending = true;
                nodeid = nextSibling;
                ltn.read(nodeid);
                chars.pop();
                chars.push(ltn.getChar());
            } else {
                descending = false;
                parent = ltn.getParent();
                parentIsSibling = ltn.parentIsSibling();
                while(parentIsSibling) {
                    nodeid = parent;
                    ltn.read(nodeid);
                    parent = ltn.getParent();
                    parentIsSibling = ltn.parentIsSibling();
                }
                nodeid = parent;
                ltn.read(nodeid);
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
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
            long nextSibling = ltn.getNextSibling();
            if (compareCharByteArrays(charbytes, ltn.getCharBytes())) {
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
    
    // Walks next siblings for specified text
    private long walkNextSiblingsForText(long nodeid, byte[] charbytes) throws IOException {
        while (true) {
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
            long nextSibling = ltn.getNextSibling();
            if (compareCharByteArrays(charbytes, ltn.getCharBytes())) {
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
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
        long child = ltn.getChild();
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
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, nodeid);
        if (isSibling) {
            ltn.setNextSibling(lastnodeid);
        } else {
            ltn.setChild(lastnodeid);
        }
        ltn.write();
        
        // Create new node
        long newnodeid = lastnodeid;
        lruTreeNode newLtn = new lruTreeNode(lruTreeFile, newnodeid);
        newLtn.setCharBytes(charbytes);
        newLtn.setParent(nodeid);
        newLtn.setParentIsSibling(isSibling);
        newLtn.write();
        lastnodeid++;
        return newnodeid;
    }
    
    // Other
    
    private boolean compareCharByteArrays(byte[] ba1, byte[] ba2) {
        return Arrays.equals(ba1, ba2);
    }
    
    // A raw way to monitor the content of the tree
    public void log() throws IOException {
        for(int i=0; i<lastnodeid; i++) {
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, i);
            char c = ltn.getChar();
            System.out.print(c + "#" + i);
            
            if (ltn.isEnding()) {
                System.out.print(" ENDING");
            }
            
            long child = ltn.getChild();
            if (child > 0) {
                lruTreeNode childLtn = new lruTreeNode(lruTreeFile, child);
                char childC = childLtn.getChar();
                System.out.print(" >child " + childC + "#" + child);
            }
            
            long nextSibling = ltn.getNextSibling();
            if (nextSibling > 0) {
                lruTreeNode nsLtn = new lruTreeNode(lruTreeFile, nextSibling);
                char nextSiblingC = nsLtn.getChar();
                System.out.print(" >nextSibling " + nextSiblingC + "#" + nextSibling);
            }
            
            long parent = ltn.getParent();
            if (parent > 0) {
                lruTreeNode parentLtn = new lruTreeNode(lruTreeFile, parent);
                char parentC = parentLtn.getChar();
                System.out.print(" >parent " + parentC + "#" + parent + "(" + (ltn.parentIsSibling() ? ("previous sibling") : ("parent")) + ")");
            }
            
            System.out.println("");
        }
    }
    
}
