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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author mlyly
 */
@Path("/localisation")
public interface LocalisationResource {

    /**
     * Loads ALL localisations with given prefix (if any).
     *
     * @param category Category, ex. "Tarjonta"
     * @param locale select by locale, ex. "kieli_fi"
     * @param keyPrefix Key prefix, "error."
     * @return list of matched localisations
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public List<LocalisationRDTO> getLocalisations(@QueryParam("category") String category, @QueryParam("locale") String locale, @QueryParam("keyPrefix") String keyPrefix);

    /**
     * Update exisiting loclaisation.
     *
     * @param data
     * @return
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public LocalisationRDTO updateLocalisation(LocalisationRDTO data);

    /**
     * Update last accessed timestamp.
     *
     * @param data
     * @return
     */
    @PUT
    @Path("access")
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public LocalisationRDTO updateLocalisationAccessed(LocalisationRDTO data);

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
     * @param data
     * @return
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    @Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
    public LocalisationRDTO deleteLocalisation(LocalisationRDTO data);
}
