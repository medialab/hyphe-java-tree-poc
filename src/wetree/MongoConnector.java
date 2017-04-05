/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import org.bson.Document;

/**
 *
 * @author Yomgui
 */
public class MongoConnector {
    private final MongoClient client;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;
    
    public MongoConnector() {
        MongoClientURI connectionURI = new MongoClientURI("mongodb://localhost:27017");
        this.client = new MongoClient(connectionURI);
        this.database = this.client.getDatabase("hyphe");
        this.collection = this.database.getCollection("AXA.pages");
    }
}
