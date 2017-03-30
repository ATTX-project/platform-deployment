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

    public String getESSiren() {
        return "http://" + System.getProperty("essiren.host") + ":" + Integer.parseInt(System.getProperty("essiren.port"));
    }

    public String getES5() {
        return "http://" + System.getProperty("es5.host") + ":" + Integer.parseInt(System.getProperty("es5.port"));
    }

    public String getFuseki() {
        return "http://" + System.getProperty("fuseki.host") + ":" + Integer.parseInt(System.getProperty("fuseki.port"));
    }

    public String getUV() {
        return "http://" + System.getProperty("frontend.host") + ":" + Integer.parseInt(System.getProperty("frontend.port"));
    }

    public String getGmapi() {
        return "http://" + System.getProperty("gmapi.host") + ":" + Integer.parseInt(System.getProperty("gmapi.port"));
    }

    public String getWfapi() {
        return "http://" + System.getProperty("wfapi.host") + ":" + Integer.parseInt(System.getProperty("wfapi.port"));
    }

}
