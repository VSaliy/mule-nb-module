/**
 *
 */
package org.mule.module.async.netty.utils;

import org.mule.transport.http.HttpConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.NameValuePair;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.util.CharsetUtil;

public class NettyUtils
{

    public static final String DEFAULT_ENCODING = CharsetUtil.ISO_8859_1.name();

    public Map<String, Object> toMap(List<Map.Entry<String, String>> headers)
    {
        final Map<String, Object> result = new HashMap<String, Object>();
        for (Map.Entry<String, String> header : headers)
        {
            result.put(header.getKey(), header.getValue());
        }
        return result;

    }

    public String getEncoding(HttpRequest request)
    {
        return getEncoding(toMap(request.getHeaders()));
    }

    private String getEncoding(Map<String, Object> headers)
    {

        Object contentType = headers.get(HttpConstants.HEADER_CONTENT_TYPE);
        return getEncoding(contentType);
    }

    public String getEncoding(Object contentType)
    {
        String encoding = DEFAULT_ENCODING;
        if (contentType != null)
        {
            // use HttpClient classes to parse the charset part from the Content-Type
            // header (e.g. "text/html; charset=UTF-16BE")
            Header contentTypeHeader = new Header(HttpConstants.HEADER_CONTENT_TYPE,
                                                  contentType.toString());
            HeaderElement values[] = contentTypeHeader.getElements();
            if (values.length == 1)
            {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null)
                {
                    encoding = param.getValue();
                }
            }
        }
        return encoding;
    }

}
