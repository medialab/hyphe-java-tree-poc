/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

import com.google.common.base.Strings;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;


/**
 * Utility methods for LRUs.
 *
 * @author benjamin ooghe-tabanou
 */
public class LRUUtil {

    public static final Pattern LRU_STEM_PATTERN = Pattern.compile("\\|[shtpqf]:");

    public static String getLimitedStemsLRU(String lru, int limit) {
        String[] ps = lru.split("\\|");
        String res = "";
        int nocount = 0;
        for(int i = 0; i < ps.length && i-nocount < limit; i++) {
            res += ps[i] + "|";
            if (ps[i].startsWith("s") || ps[i].startsWith("h")) {
                nocount++;
            }
        }
        return res;
    }

    public static String getLRUHead(String lru) {
        String[] ps = lru.split("\\|");
        String res = "";
        int i = 0;
        while (i < ps.length && (ps[i].startsWith("h") || ps[i].startsWith("s") || ps[i].startsWith("t"))) {
            res += ps[i] + "|";
            i++;
        }
        return res;
    }

    public static String stripLRUScheme(String lru) {
        String[] ps = lru.split("\\|");
        String res = "";
        for (String s : ps) {
            if (!(s.startsWith("s:") || s.startsWith("t:"))) {
                res += s + "|";
            }
        }
        return res;
    }

    public static String HTTPVariationLRU(String lru) {
    	String variation = null;
        if (lru.startsWith("s:https|")) {
            variation = lru.replaceFirst("s:https\\|", "s:http|");
        } else if (lru.startsWith("s:http|")) {
            variation = lru.replaceFirst("s:http\\|", "s:https|");
        }
    	return variation;
    }

    public static String WWWVariationLRU(String lru) {
    	String[] ps = lru.split("\\|");
        int i = 0, j;
        String lastSubdomain = "";
        while (i < ps.length) {
            if (ps[i].startsWith("h:")) {
                lastSubdomain = ps[i];
            }
            i++;
        }
        if (lastSubdomain != "") {
            i = lru.lastIndexOf(lastSubdomain);
            j = i + lastSubdomain.length() + 1;
            if (lastSubdomain.equals("h:www")) {
                return lru.substring(0, i) + lru.substring(j);
            } else {
                return lru.substring(0, j) + "h:www|" + lru.substring(j);
            }
        }
        return null;
    }

    public static String HTTPWWWVariationLRU(String lru) {
    	String httpVariation = HTTPVariationLRU(lru);
    	if (httpVariation != null) {
    		return WWWVariationLRU(httpVariation);
    	}
    	return null;
    }

    /**
     * Reverts an lru to an url.
     * null when input is null, empty or blank.
     *
     * @param lru to revert
     * @return url
     */
    public static String revertLRU(String lru) {
        if(lru == null) {
            return null;
        }
        lru = lru.trim();
        if(Strings.isNullOrEmpty(lru)) {
            return null;
        }
        String lruElement, key, scheme = "", host = "", port = "", path = "", query = "", fragment = "";
        Scanner scanner = new Scanner(lru);
        scanner.useDelimiter("\\|");
        while(scanner.hasNext()) {
        	lruElement = scanner.next();
            key = lruElement.substring(0, lruElement.indexOf(':')+1);
            lruElement = lruElement.substring(lruElement.indexOf(':')+1).trim();
            if(!Strings.isNullOrEmpty(lruElement) || key.equals("p:")) {
                if(key.equals("s:")) {
                    scheme = lruElement + "://";
                } else if(key.equals("t:") && ! (lruElement.equals("80") || lruElement.equals("443"))) {
                    port = ":" + lruElement;
                } else if(key.equals("h:")) {
                    if (host == "") {
                        host = lruElement;
                    } else {
                        host = lruElement + "." + host;
                    }
                } else if(key.equals("p:")) {
                    path += "/" + lruElement.trim();
                } else if(key.equals("q:")) {
                    if (host == null) {
                        query = "?" + lruElement;
                    } else {
                        query += "&" + lruElement;
                    }
                } else if(key.equals("f:")) {
                    fragment = "#" + lruElement;
                }
            }
        }
        scanner.close();
        return scheme + host + port + path + query + fragment;
    }

    public static int countHost(String lru) {
        int hosts = 0;
        String[] ps = lru.split("\\|");
        for (String s : ps) {
            if (s.startsWith("h:")) {
                hosts++;
            }
        }
        return hosts;
    }

    public static String nameLRU(String lru) {
    	String lruElement, key, name = "", path = "", lastHost = "", piece;
    	ArrayList<String> host = new ArrayList<String>();
    	boolean pathDone = false;
    	Scanner scanner = new Scanner(lru);
        scanner.useDelimiter("\\|");
        while(scanner.hasNext()) {
        	lruElement = scanner.next();
        	key = lruElement.substring(0, lruElement.indexOf(':')+1);
            lruElement = lruElement.substring(lruElement.indexOf(':')+1).trim();
            piece = URLDecoder.decode(lruElement);
            if(!Strings.isNullOrEmpty(lruElement)) {
            	if(key.equals("h:") && ! lruElement.equals("www")) {
                    lastHost = toTitleCase(lruElement);
                    if (host.size() > 0 || lastHost.length() > 3) {
                        host.add(0, lastHost);
                    }
                } else if(key.equals("p:")) {
                    path = (pathDone ? " /..." : "") + " /" + piece;
                    pathDone = true;
                } else if(key.equals("q:")) {
                    name += " ?" + piece;
                } else if(key.equals("f:")) {
                    name += " #" + piece;
                }
            }
        }
        scanner.close();
        if (host.size() == 0 && !Strings.isNullOrEmpty(lastHost)) {
            host.add(0, lastHost);
        }
    	return String.join(".", host) + path + name;
    }
    
    public static String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }
}