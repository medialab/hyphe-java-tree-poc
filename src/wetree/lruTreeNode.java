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
public class lruTreeNode {
    public static final int TREENODE_DATASIZE = 29;
    public static final int TREENODE_TEXTSIZE = 2; // char = 2 bytes
    public static final int TREENODE_SIZE = TREENODE_DATASIZE + TREENODE_TEXTSIZE;
    private final RandomAccessFile file;
    private long nodeid;
    private byte[] bytes;
    
    // Structure of a node :
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
    
    public lruTreeNode(RandomAccessFile file, long nodeid) throws IOException {
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
        file.seek(nodeid * lruTreeNode.TREENODE_SIZE);
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
        byte[] bytes = Longs.toByteArray(parentid);
        for(int i = 0; i<8; i++) {
            this.bytes[1+i] = bytes[i];
        }
    }
    
    public long getParent() {
        return Longs.fromByteArray(Arrays.copyOfRange(bytes, 1, 9));
    }
    
    public void setNextSibling(long parentid) {
        byte[] bytes = Longs.toByteArray(parentid);
        for(int i = 0; i<8; i++) {
            this.bytes[9+i] = bytes[i];
        }
    }
    
    public long getNextSibling() {
        return Longs.fromByteArray(Arrays.copyOfRange(bytes, 9, 17));
    }
    
    public void setChild(long childid) {
        byte[] bytes = Longs.toByteArray(childid);
        for(int i = 0; i<8; i++) {
            this.bytes[17+i] = bytes[i];
        }
    }
    
    public long getChild() {
        return Longs.fromByteArray(Arrays.copyOfRange(bytes, 17, 25));
    }
    
    public void setWebEntity(int weid) {
        byte[] bytes = Ints.toByteArray(weid);
        for(int i = 0; i<4; i++) {
            this.bytes[25+i] = bytes[i];
        }
    }
    
    public int getWebEntity() {
        return Ints.fromByteArray(Arrays.copyOfRange(bytes, 25, 29));
    }
    
    public BitSet getFlags() {
        return BitSet.valueOf(new byte[] { bytes[0] });
    }
    
    public void setFlags(BitSet flags) {
        byte[] baflags = flags.toByteArray();
        if (baflags.length > 0) {
            bytes[0] = flags.toByteArray()[0];
        } else {
            bytes[0] = 0;
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
