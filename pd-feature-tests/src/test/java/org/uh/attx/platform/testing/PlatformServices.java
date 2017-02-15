/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.uh.attx.platform.testing;

/**
 *
 * @author jkesanie
 */
public class PlatformServices {

    private final String ESSIREN = "http://essiren";
    private final int ESSIREN_PORT = 9200;

    private final String ES5 = "http://es5";
    private final int ES5_PORT = 9210;

    private final String FUSEKI = "http://fuseki";
    private final int FUSEKI_PORT = 3030;

    private final String GMAPI = "http://gmapi";
    private final int GMAPI_PORT = 4302;

    
    private final String UV = "http://frontend";
    private final int UV_PORT = 8080;

    private boolean isLocalhost = false;

    public PlatformServices() {
    }

    public PlatformServices(boolean isLocalhost) {
        this.isLocalhost = isLocalhost;
    }

    public String getESSiren() {
        if (isLocalhost) {
            return "http://localhost:" + ESSIREN_PORT;
        } else {
            return ESSIREN + ":" + ESSIREN_PORT;
        }
    }
    
    public String getES5() {
        if (isLocalhost) {
            return "http://localhost:" + ES5_PORT;
        } else {
            return ES5 + ":" + ES5_PORT;
        }
    }    

    public String getFuseki() {
        if (isLocalhost) {
            return "http://localhost:" + FUSEKI_PORT;
        } else {
            return FUSEKI + ":" + FUSEKI_PORT;
        }
    }
    
    public String getUV() {
        if (isLocalhost) {
            return "http://localhost:" + UV_PORT;
        } else {
            return UV + ":" + UV_PORT;
        }
    }    

    public String getGmapi() {
        if (isLocalhost) {
            return "http://localhost:" + GMAPI_PORT;
        } else {
            return GMAPI + ":" + GMAPI_PORT;
        }
    }

}
