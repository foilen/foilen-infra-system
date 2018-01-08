/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.core.visual.pageItem;

import java.util.ArrayList;
import java.util.List;

import com.foilen.infra.plugin.v1.core.visual.PageItem;
import com.google.common.base.MoreObjects;

/**
 * A list to display.
 */
public class ListPageItem implements PageItem {

    private String title;
    private List<String> items = new ArrayList<>();

    public List<String> getItems() {
        return items;
    }

    public String getTitle() {
        return title;
    }

    public ListPageItem setItems(List<String> items) {
        this.items = items;
        return this;
    }

    public ListPageItem setTitle(String title) {
        this.title = title;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this) //
                .add("title", title) //
                .add("items", items) //
                .toString();
    }

}
