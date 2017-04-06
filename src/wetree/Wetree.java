/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.mongodb.client.MongoCursor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

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
        WebEntityPageTree wept;
        wept = WebEntityPageTree.getInstance();
        
        boolean reset = true;
        wept.init(reset);

//        buildFakeCorpus(wept, 5, 100);
        
//        benchmarkRandomWebEntity(wept, true);
//        benchmarkAllWebEntities(wept, false);

        foodchainBenchmark(wept);
        
//        wept.log();

    }
    
    private static void foodchainBenchmark(WebEntityPageTree wept) throws IOException {
        wept.addPage("Plankton:Phytoplankton");
        wept.addPage("Plankton:Zooplankton");
        wept.addPage("Crustacean:Prawn");
        wept.addPage("Fish:Fish");
        wept.addPage("Shell:Mussels");
        wept.addPage("Crustacean:Crab");
        wept.addPage("Bird:Gull");
        wept.addPage("Plant:Seaweed");
        wept.addPage("Shell:Limpets");
        wept.addPage("Shell:Whelk");
        wept.addPage("Crustacean:Lobster");
        
        WebEntities.getInstance().create("Plankton");
        WebEntities.getInstance().create("Crustacean");
        WebEntities.getInstance().create("Fish");
        WebEntities.getInstance().create("Shell");
        WebEntities.getInstance().create("Bird");
        WebEntities.getInstance().create("Plant");
        
        wept.addLink("Plankton:Phytoplankton", "Plankton:Zooplankton");
        wept.addLink("Plankton:Phytoplankton", "Shell:Mussels");
        wept.addLink("Plankton:Zooplankton", "Crustacean:Prawn");
        wept.addLink("Plankton:Zooplankton", "Shell:Mussels");
        wept.addLink("Crustacean:Prawn", "Fish:Fish");
        wept.addLink("Crustacean:Prawn", "Bird:Gull");
        wept.addLink("Shell:Mussels", "Bird:Gull");
        wept.addLink("Shell:Mussels", "Crustacean:Crab");
        wept.addLink("Shell:Mussels", "Crustacean:Lobster");
        wept.addLink("Shell:Mussels", "Shell:Whelk");
        wept.addLink("Crustacean:Crab", "Bird:Gull");
        wept.addLink("Crustacean:Crab", "Crustacean:Lobster");
        wept.addLink("Plant:Seaweed", "Shell:Limpets");
        wept.addLink("Shell:Limpets", "Crustacean:Crab");
        wept.addLink("Shell:Limpets", "Bird:Gull");
        wept.addLink("Shell:Limpets", "Shell:Whelk");
        wept.addLink("Shell:Limpets", "Crustacean:Lobster");
        wept.addLink("Shell:Whelk", "Bird:Gull");
        wept.addLink("Shell:Whelk", "Crustacean:Lobster");
        
        System.out.println("\nWeb Entities:");
        ArrayList<WebEntity> wes = (ArrayList<WebEntity>) WebEntities.getInstance().getAll();
        wes.forEach(we->{
            try {
                System.out.print(we.getId() + ". ");
                we.getPrefixes().forEach(p->{
                    System.out.print(p + " ");
                });
                System.out.println();
                
                System.out.println("   LRUs:");
                ArrayList<String> lrus = wept.getPages(we.getId());
                lrus.forEach(lru->{
                    System.out.println("   - " + lru);
                });
                
                System.out.println("   Links to other web entities:");
                ArrayList<Integer> weids = wept.getWebEntityOutLinks(we.getPrefixes());
                weids.forEach(weid->{
                    System.out.println("   -> " + weid);
                });
                
            } catch (IOException ex) {
                Logger.getLogger(Wetree.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        ArrayList<String[]> links = wept._geAllLruLinks_SLOW();
        System.out.println("\nLRU Links:");
        links.forEach(link->{
            System.out.println(link[0] + " -> " + link[1]);
        });
    }
    
    private static void benchmarkAllWebEntities(WebEntityPageTree wept, boolean display) {
        ArrayList<WebEntity> wes = (ArrayList<WebEntity>) WebEntities.getInstance().getAll();
        wes.forEach(we->{
            try {
                benchmarkWebEntity(wept, we, display);
            } catch (IOException ex) {
                Logger.getLogger(Wetree.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    
    private static void benchmarkRandomWebEntity(WebEntityPageTree wept, boolean display) throws IOException {
        ArrayList<WebEntity> wes = (ArrayList<WebEntity>) WebEntities.getInstance().getAll();
        WebEntity we = wes.get(ThreadLocalRandom.current().nextInt(1, wes.size()));
        benchmarkWebEntity(wept, we, display);
    }
    
    private static void benchmarkWebEntity(WebEntityPageTree wept, WebEntity we, boolean display) throws IOException {
        try {
            if (display) {
                System.out.print(we.getId() + ". ");
                we.getPrefixes().forEach(p->{
                    System.out.print(p + " ");
                });
                System.out.println();
            }

            if (display) System.out.println("   LRUs:");
            ArrayList<String> lrus = wept.getPages(we.getId());
            if (display) lrus.forEach(lru->{
                System.out.println("   - " + lru);
            });

            if (display) System.out.println("   Links to other web entities:");
            ArrayList<Integer> weids = wept.getWebEntityOutLinks(we.getPrefixes());
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


    private static void buildFakeCorpus(WebEntityPageTree wept, int webentity_count, int link_count) throws IOException {
        
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
                    wept.addPage(lru);
                }
            }            
            
            WebEntities.getInstance().create(prefixes);
        }
        System.out.println(" done.");
        System.out.println(lrus.size() + " LRUs were created in that process.");
        
        // Create random links
        System.out.print("Creating " + link_count + " random LRU links...");
        while (link_count-- > 0) {
            String sourcelru = lrus.get(ThreadLocalRandom.current().nextInt(1, lrus.size()));
            String targetlru = lrus.get(ThreadLocalRandom.current().nextInt(1, lrus.size()));
            wept.addLink(sourcelru, targetlru);
        }
        System.out.println(" done.");
    }
    
    private static void mongoTest() {

        // Instantiating and resetting DB files
        WebEntityPageTree wept;
        wept = WebEntityPageTree.getInstance();
        try {
            wept.init(true);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Wetree.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Creating a mongo cursor
        MongoConnector connector = new MongoConnector();
        MongoCursor<Document> cursor = connector.getPagesCursor();

        int i = 0;
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            String lru = doc.getString("lru");

            // First we need to add the page's lru
            wept.addPage(lru);

            // Second we need to add the page's lrulinks
            List<String> lrulinks = (List<String>) doc.get("lrulinks");
            System.out.println((i++) + " (" + lrulinks.size() + ") " + lru);

            lrulinks.forEach(lrulink->{
              wept.addPage(lrulink);
                try {
                    wept.addLink(lru, lrulink);
                } catch (IOException ex) {
                    Logger.getLogger(Wetree.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
    }
}
