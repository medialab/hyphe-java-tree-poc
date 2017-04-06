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
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author jacomyma
 */
public class WebEntities {
    private static final WebEntities INSTANCE = new WebEntities();
    private final String webentitiesFileName = System.getProperty("user.dir") + File.separator + "data" + File.separator + "webentities.json";
    private List<WebEntity> webEntities;
    private HashMap<Integer, WebEntity> webEntitiesIndex;
    private int currentWebEntityId = 1;
    
    // Singleton
    private WebEntities(){}
    public static WebEntities getInstance() { return INSTANCE; }
    
    public void reset() {
        webEntities = new ArrayList<>();
        webEntitiesIndex = new HashMap<>();
    }

    public WebEntity create(List<String> prefixes) throws IOException {
        WebEntity we = new WebEntity();
        we.setId(currentWebEntityId++);
        we.setPrefixes(prefixes);
        webEntities.add(we);
        webEntitiesIndex.put(we.getId(), we);
        write();
        we.getPrefixes().forEach(lru->{
            WebEntityPageTree.getInstance().associatePrefixWithWebentity(lru, we.getId());
        });
        return we;
    }
    
    public WebEntity create(String prefix) throws IOException {
        ArrayList<String> prefixes = new ArrayList<String>(1);
        prefixes.add(prefix);
        return create(prefixes);
    }
    
    private void write() throws IOException {
        try (Writer writer = new FileWriter(webentitiesFileName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(webEntities, writer);
        }
    }
    
    public void read() throws FileNotFoundException {
        webEntities = new ArrayList<>();
        webEntitiesIndex = new HashMap<>();
        File f = new File(webentitiesFileName);
        if(f.exists() && !f.isDirectory()) {
            Gson gson = new GsonBuilder().create();
            BufferedReader br = new BufferedReader(new FileReader(webentitiesFileName));
            Type type = new TypeToken<List<WebEntity>>(){}.getType();
            webEntities = gson.fromJson(br, type);
            webEntities.forEach(we->{
                currentWebEntityId = Math.max(currentWebEntityId, we.getId());
                webEntitiesIndex.put(we.getId(), we);
            });
            currentWebEntityId++;
        }
    }
    
    public List<WebEntity> getAll() {
        return webEntities;
    }
    
    public WebEntity get(int weid) {
        return webEntitiesIndex.get(weid);
    }
}
