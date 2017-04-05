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
public class WebEntityCreationRules {
    private static final WebEntityCreationRules INSTANCE = new WebEntityCreationRules();
    private final String wecrFileName = System.getProperty("user.dir") + File.separator + "data" + File.separator + "webentitycreationrules.json";
    private List<WebEntityCreationRule> rules = new ArrayList<>();
    private int currentWebEntityId = 1;
    
    // Singleton
    private WebEntityCreationRules(){}
    public static WebEntityCreationRules getInstance() { return INSTANCE; }
    
    public void init() {
        rules = new ArrayList<>();
        try {
            read();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WebEntities.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void create(String prefix, String regexp) throws IOException {
        WebEntityCreationRule wecr = new WebEntityCreationRule();
        wecr.setPrefix(prefix);
        wecr.setRegexp(regexp);
        rules.add(wecr);
        write();
    }
    
    private void write() throws IOException {
        try (Writer writer = new FileWriter(wecrFileName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(rules, writer);
        }
    }
    
    public void read() throws FileNotFoundException {
        File f = new File(wecrFileName);
        if(f.exists() && !f.isDirectory()) {
            Gson gson = new GsonBuilder().create();
            BufferedReader br = new BufferedReader(new FileReader(wecrFileName));
            Type type = new TypeToken<List<WebEntity>>(){}.getType();
            rules = gson.fromJson(br, type);
        }
    }
    
    public List<WebEntityCreationRule> getAll() {
        return rules;
    }
}
