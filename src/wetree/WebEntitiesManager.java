/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.google.common.primitives.Chars;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;


        
/**
 *
 * @author jacomyma
 */
public class WebEntitiesManager {
    private final String rootPath;
    private RandomAccessFile lruTreeFile;
    private RandomAccessFile linksFile;
    private RandomAccessFile webentitiesFile;
    private long lastblockid = 0;
    
    public WebEntitiesManager(String p) throws IOException{
        rootPath = p;
        init();
    }
    
    private void init() throws IOException {
        // Create files
        lruTreeFile = new RandomAccessFile(rootPath + "lrus.dat", "rw");
        linksFile = new RandomAccessFile(rootPath + "links.dat", "rw");
        webentitiesFile = new RandomAccessFile(rootPath + "webentities.dat", "rw");
        
        // Keep init only if file empty
        if (lruTreeFile.length() == 0) {
            
            // Create a first node
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, lastblockid++);
            ltn.setChar("s".charAt(0)); // Note: s is convenient for LRUs
            ltn.write();
        }
    }
    
    public void reset() throws IOException {
        lruTreeFile.setLength(0);
        init();
    }
    
    public long addLru(String lru) throws IOException {
        // Add the lru to the lruTree
        long blockid = add(lru);

        // The last child has to get the ending marker.
        // It means "this branch is a string to retrieve"
        // as opposed to just traversing it as a part of a longer string
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
        ltn.setEnding(true);
        ltn.write();
        
        return blockid;
    }
    
    public long addWebEntityPrefix(String lru, int weid) throws IOException {
        // Add the lru to the lruTree
        long blockid = add(lru);

        // The last child has to get the ending marker
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
        ltn.setWebEntity(weid);
        ltn.write();
        
        return blockid;
    }
    
    public void addLink(String sourcelru, String targetlru) throws IOException {
        long sourceblockid = follow(sourcelru);
        long targetblockid = follow(targetlru);
        if (sourceblockid < 0) {
            throw new java.lang.RuntimeException(
                "Link add issue: " + sourcelru + " could not be found in the tree"
            );
        }
        if (targetblockid < 0) {
            throw new java.lang.RuntimeException(
                "Link add issue: " + sourcelru + " could not be found in the tree"
            );
        }
        addLinkStub(sourceblockid, targetblockid, true);
        addLinkStub(targetblockid, sourceblockid, false);
    }
    
    // Return all LRUs (walk all the tree)
    public ArrayList<String> getLrus() throws IOException {
        ArrayList<String> result = new ArrayList<>();
        
        // Let's casually walk in the tree depth first and store the lrus
        
        // Current string
        Stack<Character> chars = new Stack<>();
        
        // Init
        long blockid = 0;
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
        chars.push(ltn.getChar());
        long child;
        long nextSibling;
        long parent;
        boolean parentIsSibling;
        boolean descending = true;
        
        // Walk: recursively inspect blocks depth first
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
                blockid = child;
                ltn.read(blockid);
                chars.push(ltn.getChar());
            } else if (nextSibling > 0) {
                descending = true;
                blockid = nextSibling;
                ltn.read(blockid);
                chars.pop();
                chars.push(ltn.getChar());
            } else {
                descending = false;
                parent = ltn.getParent();
                parentIsSibling = ltn.parentIsSibling();
                while(parentIsSibling) {
                    blockid = parent;
                    ltn.read(blockid);
                    parent = ltn.getParent();
                    parentIsSibling = ltn.parentIsSibling();
                }
                blockid = parent;
                ltn.read(blockid);
                chars.pop();
            }
            
        }
        
        return result;
    }
    
    // Return all web entities (slow, mostly for monitoring)
    public HashMap<Integer, ArrayList<String>> getWebEntities() throws IOException {
        HashMap<Integer, ArrayList<String>> result = new HashMap<>();
        
        // Let's casually walk in the tree depth first and store the lrus
        
        // Current string
        Stack<Character> chars = new Stack<>();
        
        // Init
        long blockid = 0;
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
        chars.push(ltn.getChar());
        long child;
        long nextSibling;
        long parent;
        int weid;
        boolean parentIsSibling;
        boolean descending = true;
        
        // Walk: recursively inspect blocks depth first
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
                blockid = child;
                ltn.read(blockid);
                chars.push(ltn.getChar());
            } else if (nextSibling > 0) {
                descending = true;
                blockid = nextSibling;
                ltn.read(blockid);
                chars.pop();
                chars.push(ltn.getChar());
            } else {
                descending = false;
                parent = ltn.getParent();
                parentIsSibling = ltn.parentIsSibling();
                while(parentIsSibling) {
                    blockid = parent;
                    ltn.read(blockid);
                    parentIsSibling = ltn.parentIsSibling();
                    parent = ltn.getParent();
                }
                blockid = parent;
                ltn.read(blockid);
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
            long blockid = follow(lru);
            if (blockid < 0) {
                throw new java.lang.RuntimeException(
                    "Prefix " + lru + " could not be found in the tree"
                );
            } else {
                suffixes = getLrus_webEntityBounded(blockid);
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
        long blockid = 0;
        int i = 0;
        while (i < chars.length) {
            char c = chars[i];
            byte[] charbytes = Chars.toByteArray(c);
            
            // First we require the char on current level
            // (require = get if exists, create if not)
            blockid = requireCharFromNextSiblings(blockid, charbytes);
            
            i++;
            
            // Is there a child?
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
            long child = ltn.getChild();
            if (child > 0 && i < chars.length) {
                // There's a child: search him and its siblings
                blockid = child;
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
            blockid = createChild(blockid, charbytes);
            
            i++;
        }
        
        return blockid;
    }
    
    // Add a link stub
    private void addLinkStub(long blockid, long linkblockid, boolean direction) {
        // TODO:
        
        // Go to blockid
        // Look if there is a link mode node
        // Create it if not
        // Loop to children until end
        // Add the pointer
    }
    
    // Returns the block id if the lru if it exists, -1 else
    private long follow(String lru) throws IOException {
        char[] chars = lru.toCharArray();
        long blockid = 0;
        int i = 0;
        while (i < chars.length - 1) {
            char c = chars[i];
            byte[] charbytes = Chars.toByteArray(c);
            
            blockid = getTextFromNextSiblings(blockid, charbytes);
            
            if (blockid < 0) {
                return -1;
            }
            
            i++;

            // Is there a child?
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
            long child = ltn.getChild();
            if (child > 0) {
                // There's a child: search him and its siblings
                blockid = child;
            } else {
                // There is no child while there should be
                return -1;
            }
        }
        
        return blockid;
    }
    
    // Walk the tree from a given block id not following nodes with web entities
    // Note: does not return the full strings but only starting from the blockid
    //       ie. it returns the suffixes of the webentity prefix
    private ArrayList<String> getLrus_webEntityBounded(long blockid) throws IOException {
        ArrayList<String> result = new ArrayList<>();
        
        // A custom walk in the tree:
        // We do not follow the children of any node with a web entity id
        
        // Current string
        Stack<Character> chars = new Stack<>();
        
        // Init
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
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
        blockid = child;
        ltn.read(blockid);
        chars.push(ltn.getChar());

        
        // Walk: recursively inspect blocks depth first
        while (chars.size() > 0) {
            
            // We ignore blocks that have a web entity registered
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
                blockid = child;
                ltn.read(blockid);
                chars.push(ltn.getChar());
            } else if (nextSibling > 0) {
                descending = true;
                blockid = nextSibling;
                ltn.read(blockid);
                chars.pop();
                chars.push(ltn.getChar());
            } else {
                descending = false;
                parent = ltn.getParent();
                parentIsSibling = ltn.parentIsSibling();
                while(parentIsSibling) {
                    blockid = parent;
                    ltn.read(blockid);
                    parent = ltn.getParent();
                    parentIsSibling = ltn.parentIsSibling();
                }
                blockid = parent;
                ltn.read(blockid);
                chars.pop();
            }
            
        }
        
        return result;
    }
    
    // Walks next siblings for the specified text.
    // Create it if it does not exist.
    // The search is only at the same level (looking for next siblings only).
    private long requireCharFromNextSiblings(long blockid, byte[] charbytes) throws IOException {
        while (true) {
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
            long nextSibling = ltn.getNextSibling();
            if (compareCharByteArrays(charbytes, ltn.getCharBytes())) {
                // We found a matching node, blockid is the good one.
                return blockid;
            } else if (nextSibling > 0) {
                // Not matching, but there are siblings so we keep walking
                blockid = nextSibling;
            } else {
                // We're at the end of the level (no more siblings).
                // We create the required node and we bind it.
                blockid = block_newSibling(blockid, charbytes);
                return blockid;
            }
        }
    }
    
    // Walks next siblings for the specified text.
    private long getTextFromNextSiblings(long blockid, byte[] textbytes) throws IOException {
        while (true) {
            lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
            long nextSibling = ltn.getNextSibling();
            if (compareCharByteArrays(textbytes, ltn.getCharBytes())) {
                // We found a matching node, blockid is the good one.
                return blockid;
            } else if (nextSibling > 0) {
                // Not matching, but there are siblings so we keep walking
                blockid = nextSibling;
            } else {
                // We're at the end of the level (no more siblings).
                return -1;
            }
        }
    }
    
    // Get child or create it if no child
    private long createChild(long blockid, byte[] textbytes) throws IOException {
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
        long child = ltn.getChild();
        if (child > 0) {
            throw new java.lang.RuntimeException(
                    "Node " + blockid + " should not have a child already"
                );
        } else {
            blockid = block_newChild(blockid, textbytes);
            return blockid;
        }
    }

    
    // Block related methods
    
    // Structure of a block :
    // BLOCKDATASIZE bytes of metadata + BLOCKTEXTSIZE bytes of text
    
    // Metadata bytes:
    // byte 0: flags
    //   flag 0 : ending
    //   flag 1 : parent is sibling
    //   flag 2 : 
    //   flag 3 : 
    //   flag 4 : 
    //   flag 5 : 
    //   flag 6 : 
    //   flag 7 : 
    // bytes 1 to 8: parent / previous sibling / link (long)
    // bytes 9 to 16: next sibling (long)
    // bytes 17 to 24: child (long)
    // bytes 25 to 28: web entity id (int)
    
    private long block_newSibling(long blockid, byte[] charbytes) throws IOException {
        // Register sibling
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
        ltn.setNextSibling(lastblockid);
        ltn.write();
        
        // Create new block
        long newblockid = lastblockid;
        lruTreeNode newLtn = new lruTreeNode(lruTreeFile, newblockid);
        newLtn.setCharBytes(charbytes);
        newLtn.setParent(blockid);
        newLtn.setParentIsSibling(true);
        newLtn.write();
        lastblockid++;
        return newblockid;
    }
    
    private long block_newChild(long blockid, byte[] charbytes) throws IOException {
        // Register child
        lruTreeNode ltn = new lruTreeNode(lruTreeFile, blockid);
        ltn.setChild(lastblockid);
        ltn.write();
        
        // Create new block
        long newblockid = lastblockid;
        lruTreeNode newLtn = new lruTreeNode(lruTreeFile, newblockid);
        newLtn.setCharBytes(charbytes);
        newLtn.setParent(blockid);
        newLtn.setParentIsSibling(false);
        newLtn.write();
        lastblockid++;
        return newblockid;
    }
    
    // Other
    
    private boolean compareCharByteArrays(byte[] ba1, byte[] ba2) {
        return Arrays.equals(ba1, ba2);
    }
    
    // A raw way to monitor the content of the tree
    public void log() throws IOException {
        for(int i=0; i<lastblockid; i++) {
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
