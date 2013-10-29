/*
 * Copyright (c) 2013 The Finnish Board of Education - Opetushallitus
 *
 * This program is free software:  Licensed under the EUPL, Version 1.1 or - as
 * soon as they will be approved by the European Commission - subsequent versions
 * of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at: http://www.osor.eu/eupl/
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 */
package fi.vm.sade.lokalisointi.api.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Single translation.
 *
 * <pre>
 * {
 *   created:...,
 *   createdBy:...,
 *   modified:...,
 *   modifiedBy:...,
 *   accessed: 21.1.2011 10:42
 *   category: "Tarjonta"
 *   key: "review.title"
 *   locale: "kieli_fi"
 *   value: "Tarkastelunäkymä"
 *   description: "Tarkastelunäkymän otsikko; onkohan tämä vielä käytössä? (Jamppa/21.1.2014)"
 * }
 * </pre>
 *
 *
 * @author mlyly
 */
public class LocalisationRDTO implements Serializable {

    public static final String DEFAULT_CATEGORY = "NONE";

    private Date _created = new Date();
    private String _createdBy;
    private Date _modified = new Date();
    private String _modifiedBy;
    private Date _accessed = new Date();

    private String _category = DEFAULT_CATEGORY;
    private String _locale;
    private String _key;
    private String _value;
    private String _description;

    @Override
    public String toString() {
        return "LocalisationRDTO[category=" + getCategory() + ", key=" + getKey() + ", locale=" + getLocale() + "]";
    }

    public Date getCreated() {
        return _created;
    }

    public void setCreated(Date created) {
        this._created = created;
    }

    public String getCreatedBy() {
        return _createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this._createdBy = createdBy;
    }

    public Date getModified() {
        return _modified;
    }

    public void setModified(Date modified) {
        this._modified = modified;
    }

    public String getModifiedBy() {
        return _modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this._modifiedBy = modifiedBy;
    }

    public String getCategory() {
        return _category;
    }

    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            _category = DEFAULT_CATEGORY;
        } else {
            this._category = category;
        }
    }

    public String getLocale() {
        return _locale;
    }

    public void setLocale(String locale) {
        this._locale = locale;
    }

    public String getKey() {
        return _key;
    }

    public void setKey(String key) {
        this._key = key;
    }

    public String getValue() {
        return _value;
    }

    public void setValue(String value) {
        this._value = value;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        this._description = description;
    }

    public Date getAccessed() {
        return _accessed;
    }

    public void setAccessed(Date _accessed) {
        this._accessed = _accessed;
    }

}
