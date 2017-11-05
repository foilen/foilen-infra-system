/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.base;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.ResourceTools;
import com.foilen.smalltools.tuple.Tuple2;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IPApplicationDefinitionAssetsBundle extends AbstractBasics {

    private String assetsFolderPath;

    private List<Tuple2<String, String>> assetsRelativePathAndContent = new ArrayList<>();

    public IPApplicationDefinitionAssetsBundle() {
    }

    public IPApplicationDefinitionAssetsBundle(String assetsFolderPath) {
        this.assetsFolderPath = assetsFolderPath;
    }

    public IPApplicationDefinitionAssetsBundle addAssetContent(String assetRelativePath, String content) {
        assetsRelativePathAndContent.add(new Tuple2<>(assetRelativePath, content));
        return this;
    }

    public IPApplicationDefinitionAssetsBundle addAssetResource(String assetRelativePath, String sourceResource) {
        String content = ResourceTools.getResourceAsString(sourceResource);
        return addAssetContent(assetRelativePath, content);
    }

    public String getAssetsFolderPath() {
        return assetsFolderPath;
    }

    public List<Tuple2<String, String>> getAssetsRelativePathAndContent() {
        return assetsRelativePathAndContent;
    }

    public void setAssetsFolderPath(String assetsFolderPath) {
        this.assetsFolderPath = assetsFolderPath;
    }

    public void setAssetsRelativePathAndContent(List<Tuple2<String, String>> assetsRelativePathAndContent) {
        this.assetsRelativePathAndContent = assetsRelativePathAndContent;
    }

}
