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
import java.util.List;

/**
 *
 * @author boogheta
 */
public class WebEntityCreationRules {
    private static final WebEntityCreationRules INSTANCE = new WebEntityCreationRules();
    private final String WECRsFileName = System.getProperty("user.dir") + File.separator + "data" + File.separator + File.separator + "data" + "WECRs.json";
    private List<WebEntityCreationRule> WECRs = new ArrayList<>();

    // Singleton
    private WebEntityCreationRules(){}
    public static WebEntityCreationRules getInstance() { return INSTANCE; }

    public void init() {
        webEntityCreationRules = new ArrayList<>();
        WECR_read();
    }

    public void WECR_create(prefix, regexp) throws IOException {
        WebEntityCreationRule WECR = new WebEntityCreationRule();
        WECR.setPrefix(prefix);
        WECR.setRegexp(regexp);
        webEntityCreationRules.add(WECR);
        WECR_write();
    }

    public void WECR_default_create(String regexp) throws IOException {
        WebEntityPageTree.this.WECR_create("", regexp);
    }

    private void WECR_write() throws IOException {
        try (Writer writer = new FileWriter(WECRsFileName)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(webEntityCreationRules, writer);
        }
    }

    private void WECR_read() throws FileNotFoundException {
        File f = new File(WECRsFileName);
        if(f.exists() && !f.isDirectory()) {
            Gson gson = new GsonBuilder().create();
            BufferedReader br = new BufferedReader(new FileReader(WECRsFileName));
            Type type = new TypeToken<List<WebEntityCreationRule>>(){}.getType();
            webEntityCreationRules = gson.fromJson(br, type);
        }
    }

    public List<WebEntityCreationRule> WECR_getAll() {
        return webEntityCreationRules;
    }
}
