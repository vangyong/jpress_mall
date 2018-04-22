package io.jpress.core;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author heguoliang
 * @Description: TODO(对HttpServletRequestWrapper重写，为了统一XSS处理)
 * @date 2017-6-2 15:53
 */
public class HttpServletRequestWrapper extends javax.servlet.http.HttpServletRequestWrapper{

    public HttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * 重写并过滤getParameter方法
     */
    @Override
    public String getParameter(String name) {
        return getBasicHtmlandimage(super.getParameter(name));
    }

    /**
     * 重写并过滤getParameterValues方法
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (null == values){
            return null;
        }
        for (int i = 0; i < values.length; i++) {
            values[i] = getBasicHtmlandimage(values[i]);
        }
        return values;
    }

    /**
     * 重写并过滤getParameterMap方法
     */
    @Override
    public Map getParameterMap() {
        Map<String, String[]> paraMap = super.getParameterMap();
        Map<String, String[]> reslutMap = new HashMap<String, String[]>();
        if (null == paraMap || paraMap.isEmpty()) {
            return paraMap;
        }
        for (String key : paraMap.keySet()) {
            String[] values = paraMap.get(key);
            if (null == values) {
                continue;
            }
            String[] newValues  = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                newValues[i] = getBasicHtmlandimage(values[i]);
            }
            reslutMap.put(key, newValues);
        }
        return reslutMap;
    }

    public static String getBasicHtmlandimage(String html) {
        if (html == null)
            return null;
        return Jsoup.clean(html, Whitelist.basicWithImages());
    }

}
