/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017-2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.withparent;

public class ConcreteLevel1 extends AbstractParent {

    public static final String PROPERTY_ON_LEVEL_1 = "onLevel1";

    private String onLevel1;

    public ConcreteLevel1() {
    }

    public ConcreteLevel1(String name, String onParent, String onLevel1) {
        super(name, onParent);
        this.onLevel1 = onLevel1;
    }

    public String getOnLevel1() {
        return onLevel1;
    }

    public void setOnLevel1(String onLevel1) {
        this.onLevel1 = onLevel1;
    }

}
