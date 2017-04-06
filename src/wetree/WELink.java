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
public class WELink {
    public final int sourceWebentityid;
    public final int targetWebentityid;
    public WELink(int sourceWebentityid, int targetWebentityid) {
        this.sourceWebentityid = sourceWebentityid;
        this.targetWebentityid = targetWebentityid;
    }
}
