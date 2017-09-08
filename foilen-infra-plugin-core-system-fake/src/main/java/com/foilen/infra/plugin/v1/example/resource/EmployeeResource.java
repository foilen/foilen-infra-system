/*
    Foilen Infra Plugin
    https://github.com/foilen/foilen-infra-plugin
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.plugin.v1.example.resource;

import java.util.Date;

import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;
import com.foilen.smalltools.tools.DateTools;

public class EmployeeResource extends AbstractIPResource {

    public static final String PROPERTY_FIRST_NAME = "firstName";
    public static final String PROPERTY_LAST_NAME = "lastName";
    public static final String PROPERTY_BIRTHDAY = "birthday";
    public static final String PROPERTY_NOTES = "notes";

    public static final String LINK_TYPE_MANAGER = "MANAGER";

    private String firstName;
    private String lastName;
    private Date birthday;
    private String notes;

    public EmployeeResource() {
    }

    public EmployeeResource(String firstName, String lastName, Date birthday) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthday = birthday;
    }

    public Date getBirthday() {
        return birthday;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNotes() {
        return notes;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.INFRASTRUCTURE;
    }

    @Override
    public String getResourceDescription() {
        return DateTools.formatDateOnly(birthday);
    }

    @Override
    public String getResourceName() {
        return lastName + ", " + firstName;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

}
