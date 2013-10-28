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
package fi.vm.sade.lokalisointi.service.resource;

import fi.vm.sade.lokalisointi.api.model.LocalisationRDTO;
import fi.vm.sade.lokalisointi.service.dao.LocalisationDao;
import fi.vm.sade.lokalisointi.service.model.Localisation;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author mlyly
 */
@ContextConfiguration(locations = {"classpath:test-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class LocalisationResourceImplTest {

    private static final Logger LOG = LoggerFactory.getLogger(LocalisationResourceImplTest.class);

    @Autowired(required = true)
    private LocalisationDao dao;

    public LocalisationResourceImplTest() {
        LOG.info("LocalisationResourceImplTest()");
    }

    @BeforeClass
    public static void setUpClass() {
        LOG.info("setUpClass()");
    }

    @AfterClass
    public static void tearDownClass() {
        LOG.info("tearDownClass()");
    }

    @Before
    public void setUp() {
        LOG.info("setUp()");
    }

    @After
    public void tearDown() {
        LOG.info("tearDown()");
    }

    @Test
    public void testGetLocalisations() {
        LOG.info("testGetLocalisations()");
        assertTrue("Should be empty...", dao.findAll().size() == 0);

        insertOne("foo", "fi");
        insertOne("foo", "en");
        insertOne("foo", "sv");

        assertTrue("Should be empty...", dao.findAll().size() == 3);
    }

    @Test
    public void testUpdateLocalisation() {
        LOG.info("testUpdateLocalisation()");

        Localisation l = insertOne("foo", "fi");

        Localisation l1 = dao.read(l.getId());
        assertTrue("Should find it...", l1 != null);

        String newValue = "NEW_" + System.currentTimeMillis();
        l1.setValue(newValue);
        dao.save(l1);

        Localisation l2 = dao.read(l.getId());
        assertTrue("Should find it...", l1 != null);

        assertEquals(newValue, l2.getValue());
    }

    @Test
    public void testUpdateLocalisationAccessed() {
        LOG.info("testUpdateLocalisationAccessed()");

        Localisation l = insertOne("foo", "fi");

        Date d = new Date();
        long ts = d.getTime();

        l.setAccessed(d);
        dao.save(l);

        Localisation l1 = dao.read(l.getId());
        assertTrue("Should find it...", l1 != null);

        assertEquals("Accessed not updated.",  l1.getAccessed().getTime(), ts);
    }

    @Test
    public void testDeleteLocalisation() {
        LOG.info("testDeleteLocalisation()");

        Localisation l = insertOne("foo", "sv");
        assertTrue("Should find...", dao.findAll().size() == 1);

        dao.delete(l);
        assertTrue("Should not find any!", dao.findAll().size() == 0);
    }




    private Localisation insertOne(String key, String locale) {
        Localisation l = new Localisation();
        l.setCategory("TEST");
        l.setKey(key);
        l.setLanguage(locale);
        l.setValue("TEST VALUE");

        return dao.save(l);
    }

}
