/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wetree;

/**
 *
 * @author boogheta
 */
public class WebEntityCreationRule {
    private String prefix;
    private String regexp;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isDefault() {
        return (this.prefix.length() == 0);
    }

    public String getRegexp() {
        return regexp;
    }

    public void setRegexp(String regexp) {
        this.regexp = regexp;
    }

    public WebEntityCreationRule() {}
}
