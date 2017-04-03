/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.google.common.primitives.Chars;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jacomyma
 */
public class Wetree {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        String path = "D:\\temp\\TEST_WETREE.dat";
        
        WebEntityTree wet;
        wet = new WebEntityTree(path);
        
        wet.reset();
        buildFakeCorpus(wet);

//        // Log web entities
//        HashMap<Integer, ArrayList<String>> webentities = wet.getWebEntities();
//        webentities.forEach((weid, prefixes)->{
//            System.out.println("Web entity "+weid+" prefixes:");
//            prefixes.forEach(lru->{
//               System.out.println("  > " + lru); 
//            });
//        });
        
//        // Display all LRUs
//        ArrayList<String> lrus = wet.getLrus();
//        System.out.println(lrus.size() + " lrus in the file");
//        lrus.forEach(lru -> {
//            System.out.println(lru + " (" + lru.length() + ")");
//        });
        
//        // List the LRUs of a web entity
//        String[] prefixes = new String[1];
//        prefixes[0] = "pou|loup|ver|grenouille|";
//        wet.getLrusFromWebEntity(prefixes, 6201).forEach(lru -> {
//            System.out.println(lru);
//        });
        
        /*
        // Build web entity 1
        String[] we1prefixes = new String[3];
        we1prefixes[0] = "com|google|";
        we1prefixes[1] = "com|google|maps|";
        we1prefixes[2] = "fr|google|";

        // Build web entity 2
        String[] we2prefixes = new String[2];
        we2prefixes[0] = "com|google|images";
        we2prefixes[1] = "fr|google|images";

        // Store web entities
        for(String lru : we1prefixes) {
            wet.addWebEntityPrefix(lru, 1);
        }
        for(String lru : we2prefixes) {
            wet.addWebEntityPrefix(lru, 2);
        }
        
        wet.addLru("com|google|home|");
        wet.addLru("com|google|maps|");
        wet.addLru("com|google|maps|home");
        wet.addLru("fr|google|home");
        wet.addLru("com|google|images");
        wet.addLru("com|google|images|home");
        wet.addLru("fr|google|images|home");
        
        wet.log();
        */        
    }
    
    private static void buildFakeCorpus(WebEntityTree wet) throws IOException {
        // Build a fake corpus
        // Settings
        int webentity_count = 3;
        
        // Init
        int currentweid = 1;
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
            
            // Web entity prefix
            String webentity_prefix = "";
            int prefix_size = ThreadLocalRandom.current().nextInt(2, 5+1);
            while (prefix_size-- > 0) {
                webentity_prefix += stems.get(ThreadLocalRandom.current().nextInt(0, stems.size())) + "|";
            }
            
            System.out.println("Web entity " + currentweid + " has prefixes:");
            System.out.println(webentity_prefix);
            
            wet.addWebEntityPrefix(webentity_prefix, currentweid);
            currentweid++;
            
            // Pages
            int pages_count = ThreadLocalRandom.current().nextInt(1, 10+1) * ThreadLocalRandom.current().nextInt(1, 10+1) * ThreadLocalRandom.current().nextInt(1, 10+1) * ThreadLocalRandom.current().nextInt(1, 10+1);
            while (pages_count-- > 0) {
                String lru = webentity_prefix;
                int suffix_size = ThreadLocalRandom.current().nextInt(1, 6+1);
                while (suffix_size-- > 0) {
                    lru += stems.get(ThreadLocalRandom.current().nextInt(0, stems.size())) + "|";
                }
                wet.addLru(lru);
            }
        }
        
    }
    
}
