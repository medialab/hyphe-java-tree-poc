/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import java.util.Iterator;

import com.mongodb.client.MongoCursor;

import org.bson.Document;

/**
 *
 * @author Yomgui
 * @param <String>
 */
public class MongoPageLrusIterator<String> implements Iterable<String> {
    private final MongoCursor<Document> cursor;
    
    public MongoPageLrusIterator(MongoCursor<Document> cursor) {
        this.cursor = cursor;
    }
    
    @Override
    public Iterator<String> iterator() {
        MongoCursor<Document> mongoCursor;
        mongoCursor = this.cursor;
        
        Iterator<String> it;
        it = new Iterator<String>() {
            
            @Override
            public boolean hasNext() {
                return mongoCursor.hasNext();
            }
            
            @Override
            public String next() {
                return (String) "Hello";
            }
        };
        
        return it;
    }
}
