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
import com.mysema.query.jpa.impl.JPAUpdateClause;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.expr.BooleanExpression;
import fi.vm.sade.generic.dao.AbstractJpaDAOImpl;
import fi.vm.sade.lokalisointi.api.model.LocalisationRDTO;
import fi.vm.sade.lokalisointi.service.model.Localisation;
import fi.vm.sade.lokalisointi.service.model.QLocalisation;
import java.util.List;
import javax.persistence.Query;
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
    public List<Localisation> findBy(Long id, String category, String key, String locale) {
        LOG.debug("findBy({}, {}, {}, {})", new Object[]{id, category, key, locale});

        QLocalisation qLocalisation = QLocalisation.localisation;

        JPAQuery q = from(qLocalisation);

        // What is good 1=1 expression in Mysema?
        BooleanExpression whereExpr = qLocalisation.id.ne(Long.MAX_VALUE);

        if (id != null && id >= 0) {
            // Search by ID
            whereExpr = qLocalisation.id.eq(id);
        } else {
            // Search by key, cat, lang
            whereExpr = (category != null && !category.equals(LocalisationRDTO.DEFAULT_CATEGORY)) ? whereExpr.and(qLocalisation.category.eq(category)) : whereExpr;
            whereExpr = (key != null) ? whereExpr.and(qLocalisation.key.eq(key)) : whereExpr;
            whereExpr = (locale != null) ? whereExpr.and(qLocalisation.language.eq(locale)) : whereExpr;
        }

        List<Localisation> ll = q.where(whereExpr).list(qLocalisation);

        LOG.debug(" --> result.size = {}", ll == null ? 0 : ll.size());

        return ll;
    }

    @Override
    public Localisation findOne(Long id, String category, String key, String locale) {
        LOG.debug("findOne({}, {}, {}, {})", new Object[]{id, category, key, locale});

        List<Localisation> ll = findBy(id, category, key, locale);

        if (ll.size() == 1) {
            return ll.get(0);
        } else {
            if (ll.size() > 1) {
                LOG.warn("findOne() found MORE THAT ONE! {}, {}, {}, {}", new Object[]{id, category, key, locale});
            }
            return null;
        }
    }

    @Override
    public Localisation save(Localisation localisation) {
        LOG.debug("save({})", localisation);

        if (localisation.getId() != null) {
            super.update(localisation);
        } else {
            localisation = super.insert(localisation);
        }

        return localisation;
    }

    @Override
    public boolean delete(Localisation localisation) {
        LOG.debug("delete({})", localisation);

        super.remove(localisation);

        return true;
    }

    @Override
    public long updateAccessed(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        String sql = "UPDATE localisation SET accesscount = accesscount + 1, accessed = NOW() WHERE id IN (:ids)";

        Query q = getEntityManager().createNativeQuery(sql);
        q.setParameter("ids", ids);

        long result = q.executeUpdate();

        LOG.debug("updateAccessed() - oids.size={} --> updated {} translations", (ids != null) ? ids.size() : 0, result);

        return result;
    }

    protected JPAQuery from(EntityPath<?>... o) {
        return new JPAQuery(getEntityManager()).from(o);
    }

    protected JPAUpdateClause update(EntityPath<?> o) {
        return new JPAUpdateClause(getEntityManager(), o);
    }

}
