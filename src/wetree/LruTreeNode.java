/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.google.common.primitives.Chars;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.BitSet;

/**
 *
 * @author jacomyma
 */
public class LruTreeNode {
    public static final int TREENODE_DATASIZE = 57;
    public static final int TREENODE_TEXTSIZE = 2; // char = 2 bytes
    public static final int TREENODE_SIZE = TREENODE_DATASIZE + TREENODE_TEXTSIZE;
    // Offsets for the different byte zones
    public static final int TREENODE_OFFSET_FLAGS = 0; // Length 1
    public static final int TREENODE_OFFSET_PARENT = 1; // Length 8
    public static final int TREENODE_OFFSET_NEXT_SIBLING = 9; // Length 8
    public static final int TREENODE_OFFSET_CHILD = 17; // Length 8
    public static final int TREENODE_OFFSET_WEB_ENTITY_ID = 25; // Length 4
    public static final int TREENODE_OFFSET_WEB_ENTITY_CREATION_RULE_ID = 29; // Length 4
    public static final int TREENODE_OFFSET_LINKS_OUT = 33; // Length 8
    public static final int TREENODE_OFFSET_LINKS_IN = 41; // Length 8
    public static final int TREENODE_OFFSET_INDEGREE = 49; // Length 4
    public static final int TREENODE_OFFSET_OUTDEGREE = 53; // Length 4
    private final RandomAccessFile file;
    private long nodeid;
    private final byte[] bytes;
    
    // Structure of a node :
    // TREENODE_DATASIZE bytes of metadata + TREENODE_TEXTSIZE bytes of text
    
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
    // bytes 29 to 36: links out (long)
    // bytes 37 to 44: links in (long)
    
    public LruTreeNode(RandomAccessFile file, long nodeid) throws IOException {
        bytes = new byte[TREENODE_SIZE];
        this.file = file;
        this.nodeid = nodeid;
        file.seek(nodeid * TREENODE_SIZE);
        file.read(bytes);
    }

    public byte[] getBytes() {
        return bytes;
    }
    
    public void read(long nodeid) throws IOException {
        this.nodeid = nodeid;
        file.seek(nodeid * TREENODE_SIZE);
        file.read(bytes);
    }
    
    public void write() throws IOException{
        file.seek(nodeid * TREENODE_SIZE);
        file.write(bytes);
    }
        
    public void moveTo(long nodeid) {
        this.nodeid = nodeid;
    }

    public void setChar(char c) {
        byte[] charbytes = Chars.toByteArray(c);
        setCharBytes(charbytes);
    }
            
    public char getChar() {
        return Chars.fromByteArray(getCharBytes());
    }
        
    public void setCharBytes(byte[] charbytes) {
        for (int i = 0; i < TREENODE_TEXTSIZE; i++ ) {
            bytes[TREENODE_DATASIZE + i] = charbytes[i];
        }
    }

    public byte[] getCharBytes() {
        return Arrays.copyOfRange(bytes, TREENODE_DATASIZE, TREENODE_SIZE);
    }
    
    public void setParent(long parentid) {
        byte[] b = Longs.toByteArray(parentid);
        for(int i = 0; i<8; i++) {
            this.bytes[TREENODE_OFFSET_PARENT+i] = b[i];
        }
    }
    
