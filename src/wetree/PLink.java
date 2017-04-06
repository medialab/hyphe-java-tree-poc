/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

/**
 *
 * @author jacomyma
 */
public class PLink {
    public final String sourcePage;
    public final String targetPage;
    public PLink(String sourcePage, String targetPage) {
        this.sourcePage = sourcePage;
        this.targetPage = targetPage;
    }
}
