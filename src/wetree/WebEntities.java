/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jacomyma
 */
public class WebEntities {
    private static final WebEntities INSTANCE = new WebEntities();
    private final String webentitiesFileName = System.getProperty("user.dir") + File.separator + "data" + File.separator + File.separator + "data" + "webentities.json";
    private List<WebEntity> webEntities = new ArrayList<>();
    private int currentWebEntityId = 1;
    
    // Singleton
    private WebEntities(){}
    public static WebEntities getInstance() { return INSTANCE; }
    
    public void init() {
        webEntities = new ArrayList<>();
        try {
            read();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WebEntities.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void create(String[] prefixes) throws IOException {
        WebEntity we = new WebEntity();
        we.setTreeId(currentWebEntityId++);
        we.setPrefixes(Arrays.asList(prefixes));
        webEntities.add(we);
        write();
        we.getPrefixes().forEach(lru->{
            WebEntityPageTree.getInstance().associatePrefixWithWebentity(lru, we.getTreeId());
        });
    }
    
    public void create(String prefix) throws IOException {
        String[] prefixes = new String[1];
        prefixes[0] = prefix;
        WebEntities.this.create(prefixes);
    }
    
    private void write() throws IOException {
        try (Writer writer = new FileWriter(webentitiesFileName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(webEntities, writer);
        }
    }
    
    public void read() throws FileNotFoundException {
        File f = new File(webentitiesFileName);
        if(f.exists() && !f.isDirectory()) {
            Gson gson = new GsonBuilder().create();
            BufferedReader br = new BufferedReader(new FileReader(webentitiesFileName));
            Type type = new TypeToken<List<WebEntity>>(){}.getType();
            webEntities = gson.fromJson(br, type);
            webEntities.forEach(we->{
                currentWebEntityId = Math.max(currentWebEntityId, we.getTreeId());
            });
            currentWebEntityId++;
        }
    }
    
    public List<WebEntity> getAll() {
        return webEntities;
    }
}
