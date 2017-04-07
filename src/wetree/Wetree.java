/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.client.MongoCursor;
import com.opencsv.CSVWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
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
        wept.setDefaultWecreationrule(WebEntityCreationRules.RULE_DOMAIN);
        
        boolean reset = true;
        wept.init(reset);

        buildFakeCorpus(wept, 100, 100000);

//        List<WELink> links = wept.getWelinks();
//        System.out.println(links.size() + " links");
        
//        Set<Integer> wes = wept._getAllWebEntities_SLOW();
//        System.out.println("Web Entities: " + wes.size());

//        lruBenchmark(wept);
        exportWebentitiesCSV(wept);
        
//        wept.log();

    }
    
    private static void lruBenchmark(WebEntityPageTree wept) throws IOException {
        wept.addWecreationrule(WebEntityCreationRules.getInstance().create("s:http|h:com|h:twitter|", WebEntityCreationRules.RULE_PATH1));
        wept.addPage("s:http|h:com|h:site|p:people|p:papa|");
        wept.addPage("s:http|h:com|h:site|p:people|p:maman|");
        wept.addPage("s:http|h:com|h:site|h:www|");
        wept.addPage("s:http|h:com|h:site|h:www|p:people|p:maman|");

        wept.addPage("s:http|h:com|h:twitter|h:www|");
        wept.addPage("s:http|h:com|h:twitter|p:papa|");
        wept.addPage("s:http|h:com|h:twitter|p:pépé|");
        wept.addPage("s:http|h:com|h:twitter|p:pépé|today|");
        wept.addPage("s:http|h:com|h:twitter|p:pépé|yesterday|");
        
        wept.addPlink("s:http|h:com|h:site|p:people|p:papa|", "s:http|h:com|h:site|p:people|p:maman|");
        wept.addPlink("s:http|h:com|h:site|p:people|p:maman|", "s:http|h:com|h:site|p:people|p:papa|");
        
        wept.addPlink("s:http|h:com|h:site|p:people|p:papa|", "s:http|h:com|h:twitter|p:papa|");
        
        ArrayList<PLink> twitterHomeLinks = new ArrayList<>();
        twitterHomeLinks.add(new PLink("s:http|h:com|h:twitter|p:papa|", "s:http|h:com|h:twitter|h:www|"));
        twitterHomeLinks.add(new PLink("s:http|h:com|h:twitter|p:pépé|", "s:http|h:com|h:twitter|h:www|"));
        twitterHomeLinks.add(new PLink("s:http|h:com|h:twitter|p:pépé|today|", "s:http|h:com|h:twitter|h:www|"));
        twitterHomeLinks.add(new PLink("s:http|h:com|h:twitter|p:pépé|yesterday|", "s:http|h:com|h:twitter|h:www|"));
        wept.addPlinks(twitterHomeLinks);
        
        System.out.println("\nLRUs:");
        wept._getAllLrus_SLOW().forEach(lru->{
            System.out.println(lru + "  " + wept.getPageIndegree(lru) + "=>|=>" + wept.getPageOutdegree(lru));
        });
        
        System.out.println("\nLRU Links:");
        wept._geAllLruLinks_SLOW().forEach(link->{
            System.out.println(link[0] + " -> " + link[1]);
        });
        
        System.out.println("\nWeb Entities:");
        wept.getWebentities().forEach(we->{
            System.out.println(we.getId() + ". " + we.getName());
            System.out.print("   Prefixes: ");
            we.getPrefixes().forEach(p->{
                System.out.print(p + " ");
            });
            System.out.println();
            System.out.println("   LRUs:");
            wept.getPages(we.getId()).forEach(lru->{
                System.out.println("   - " + lru);
            });
            System.out.println("   W. E. Links:");
            List<WELink> weLinks = wept.getWelinksOutbound(we.getId());
            weLinks.forEach(weLink->{
                System.out.println("   -> " + weLink.targetWebentityid);
            });
        });
        
        System.out.println("\n:: Test getPages(String prefix) on: 's:http|h:com|h:site|h:www|p:people|p:maman|'");
        wept.getPages("s:http|h:com|h:site|h:www|").forEach(lru->{
            System.out.println("   - " + lru);
        });

        System.out.println("\n:: Test getPrefix(String lru) on: 's:http|h:com|h:site|h:www|p:people|p:maman|'");
        System.out.println("   > " + wept.getPrefix("s:http|h:com|h:site|h:www|p:people|p:maman|"));
                
        System.out.println("\n:: Test getWebentity(String lru) on: 's:http|h:com|h:site|h:www|p:people|p:maman|'");
        System.out.println("   > " + wept.getWebentity("s:http|h:com|h:site|h:www|p:people|p:maman|"));

        System.out.println("\n:: Test getPlinks(String page) on: 's:http|h:com|h:site|p:people|p:papa|'");
        wept.getPlinks("s:http|h:com|h:site|p:people|p:papa|").forEach(pLink->{
            System.out.println("   " + pLink.sourcePage + " => " + pLink.targetPage);
        });

        System.out.println("\n:: Test getPlinksInbound(String page) on: 's:http|h:com|h:site|p:people|p:papa|'");
        wept.getPlinksInbound("s:http|h:com|h:site|p:people|p:papa|").forEach(pLink->{
            System.out.println("   " + pLink.sourcePage + " => " + pLink.targetPage);
        });

        System.out.println("\n:: Test getPlinksOutbound(String page) on: 's:http|h:com|h:site|p:people|p:papa|'");
        wept.getPlinksOutbound("s:http|h:com|h:site|p:people|p:papa|").forEach(pLink->{
            System.out.println("   " + pLink.sourcePage + " => " + pLink.targetPage);
        });
        
        System.out.println("\n:: Test getWelinks(String page) on: 's:http|h:com|h:site|p:people|p:papa|'");
        wept.getWelinks("s:http|h:com|h:site|p:people|p:papa|").forEach(weLink->{
            System.out.println("   " + weLink.sourceWebentityid + " => " + weLink.targetWebentityid);
        });
        
        System.out.println("\n:: Test getWelinksInbound(String page) on: 's:http|h:com|h:site|p:people|p:papa|'");
        wept.getWelinksInbound("s:http|h:com|h:site|p:people|p:papa|").forEach(weLink->{
            System.out.println("   " + weLink.sourceWebentityid + " => " + weLink.targetWebentityid);
        });
        
        System.out.println("\n:: Test getWelinksOutbound(String page) on: 's:http|h:com|h:site|p:people|p:papa|'");
        wept.getWelinksOutbound("s:http|h:com|h:site|p:people|p:papa|").forEach(weLink->{
            System.out.println("   " + weLink.sourceWebentityid + " => " + weLink.targetWebentityid);
        });
        
//        wept.log();
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
            
            int prefix_count = ThreadLocalRandom.current().nextInt(1, 2+1);
            ArrayList<String> prefixes = new ArrayList<String>(prefix_count);
            for (int i=0; i<prefix_count; i++) {
                String p = "s:http|";
                int prefix_size = ThreadLocalRandom.current().nextInt(2, 5+1);
                while (prefix_size-- > 0) {
                    p += "h:" + stems.get(ThreadLocalRandom.current().nextInt(0, stems.size())) + "|";
                }
                prefixes.add(p);
                
                // Pages
                int pages_count = (int) Math.floor(Math.pow(2, ThreadLocalRandom.current().nextInt(0, 10+1)));
                while (pages_count-- > 0) {
                    String lru = p;
                    int suffix_size = ThreadLocalRandom.current().nextInt(1, 6+1);
                    while (suffix_size-- > 0) {
                        lru += "p:" + stems.get(ThreadLocalRandom.current().nextInt(0, stems.size())) + "|";
                    }
                    lrus.add(lru);
                    wept.addPage(lru);
                }
            }            
            
            if (ThreadLocalRandom.current().nextInt(0, 3) == 0) WebEntities.getInstance().create(prefixes);
        }
        System.out.println(" done.");
        System.out.println(lrus.size() + " LRUs were created in that process.");
        
        // Create random links
        System.out.print("Creating " + link_count + " random LRU links...");
        ArrayList<PLink> links = new ArrayList<>();
        while (link_count-- > 0) {
            links.add(new PLink(lrus.get(ThreadLocalRandom.current().nextInt(1, lrus.size())), lrus.get(ThreadLocalRandom.current().nextInt(1, lrus.size()))));
        }
        wept.addPlinks(links);
        System.out.println(" done.");
    }
    
    private static void mongoTest() {

        // Instantiating and resetting DB files
        WebEntityPageTree wept;
        wept = WebEntityPageTree.getInstance();
        wept.setDefaultWecreationrule(WebEntityCreationRules.RULE_DOMAIN);
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

            ArrayList<PLink> plinks = new ArrayList<>();

            lrulinks.forEach((String lrulink)->{
               plinks.add(new PLink(lru, lrulink));
            });

            wept.addPlinks(plinks);
        }
    }
    
    private static void exportWebentitiesCSV(WebEntityPageTree wept) throws IOException {
        CSVWriter writer = new CSVWriter(new FileWriter("data/webentities.csv"), ',');
        // feed in your array (or convert your data to an array)
        String[] headEntries = "id,name,prefixes,outlinks".split(",");
        writer.writeNext(headEntries);
        
        // Get All Links
        Multimap<Integer, Integer> outlinks = ArrayListMultimap.create();
        wept.getWelinks().forEach(weLink->{
            outlinks.put(weLink.sourceWebentityid, weLink.targetWebentityid);
        });
        
        wept.getWebentities().forEach(we->{
            ArrayList<String> links = new ArrayList<>();
            outlinks.get(we.getId()).forEach(we2id->{
                links.add(we2id.toString());
            });
            
            String[] entries = new String[4];
            entries[0] = we.getId().toString();
            entries[1] = we.getName();
            entries[2] = String.join(",", we.getPrefixes());
            entries[3] = String.join(",", links);
            
            writer.writeNext(entries);
        });
        
        writer.close();
    }
}
