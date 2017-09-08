/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Define what to display on the page.
 */
public class PageDefinition {

    private String pageTitle;

    private List<PageItem> pageItems = new ArrayList<>();

    public PageDefinition(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    /**
     * Add an item on the page.
     *
     * @param pageItem
     *            the page item
     * @return this
     */
    public PageDefinition addPageItem(PageItem pageItem) {
        pageItems.add(pageItem);
        return this;
    }

    /**
     * Get a specific field.
     *
     * @param fieldName
     *            the field name
     * @return the field if present
     */
    public Optional<FieldPageItem> getField(String fieldName) {
        return pageItems.stream() //
                .filter(it -> it instanceof FieldPageItem) //
                .map(it -> (FieldPageItem) it) //
                .filter(it -> fieldName.equals(it.getFieldName())) //
                .findAny();
    }

    public List<? extends PageItem> getPageItems() {
        return pageItems;
    }

    public String getPageTitle() {
        return pageTitle;
    }

}
