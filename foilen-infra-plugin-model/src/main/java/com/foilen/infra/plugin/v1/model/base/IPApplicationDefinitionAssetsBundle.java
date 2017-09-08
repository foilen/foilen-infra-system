/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.model.base;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.infra.plugin.v1.model.ModelsException;
import com.foilen.smalltools.tools.ResourceTools;
import com.foilen.smalltools.tuple.Tuple2;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IPApplicationDefinitionAssetsBundle {

    private String assetsFolderPath;

    private List<Tuple2<String, String>> assetsRelativePathAndContent = new ArrayList<>();
    private List<Tuple2<String, ByteArrayOutputStream>> futureAssetsRelativePathAndContent = new ArrayList<>();

    public IPApplicationDefinitionAssetsBundle() {
    }

    public IPApplicationDefinitionAssetsBundle(String assetsFolderPath) {
        this.assetsFolderPath = assetsFolderPath;
    }

    public IPApplicationDefinitionAssetsBundle addAssetContent(String content, String assetRelativePath) {
        assetsRelativePathAndContent.add(new Tuple2<>(assetRelativePath, content));
        return this;
    }

    public IPApplicationDefinitionAssetsBundle addAssetResource(String sourceResource, String assetRelativePath) {
        String content = ResourceTools.getResourceAsString(sourceResource);
        return addAssetContent(content, assetRelativePath);
    }

    public OutputStream addAssetStream(String assetRelativePath) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        futureAssetsRelativePathAndContent.add(new Tuple2<>(assetRelativePath, byteArrayOutputStream));
        return byteArrayOutputStream;
    }

    public String getAssetsFolderPath() {
        return assetsFolderPath;
    }

    public List<Tuple2<String, String>> getAssetsRelativePathAndContent() {
        // Compute the future ones
        Iterator<Tuple2<String, ByteArrayOutputStream>> it = futureAssetsRelativePathAndContent.iterator();
        while (it.hasNext()) {
            Tuple2<String, ByteArrayOutputStream> futureAssetRelativePathAndContent = it.next();
            try {
                assetsRelativePathAndContent.add(new Tuple2<>(futureAssetRelativePathAndContent.getA(), futureAssetRelativePathAndContent.getB().toString("UTF-8")));
            } catch (UnsupportedEncodingException e) {
                throw new ModelsException(e);
            }
            it.remove();
        }
        return assetsRelativePathAndContent;
    }

    public void setAssetsFolderPath(String assetsFolderPath) {
        this.assetsFolderPath = assetsFolderPath;
    }

    public void setAssetsRelativePathAndContent(List<Tuple2<String, String>> assetsRelativePathAndContent) {
        futureAssetsRelativePathAndContent.clear();
        this.assetsRelativePathAndContent = assetsRelativePathAndContent;
    }

}
