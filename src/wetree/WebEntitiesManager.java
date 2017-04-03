/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.google.common.base.Charsets;
import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
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
    private static final int BLOCKDATASIZE = 29;
    private static final int BLOCKTEXTSIZE = 2; // char = 2 bytes
    private static final int BLOCKSIZE = BLOCKDATASIZE + BLOCKTEXTSIZE;
    private static final long BLOCKCOUNT = 1024 * 64;
    private static final long FILESIZE = BLOCKSIZE * BLOCKCOUNT;
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
            byte[] block = new byte[BLOCKSIZE];
            block_setChar(block, "s".charAt(0)); // Note: s is convenient for LRUs
            block_new(block);
        }
    }
    
    public void reset() throws IOException {
        lruTreeFile.setLength(0);
        init();
    }
    
    public long addLru(String lru) throws IOException {
        long blockid = add(lru);

        // The last child has to get the ending marker.
        // It means "this branch is a string to retrieve"
        // as opposed to just traversing it as a part of a longer string
        byte[] block = block_read(blockid);
        block_setEnding(block, true);
        block_update(blockid, block);
        
        return blockid;
    }
    
    public long addWebEntityPrefix(String lru, int weid) throws IOException {
        long blockid = add(lru);

        // The last child has to get the ending marker
        byte[] block = block_read(blockid);
        block_setWebEntity(block, weid);
        block_update(blockid, block);
        
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
        byte[] block = block_read(blockid);
        chars.push(block_getChar(block));
        long child;
        long nextSibling;
        long parent;
        boolean parentIsSibling;
        boolean descending = true;
        
        // Walk: recursively inspect blocks depth first
        while (chars.size() > 0) {
            // Add LRU to the list
            if (descending && block_isEnding(block)) {
                StringBuilder sb = new StringBuilder(chars.size());
                chars.forEach((c) -> {
                    sb.append(c);
                });
                String lru = sb.toString();
                result.add(lru);
            }
            
            child = block_getChild(block);
            nextSibling = block_getNextSibling(block);
            if (descending && child > 0) {
                blockid = child;
                block = block_read(blockid);
                chars.push(block_getChar(block));
            } else if (nextSibling > 0) {
                descending = true;
                blockid = nextSibling;
                block = block_read(blockid);
                chars.pop();
                chars.push(block_getChar(block));
            } else {
                descending = false;
                parent = block_getParent(block);
                parentIsSibling = block_parentIsSibling(block);
                while(parentIsSibling) {
                    blockid = parent;
                    block = block_read(blockid);
                    parentIsSibling = block_parentIsSibling(block);
                    parent = block_getParent(block);
                }
                blockid = parent;
                block = block_read(blockid);
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
        byte[] block = block_read(blockid);
        chars.push(block_getChar(block));
        long child;
        long nextSibling;
        long parent;
        int weid;
        boolean parentIsSibling;
        boolean descending = true;
        
        // Walk: recursively inspect blocks depth first
        while (chars.size() > 0) {
            // Add to results if needed
            weid = block_getWebEntity(block);
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
            
            child = block_getChild(block);
            nextSibling = block_getNextSibling(block);
            if (descending && child > 0) {
                blockid = child;
                block = block_read(blockid);
                chars.push(block_getChar(block));
            } else if (nextSibling > 0) {
                descending = true;
                blockid = nextSibling;
                block = block_read(blockid);
                chars.pop();
                chars.push(block_getChar(block));
            } else {
                descending = false;
                parent = block_getParent(block);
                parentIsSibling = block_parentIsSibling(block);
                while(parentIsSibling) {
                    blockid = parent;
                    block = block_read(blockid);
                    parentIsSibling = block_parentIsSibling(block);
                    parent = block_getParent(block);
                }
                blockid = parent;
                block = block_read(blockid);
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
            // Get the letter and check if its size is not an issue
            char c = chars[i];
            byte[] charbytes = Chars.toByteArray(c);
            
            // First we require the char on current level
            // (require = get if exists, create if not)
            blockid = requireCharFromNextSiblings(blockid, charbytes);
            
            i++;
            
            // Is there a child?
            byte[] block = block_read(blockid);
            long child = block_getChild(block);
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
            byte[] block = block_read(blockid);
            long child = block_getChild(block);
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
        byte[] block = block_read(blockid);
        long child;
        long nextSibling;
        long parent;
        boolean parentIsSibling;
        boolean descending = true;
        boolean ignore;
        
        // The starting LRU may be a word
        if (block_isEnding(block)) {
            StringBuilder sb = new StringBuilder(chars.size());
            chars.forEach((c) -> {
                sb.append(c);
            });
            String lru = sb.toString();
            result.add(lru);
        }
        
        // If there is no child, it stops there
        child = block_getChild(block);
        if (child <= 0) {
            return result;
        }
        
        // Let's start the walk with the child
        blockid = child;
        block = block_read(blockid);
        chars.push(block_getChar(block));

        
        // Walk: recursively inspect blocks depth first
        while (chars.size() > 0) {
            
            // We ignore blocks that have a web entity registered
            ignore = block_getWebEntity(block) > 0;
                
            // Add LRU to the list
            if (descending && block_isEnding(block) && !ignore) {
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
            
            
            child = block_getChild(block);
            nextSibling = block_getNextSibling(block);
            if (descending && child > 0 && !ignore) {
                blockid = child;
                block = block_read(blockid);
                chars.push(block_getChar(block));
            } else if (nextSibling > 0) {
                descending = true;
                blockid = nextSibling;
                block = block_read(blockid);
                chars.pop();
                chars.push(block_getChar(block));
            } else {
                descending = false;
                parent = block_getParent(block);
                parentIsSibling = block_parentIsSibling(block);
                while(parentIsSibling) {
                    blockid = parent;
                    block = block_read(blockid);
                    parentIsSibling = block_parentIsSibling(block);
                    parent = block_getParent(block);
                }
                blockid = parent;
                block = block_read(blockid);
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
            byte[] block = block_read(blockid);
            long nextSibling = block_getNextSibling(block);
            if (compareCharByteArrays(charbytes, block_getCharBytes(block))) {
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
            byte[] block = block_read(blockid);
            long nextSibling = block_getNextSibling(block);
            if (compareCharByteArrays(textbytes, block_getCharBytes(block))) {
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
        byte[] block = block_read(blockid);
        long child = block_getChild(block);
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
        byte[] block = block_read(blockid);
        block_setNextSibling(block, lastblockid);
        lruTreeFile.seek(blockid * BLOCKSIZE);
        lruTreeFile.write(block);
        
        // Create new block
        byte[] newblock = new byte[BLOCKSIZE];
        block_setCharBytes(newblock, charbytes);
        block_setParent(newblock, blockid);
        block_setParentIsSibling(newblock, true);
        long newblockid = block_new(newblock);
        return newblockid;
    }
    
    private long block_newChild(long blockid, byte[] charbytes) throws IOException {
        // Register child
        byte[] block = block_read(blockid);
        block_setChild(block, lastblockid);
        lruTreeFile.seek(blockid * BLOCKSIZE);
        lruTreeFile.write(block);
        
        // Create new block
        byte[] newblock = new byte[BLOCKSIZE];
        block_setCharBytes(newblock, charbytes);
        block_setParent(newblock, blockid);
        block_setParentIsSibling(newblock, false);
        long newblockid = block_new(newblock);
        return newblockid;
    }
    
    private long block_new(byte[] block) throws IOException{
        // System.out.println("Create new block at "+lastblockid);
        // block_log(block);
        lruTreeFile.seek(lastblockid * BLOCKSIZE);
        lruTreeFile.write(block);
        return lastblockid++;
    }
    
    private byte[] block_read(long blockid) throws IOException {
        lruTreeFile.seek(blockid * BLOCKSIZE);
        byte[] bytes = new byte[BLOCKSIZE];
        lruTreeFile.read(bytes);
        return bytes;
    }
    
    private void block_update(long blockid, byte[] block) throws IOException{
        lruTreeFile.seek(blockid * BLOCKSIZE);
        lruTreeFile.write(block);
    }
    
    private void block_setChar(byte[] block, char c) {
        byte[] charbytes = Chars.toByteArray(c);
        block_setCharBytes(block, charbytes);
    }
            
    private char block_getChar(byte[] block) {
        return Chars.fromByteArray(block_getCharBytes(block));
    }
        
    private void block_setCharBytes(byte[] block, byte[] charbytes) {
        for (int i = 0; i < BLOCKTEXTSIZE; i++ ) {
            if (i + 1 > charbytes.length) {
                block[BLOCKDATASIZE + i] = 0;
            } else {
                block[BLOCKDATASIZE + i] = charbytes[i];
            }
        }
    }

    private byte[] block_getCharBytes(byte[] block) {
        return Arrays.copyOfRange(block, BLOCKDATASIZE, BLOCKSIZE);
    }
    
    private void block_setParent(byte[] block, long parentid) {
        byte[] bytes = Longs.toByteArray(parentid);
        for(int i = 0; i<8; i++) {
            block[1+i] = bytes[i];
        }
    }
    
    private long block_getParent(byte[] block) {
        return Longs.fromByteArray(Arrays.copyOfRange(block, 1, 9));
    }
    
    private void block_setNextSibling(byte[] block, long parentid) {
        byte[] bytes = Longs.toByteArray(parentid);
        for(int i = 0; i<8; i++) {
            block[9+i] = bytes[i];
        }
    }
    
    private long block_getNextSibling(byte[] block) {
        return Longs.fromByteArray(Arrays.copyOfRange(block, 9, 17));
    }
    
    private void block_setChild(byte[] block, long childid) {
        byte[] bytes = Longs.toByteArray(childid);
        for(int i = 0; i<8; i++) {
            block[17+i] = bytes[i];
        }
    }
    
    private long block_getChild(byte[] block) {
        return Longs.fromByteArray(Arrays.copyOfRange(block, 17, 25));
    }
    
    private void block_setWebEntity(byte[] block, int weid) {
        byte[] bytes = Ints.toByteArray(weid);
        for(int i = 0; i<4; i++) {
            block[25+i] = bytes[i];
        }
    }
    
    private int block_getWebEntity(byte[] block) {
        return Ints.fromByteArray(Arrays.copyOfRange(block, 25, 29));
    }
    
    private BitSet block_getFlags(byte[] block) {
        return BitSet.valueOf(new byte[] { block[0] });
    }
    
    private void block_setFlags(byte[] block, BitSet flags) {
        byte[] baflags = flags.toByteArray();
        if (baflags.length > 0) {
            block[0] = flags.toByteArray()[0];
        } else {
            block[0] = 0;
        }
    }
    
    private boolean block_isFlag(byte[] block, int flagid) {
        return block_getFlags(block).get(flagid);
    }
    
    private void block_setFlag(byte[] block, int flagid, boolean e) {
        BitSet flags = block_getFlags(block);
        flags.set(flagid, e);
        block_setFlags(block, flags);
    }

    private boolean block_isEnding(byte[] block) {
        return block_isFlag(block, 0);
    }
    
    private void block_setEnding(byte[] block, boolean e) {
        block_setFlag(block, 0, e);
    }
    
    private boolean block_parentIsSibling(byte[] block) {
        return block_isFlag(block, 1);
    }
    
    private void block_setParentIsSibling(byte[] block, boolean e) {
        block_setFlag(block, 1, e);
    }
        
    private void block_log(byte[] block) {
        System.out.print("block (size " + block.length + "): ");
        for(int i = 0; i<block.length; i++) {
            System.out.print(" " + block[i]);
        }
        System.out.println("");
    }
    
    // Other
    
    private boolean compareCharByteArrays(byte[] ba1, byte[] ba2) {
        return Arrays.equals(ba1, ba2);
    }
    
    // A raw way to monitor the content of the tree
    public void log() throws IOException {
        for(int i=0; i<lastblockid; i++) {
            byte[] block = block_read(i);
            char c = block_getChar(block);
            System.out.print(c + "#" + i);
            
            if (block_isEnding(block)) {
                System.out.print(" ENDING");
            }
            
            long child = block_getChild(block);
            if (child > 0) {
               byte[] childBlock = block_read(child);
               char childC = block_getChar(childBlock);
               System.out.print(" >child " + childC + "#" + child);
            }
            
            long nextSibling = block_getNextSibling(block);
            if (nextSibling > 0) {
               byte[] nextSiblingBlock = block_read(nextSibling);
               char nextSiblingC = block_getChar(nextSiblingBlock);
               System.out.print(" >nextSibling " + nextSiblingC + "#" + nextSibling);
            }
            
            long parent = block_getParent(block);
            if (parent > 0) {
               byte[] parentBlock = block_read(parent);
               char parentC = block_getChar(parentBlock);
               System.out.print(" >parent " + parentC + "#" + parent + "(" + (block_parentIsSibling(block) ? ("previous sibling") : ("parent")) + ")");
            }
            
            System.out.println("");
        }
    }
    
}
