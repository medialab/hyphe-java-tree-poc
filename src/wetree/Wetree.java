/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author jacomyma
 */
public class Wetree {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        String path = System.getProperty("user.dir") + File.separator + "data" + File.separator;
        
        WebEntitiesManager wem;
        wem = new WebEntitiesManager(path);
        
        wem.reset();
//        buildFakeCorpus(wem);
        
        wem.addLru("A");
        wem.addLru("B");
        wem.addLru("C");
        
        wem.addLink("A", "B");
        wem.addLink("A", "C");
        wem.addLink("B", "C");
        
        wem.log();

    }
    
    private static void buildFakeCorpus(WebEntitiesManager wem) throws IOException {
        // Build a fake corpus
        // Settings
        int webentity_count = 1;
        
        // Init
        ArrayList<String> stems = new ArrayList<>();
        stems.add("poney");
        stems.add("vache");
        stems.add("éléphant");
        stems.add("cochon");
        stems.add("rat");
        stems.add("cheval");
        stems.add("chien");
        stems.add("chat");
        stems.add("lion");
        stems.add("giraffe");
        stems.add("colibri");
        stems.add("abeille");
        stems.add("moustique");
        stems.add("sauterelle");
        stems.add("tigre");
        stems.add("loup");
        stems.add("émeu");
        stems.add("perroquet");
        stems.add("rouge-gorge");
        stems.add("étourneau");
        stems.add("ver");
        stems.add("pou");
        stems.add("crabe");
        stems.add("pouple");
        stems.add("requin");
        stems.add("dauphin");
        stems.add("silure");
        stems.add("gougeon");
        stems.add("grenouille");
        
        while (webentity_count-- > 0) {
            
            String[] prefixes = new String[ThreadLocalRandom.current().nextInt(1, 8)];
            for (int i=0; i<prefixes.length; i++) {
                String p = "";
                int prefix_size = ThreadLocalRandom.current().nextInt(2, 5+1);
                while (prefix_size-- > 0) {
                    p += stems.get(ThreadLocalRandom.current().nextInt(0, stems.size())) + "|";
                }
                prefixes[i] = p;
                
                // Pages
                int pages_count = 3;//ThreadLocalRandom.current().nextInt(1, 10+1) * ThreadLocalRandom.current().nextInt(1, 10+1) * ThreadLocalRandom.current().nextInt(1, 10+1) * ThreadLocalRandom.current().nextInt(1, 10+1);
                while (pages_count-- > 0) {
                    String lru = p;
                    int suffix_size = ThreadLocalRandom.current().nextInt(1, 6+1);
                    while (suffix_size-- > 0) {
                        lru += stems.get(ThreadLocalRandom.current().nextInt(0, stems.size())) + "|";
                    }
                    wem.addLru(lru);
                }
            }            
            
            wem.createWebEntity(prefixes);
        }
        
    }
    
}
