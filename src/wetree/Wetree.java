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
import java.util.logging.Level;
import java.util.logging.Logger;

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
        buildFakeCorpus(wem, 1500, 5000000);
        
//        benchmarkRandomWebEntity(wem);
//        benchmarkAllWebEntities(wem, false);

//        foodchainBenchmark(wem);
        
//        wem.log();

    }
    
    private static void foodchainBenchmark(WebEntitiesManager wem) throws IOException {
        wem.addLru("Plankton:Phytoplankton");
        wem.addLru("Plankton:Zooplankton");
        wem.addLru("Crustacean:Prawn");
        wem.addLru("Fish:Fish");
        wem.addLru("Shell:Mussels");
        wem.addLru("Crustacean:Crab");
        wem.addLru("Bird:Gull");
        wem.addLru("Plant:Seaweed");
        wem.addLru("Shell:Limpets");
        wem.addLru("Shell:Whelk");
        wem.addLru("Crustacean:Lobster");
        
        wem.webentity_create("Plankton");
        wem.webentity_create("Crustacean");
        wem.webentity_create("Fish");
        wem.webentity_create("Shell");
        wem.webentity_create("Bird");
        wem.webentity_create("Plant");
        
        wem.addLink("Plankton:Phytoplankton", "Plankton:Zooplankton");
        wem.addLink("Plankton:Phytoplankton", "Shell:Mussels");
        wem.addLink("Plankton:Zooplankton", "Crustacean:Prawn");
        wem.addLink("Plankton:Zooplankton", "Shell:Mussels");
        wem.addLink("Crustacean:Prawn", "Fish:Fish");
        wem.addLink("Crustacean:Prawn", "Bird:Gull");
        wem.addLink("Shell:Mussels", "Bird:Gull");
        wem.addLink("Shell:Mussels", "Crustacean:Crab");
        wem.addLink("Shell:Mussels", "Crustacean:Lobster");
        wem.addLink("Shell:Mussels", "Shell:Whelk");
        wem.addLink("Crustacean:Crab", "Bird:Gull");
        wem.addLink("Crustacean:Crab", "Crustacean:Lobster");
        wem.addLink("Plant:Seaweed", "Shell:Limpets");
        wem.addLink("Shell:Limpets", "Crustacean:Crab");
        wem.addLink("Shell:Limpets", "Bird:Gull");
        wem.addLink("Shell:Limpets", "Shell:Whelk");
        wem.addLink("Shell:Limpets", "Crustacean:Lobster");
        wem.addLink("Shell:Whelk", "Bird:Gull");
        wem.addLink("Shell:Whelk", "Crustacean:Lobster");
        
        System.out.println("\nWeb Entities:");
        ArrayList<WebEntity> wes = (ArrayList<WebEntity>) wem.webentity_getAll();
        wes.forEach(we->{
            try {
                System.out.print(we.getId() + ". ");
                we.getPrefixes().forEach(p->{
                    System.out.print(p + " ");
                });
                System.out.println();
                
                System.out.println("   LRUs:");
                ArrayList<String> lrus = wem.getLrusFromWebEntity(we.getPrefixes());
                lrus.forEach(lru->{
                    System.out.println("   - " + lru);
                });
                
                System.out.println("   Links to other web entities:");
                ArrayList<Integer> weids = wem.getWebEntityOutLinks(we.getPrefixes());
                weids.forEach(weid->{
                    System.out.println("   -> " + weid);
                });
                
            } catch (IOException ex) {
                Logger.getLogger(Wetree.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        ArrayList<String[]> links = wem._geAllLruLinks_SLOW();
        System.out.println("\nLRU Links:");
        links.forEach(link->{
            System.out.println(link[0] + " -> " + link[1]);
        });
    }
    
    private static void benchmarkAllWebEntities(WebEntitiesManager wem, boolean display) {
        ArrayList<WebEntity> wes = (ArrayList<WebEntity>) wem.webentity_getAll();
        wes.forEach(we->{
            try {
                benchmarkWebEntity(wem, we, display);
            } catch (IOException ex) {
                Logger.getLogger(Wetree.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    
    private static void benchmarkRandomWebEntity(WebEntitiesManager wem, boolean display) throws IOException {
        ArrayList<WebEntity> wes = (ArrayList<WebEntity>) wem.webentity_getAll();
        WebEntity we = wes.get(ThreadLocalRandom.current().nextInt(1, wes.size()));
        benchmarkWebEntity(wem, we, display);
    }
    
    private static void benchmarkWebEntity(WebEntitiesManager wem, WebEntity we, boolean display) throws IOException {
        try {
            if (display) {
                System.out.print(we.getId() + ". ");
                we.getPrefixes().forEach(p->{
                    System.out.print(p + " ");
                });
                System.out.println();
            }

            if (display) System.out.println("   LRUs:");
            ArrayList<String> lrus = wem.getLrusFromWebEntity(we.getPrefixes());
            if (display) lrus.forEach(lru->{
                System.out.println("   - " + lru);
            });

            if (display) System.out.println("   Links to other web entities:");
            ArrayList<Integer> weids = wem.getWebEntityOutLinks(we.getPrefixes());
            if (display) weids.forEach(weid->{
                System.out.println("   -> " + weid);
            });
            
            if (!display) {
                System.out.println("Web Entity " + we.getId() + ": retrieved " + lrus.size() + " LRUs and links to " + weids.size() + " web entities");
            }
        } catch (IOException ex) {
            Logger.getLogger(Wetree.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    private static void buildFakeCorpus(WebEntitiesManager wem, int webentity_count, int link_count) throws IOException {
        
        // Init
        ArrayList<String> lrus = new ArrayList<>();
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
        
        System.out.print("Creating " + webentity_count + " web entities with pages...");
        while (webentity_count-- > 0) {
            
            String[] prefixes = new String[ThreadLocalRandom.current().nextInt(1, 8+1)];
            for (int i=0; i<prefixes.length; i++) {
                String p = "";
                int prefix_size = ThreadLocalRandom.current().nextInt(2, 5+1);
                while (prefix_size-- > 0) {
                    p += stems.get(ThreadLocalRandom.current().nextInt(0, stems.size())) + "|";
                }
                prefixes[i] = p;
                
                // Pages
                int pages_count = (int) Math.floor(Math.pow(2, ThreadLocalRandom.current().nextInt(0, 10+1)));
                while (pages_count-- > 0) {
                    String lru = p;
                    int suffix_size = ThreadLocalRandom.current().nextInt(1, 6+1);
                    while (suffix_size-- > 0) {
                        lru += stems.get(ThreadLocalRandom.current().nextInt(0, stems.size())) + "|";
                    }
                    lrus.add(lru);
                    wem.addLru(lru);
                }
            }            
            
            wem.webentity_create(prefixes);
        }
        System.out.println(" done.");
        System.out.println(lrus.size() + " LRUs were created in that process.");
        
        // Create random links
        System.out.print("Creating " + link_count + " random LRU links...");
        while (link_count-- > 0) {
            String sourcelru = lrus.get(ThreadLocalRandom.current().nextInt(1, lrus.size()));
            String targetlru = lrus.get(ThreadLocalRandom.current().nextInt(1, lrus.size()));
            wem.addLink(sourcelru, targetlru);
        }
        System.out.println(" done.");
    }
    
}
