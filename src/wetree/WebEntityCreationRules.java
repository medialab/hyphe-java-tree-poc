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
import java.util.HashMap;
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
    private HashMap<Integer, WebEntityCreationRule> rulesIndex = new HashMap<>();
    private int currentWECRId = 1;
    
    public static final String RULE_DOMAIN = "(s:[a-zA-Z]+\\|(t:[0-9]+\\|)?(h:[^\\|]+\\|(h:[^\\|]+\\|)|h:(localhost|(\\d{1,3}\\.){3}\\d{1,3}|\\[[\\da-f]*:[\\da-f:]*\\])\\|))";
    public static final String RULE_SUBDOMAIN = "(s:[a-zA-Z]+\\|(t:[0-9]+\\|)?(h:[^\\|]+\\|(h:[^\\|]+\\|)+|h:(localhost|(\\d{1,3}\\.){3}\\d{1,3}|\\[[\\da-f]*:[\\da-f:]*\\])\\|))";
    public static final String RULE_PATH_1 = "(s:[a-zA-Z]+\\|(t:[0-9]+\\|)?(h:[^\\|]+\\|(h:[^\\|]+\\|)+|h:(localhost|(\\d{1,3}\\.){3}\\d{1,3}|\\[[\\da-f]*:[\\da-f:]*\\])\\|)(p:[^\\|]+\\|){1})";
    public static final String RULE_PATH_2 = "(s:[a-zA-Z]+\\|(t:[0-9]+\\|)?(h:[^\\|]+\\|(h:[^\\|]+\\|)+|h:(localhost|(\\d{1,3}\\.){3}\\d{1,3}|\\[[\\da-f]*:[\\da-f:]*\\])\\|)(p:[^\\|]+\\|){2})";
    
    // Singleton
    private WebEntityCreationRules(){}
    public static WebEntityCreationRules getInstance() { return INSTANCE; }
    
    public void reset() {
        rules = new ArrayList<>();
        rulesIndex = new HashMap<>();
    }
    
    public WebEntityCreationRule create(String prefix, String regexp) {
        WebEntityCreationRule wecr = new WebEntityCreationRule();
        wecr.setPrefix(prefix);
        wecr.setRegexp(regexp);
        wecr.setId(currentWECRId++);
        rules.add(wecr);
        rulesIndex.put(wecr.getId(), wecr);
        write();
        return wecr;
    }
    
    public void add(WebEntityCreationRule wecr) {
        rules.add(wecr);
        rulesIndex.put(wecr.getId(), wecr);
        write();
    }
    
    private void write() {
        try (Writer writer = new FileWriter(wecrFileName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(rules, writer);
        } catch (IOException ex) {
            Logger.getLogger(WebEntityCreationRules.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void read() throws FileNotFoundException {
        rules = new ArrayList<>();
        rulesIndex = new HashMap<>();
        File f = new File(wecrFileName);
        if(f.exists() && !f.isDirectory()) {
            Gson gson = new GsonBuilder().create();
            BufferedReader br = new BufferedReader(new FileReader(wecrFileName));
            Type type = new TypeToken<List<WebEntity>>(){}.getType();
            rules = gson.fromJson(br, type);
            rules.forEach(rule->{
                currentWECRId = Math.max(currentWECRId, rule.getId());
                rulesIndex.put(rule.getId(), rule);
            });
            currentWECRId++;
        }
    }
    
    public List<WebEntityCreationRule> getAll() {
        return rules;
    }
    
    public WebEntityCreationRule get(int wecrid) {
        return rulesIndex.get(wecrid);
    }
}