    public long getParent() {
        return Longs.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_PARENT, TREENODE_OFFSET_PARENT+8));
    }
    
    public void setNextSibling(long parentid) {
        byte[] b = Longs.toByteArray(parentid);
        for(int i = 0; i<8; i++) {
            this.bytes[TREENODE_OFFSET_NEXT_SIBLING+i] = b[i];
        }
    }
    
    public long getNextSibling() {
        return Longs.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_NEXT_SIBLING, TREENODE_OFFSET_NEXT_SIBLING+8));
    }
    
    public void setChild(long childid) {
        byte[] b = Longs.toByteArray(childid);
        for(int i = 0; i<8; i++) {
            this.bytes[TREENODE_OFFSET_CHILD+i] = b[i];
        }
    }
    
    public long getChild() {
        return Longs.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_CHILD, TREENODE_OFFSET_CHILD+8));
    }
    
    public void setWebEntity(int weid) {
        byte[] b = Ints.toByteArray(weid);
        for(int i = 0; i<4; i++) {
            this.bytes[TREENODE_OFFSET_WEB_ENTITY_ID+i] = b[i];
        }
    }
    
    public int getWebEntity() {
        return Ints.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_WEB_ENTITY_ID, TREENODE_OFFSET_WEB_ENTITY_ID+4));
    }

    public void setWebEntityCreationRule(int wecrid) {
        byte[] b = Ints.toByteArray(wecrid);
        for(int i = 0; i<4; i++) {
            this.bytes[TREENODE_OFFSET_WEB_ENTITY_CREATION_RULE_ID+i] = b[i];
        }
    }
    
    public int getWebEntityCreationRule() {
        return Ints.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_WEB_ENTITY_CREATION_RULE_ID, TREENODE_OFFSET_WEB_ENTITY_CREATION_RULE_ID+4));
    }

    public void setOutLinks(long pointer) {
        byte[] b = Longs.toByteArray(pointer);
        for(int i = 0; i<8; i++) {
            this.bytes[TREENODE_OFFSET_LINKS_OUT+i] = b[i];
        }
    }
    
    public long getOutLinks() {
        return Longs.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_LINKS_OUT, TREENODE_OFFSET_LINKS_OUT+8));
    }
    
    public void setInLinks(long pointer) {
        byte[] b = Longs.toByteArray(pointer);
        for(int i = 0; i<8; i++) {
            this.bytes[TREENODE_OFFSET_LINKS_IN+i] = b[i];
        }
    }
    
    public long getInLinks() {
        return Longs.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_LINKS_IN, TREENODE_OFFSET_LINKS_IN+8));
    }
        
    public void setIndegree(int indegree) {
        byte[] b = Ints.toByteArray(indegree);
        for(int i = 0; i<4; i++) {
            this.bytes[TREENODE_OFFSET_INDEGREE+i] = b[i];
        }
    }
    
    public int getIndegree() {
        return Ints.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_INDEGREE, TREENODE_OFFSET_INDEGREE+4));
    }
        
    public void setOutdegree(int outdegree) {
        byte[] b = Ints.toByteArray(outdegree);
        for(int i = 0; i<4; i++) {
            this.bytes[TREENODE_OFFSET_OUTDEGREE+i] = b[i];
        }
    }
    
    public int getOutdegree() {
        return Ints.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_OUTDEGREE, TREENODE_OFFSET_OUTDEGREE+4));
    }
        
    public BitSet getFlags() {
        return BitSet.valueOf(new byte[] { bytes[0] });
    }
    
    public void setFlags(BitSet flags) {
        byte[] baflags = flags.toByteArray();
        if (baflags.length > 0) {
            bytes[TREENODE_OFFSET_FLAGS] = flags.toByteArray()[0];
        } else {
            bytes[TREENODE_OFFSET_FLAGS] = 0;
        }
    }
    
    private boolean isFlag(int flagid) {
        return getFlags().get(flagid);
    }
    
    private void setFlag(int flagid, boolean e) {
        BitSet flags = getFlags();
        flags.set(flagid, e);
        setFlags(flags);
    }

    public boolean isEnding() {
        return isFlag(0);
    }
    
    public void setEnding(boolean e) {
        setFlag(0, e);
    }
    
    public boolean parentIsSibling() {
        return isFlag(1);
    }
    
    public void setParentIsSibling(boolean e) {
        setFlag(1, e);
    }
        
    public void log() {
        System.out.print("block (size " + bytes.length + "): ");
        for(int i = 0; i<bytes.length; i++) {
            System.out.print(" " + bytes[i]);
        }
        System.out.println("");
    }
}
