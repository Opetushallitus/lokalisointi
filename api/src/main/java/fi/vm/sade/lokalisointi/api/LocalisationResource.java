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
package fi.vm.sade.lokalisointi.api;

import fi.vm.sade.lokalisointi.api.model.LocalisationRDTO;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Implement necessary services to manage localisation and internationalization support for other applications.
 *
 * @author mlyly
 * @see CORS fi.vm.sade.lokalisointi.service.resource.AllowAllCorsRequestsFilter in web.xml
 */
@Path("/v1/localisation")
public interface LocalisationResource {

    /**
     * This method is for development purposes so that developer can authorize via CAS if needed.
     *
     * @return
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("authorize")
    public String authorize();

    /**
     * Loads ALL localisations with given prefix (if any).
     *
     * @param query "Localisation object" that can be used in query, ie. "category", "locale", "key", etc.
     * @return list of matched localisations
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public List<LocalisationRDTO> getLocalisations(@QueryParam("") LocalisationRDTO query);

    /**
     * Update exisiting localisation. If a localisation is found via ID the it is updated - otherwise
     * a localisation is searched with category, key and locale and is´ only one is found then its value + description is updated.
     *
     * @param id NOT USED
     * @param data if data.id < 0 find by (key, locale, cat) - if data.id >= 0 find by id.
     * @return
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Path("{id}")
    public LocalisationRDTO updateLocalisation(@PathParam("id") Long id, LocalisationRDTO data);

    /**
     * Update last accessed timestamp.
     *
     * @param id NOT USED
     * @param data if data.id < 0 find by (key, locale, cat) - if data.id >= 0 find by id.
     * @return
     */
    @PUT
    @Path("{id}/access")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public LocalisationRDTO updateLocalisationAccessed(@PathParam("id") Long id, LocalisationRDTO data);

    /**
     * Creates new localisation.
     *
     * @param data
     * @return
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public LocalisationRDTO createLocalisation(LocalisationRDTO data);

    /**
     * Deletes a localisation.
     *
     * @param id
     * @return
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Path("{id}")
    public LocalisationRDTO deleteLocalisation(@PathParam("id") Long id);
}
