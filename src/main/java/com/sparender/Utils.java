package com.sparender;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by dteixeira on 16.06.17.
 */
public class Utils {


    public static String getFullURL(HttpServletRequest request) {
        String requestURL = request.getPathInfo();
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL + '?' + queryString;
        }
    }
}
