/**
 *
 */
package org.mule.module.async.netty.source;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class HttpUriHelper
{

    private static final String PARAMETER_SEPARATOR = "&";
    private static final String NAME_VALUE_SEPARATOR = "=";
    public static final String QUERY_STRING_SEPARATOR = "?";

    public static String parseQueryString(String uri)
    {
        int i = uri.indexOf("?");
        String queryString = "";
        if (i > -1)
        {
            queryString = uri.substring(i + 1);

        }
        return queryString;
    }

    public static String parsePath(String uri)
    {
        int i = uri.indexOf("?");
        String queryString = uri;
        if (i > -1)
        {
            queryString = uri.substring(0, i);

        }
        return queryString;
    }

    public static Map<String, Object> parseQueryParams(String uri, String encoding) throws UnsupportedEncodingException
    {
        Map<String, Object> httpParams = new HashMap<String, Object>();

        int i = uri.indexOf(QUERY_STRING_SEPARATOR);
        if (i > -1)
        {
            String queryString = uri.substring(i + 1);
            for (StringTokenizer st = new StringTokenizer(queryString, PARAMETER_SEPARATOR); st.hasMoreTokens(); )
            {
                String token = st.nextToken();
                int idx = token.indexOf(NAME_VALUE_SEPARATOR);
                if (idx < 0)
                {
                    addQueryParamToMap(httpParams, unescape(token, encoding), null);
                }
                else if (idx > 0)
                {
                    addQueryParamToMap(httpParams, unescape(token.substring(0, idx), encoding),
                                       unescape(token.substring(idx + 1), encoding));
                }
            }
        }

        return httpParams;
    }

    private static void addQueryParamToMap(Map<String, Object> httpParams, String key, String value)
    {
        Object existingValue = httpParams.get(key);
        if (existingValue == null)
        {
            httpParams.put(key, value);
        }
        else if (existingValue instanceof List)
        {
            List<String> list = (List<String>) existingValue;
            list.add(value);
        }
        else if (existingValue instanceof String)
        {
            List<String> list = new ArrayList<String>();
            list.add((String) existingValue);
            list.add(value);
            httpParams.put(key, list);
        }
    }

    private static String unescape(String escapedValue, String encoding) throws UnsupportedEncodingException
    {
        if (escapedValue != null)
        {
            return URLDecoder.decode(escapedValue, encoding);
        }
        return escapedValue;
    }

}
