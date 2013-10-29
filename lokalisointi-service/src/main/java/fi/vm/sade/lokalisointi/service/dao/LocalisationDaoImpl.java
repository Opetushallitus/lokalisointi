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
package fi.vm.sade.lokalisointi.service.dao;

import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.expr.BooleanExpression;
import fi.vm.sade.generic.dao.AbstractJpaDAOImpl;
import fi.vm.sade.lokalisointi.api.model.LocalisationRDTO;
import fi.vm.sade.lokalisointi.service.model.Localisation;
import fi.vm.sade.lokalisointi.service.model.QLocalisation;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 *
 * @author mlyly
 */
@Repository
public class LocalisationDaoImpl extends AbstractJpaDAOImpl<Localisation, Long> implements LocalisationDao {

    private static final Logger LOG = LoggerFactory.getLogger(LocalisationDaoImpl.class);

    @Override
    public List<Localisation> findBy(String category, String locale, String keyPrefix) {
        LOG.info("findBy({}, {}, {})", new Object[] {category, locale, keyPrefix});

        QLocalisation qLocalisation = QLocalisation.localisation;

        JPAQuery q = from(qLocalisation);

        // What is good 1=1 expression in Mysema?
        BooleanExpression whereExpr = qLocalisation.id.ne(-1L);

        whereExpr = (category != null && !category.equals(LocalisationRDTO.DEFAULT_CATEGORY)) ? whereExpr.and(qLocalisation.category.eq(category)) : whereExpr;
        whereExpr = (locale != null) ? whereExpr.and(qLocalisation.language.eq(locale)) : whereExpr;
        whereExpr = (keyPrefix != null) ? whereExpr.and(qLocalisation.key.eq(keyPrefix)) : whereExpr;

        List<Localisation> ll = q.where(whereExpr).list(qLocalisation);

        LOG.info(" --> result = {}", ll);

        return ll;
    }

    @Override
    public Localisation findOne(String category, String key, String locale) {
        LOG.info("findOne()");

        List<Localisation> ll = findBy(category, locale, key);
        if (ll.size() == 1) {
            return ll.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Localisation save(Localisation localisation) {
        LOG.info("save({})", localisation);

        if (localisation.getId() != null) {
            super.update(localisation);
        } else {
            localisation = super.insert(localisation);
        }

        return localisation;
    }

    @Override
    public boolean delete(Localisation localisation) {
        LOG.info("delete({})", localisation);

        super.remove(localisation);

        return true;
    }

    protected JPAQuery from(EntityPath<?>... o) {
        return new JPAQuery(getEntityManager()).from(o);
    }

}
