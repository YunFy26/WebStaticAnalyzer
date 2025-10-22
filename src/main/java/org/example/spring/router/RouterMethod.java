package org.example.spring.router;

import org.example.spring.rules.EntryMappingRules;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.classes.JMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Entity class including the JMethod and urls
 */
public class RouterMethod {

    private final JMethod jMethod;

    private final EntryMappingRules.HttpMethod httpMethod;

    private final List<String> urls;

    public RouterMethod(JMethod jMethod, EntryMappingRules.HttpMethod httpMethod) {
        this.jMethod = jMethod;
        this.httpMethod = httpMethod;
        this.urls = new ArrayList<>();
    }

    public JMethod getJMethod() {
        return jMethod;
    }

    public void setUrls(List<String> urls) {
        this.urls.addAll(urls);
    }
    public List<String> getUrls() {
        return urls;
    }

}
