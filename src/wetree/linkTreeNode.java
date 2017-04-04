/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.google.common.primitives.Longs;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

/**
 *
 * @author jacomyma
 */
public class LinkTreeNode {
    public static final int TREENODE_SIZE = 16;
    public static final int TREENODE_OFFSET_LRU = 0;
    public static final int TREENODE_OFFSET_NEXT = 8;
    private final RandomAccessFile file;
    private long nodeid;
    private final byte[] bytes;
    
    public LinkTreeNode(RandomAccessFile file, long nodeid) throws IOException {
        bytes = new byte[TREENODE_SIZE];
        this.file = file;
        this.nodeid = nodeid;
        file.seek(nodeid * TREENODE_SIZE);
        file.read(bytes);
    }
    
    // Structure of a node :
    // bytes 0 to 7: the other end's node id (long)
    // bytes 8 to 15: next stub of the series (long)
    
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
    
    public void setLru(long lrunodeid) {
        byte[] b = Longs.toByteArray(lrunodeid);
        for(int i = 0; i<8; i++) {
            this.bytes[TREENODE_OFFSET_LRU+i] = b[i];
        }
    }
    
    public long getLru() {
        return Longs.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_LRU, TREENODE_OFFSET_LRU+8));
    }
    
    public void setNext(long linknodeid) {
        byte[] b = Longs.toByteArray(linknodeid);
        for(int i = 0; i<8; i++) {
            this.bytes[TREENODE_OFFSET_NEXT+i] = b[i];
        }
    }
    
    public long getNext() {
        return Longs.fromByteArray(Arrays.copyOfRange(bytes, TREENODE_OFFSET_NEXT, TREENODE_OFFSET_NEXT+8));
    }
    
    public long getId() {
        return nodeid;
    }
    // TEST
}
