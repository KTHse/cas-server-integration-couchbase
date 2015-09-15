package se.kth.infosys.login.couchbase;

/*
 * Copyright (C) 2013 KTH, Kungliga tekniska hogskolan, http://www.kth.se
 *
 * This file is part of cas-server-integration-couchbase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jasig.cas.services.AbstractRegisteredService;
import org.jasig.cas.services.RegexRegisteredService;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.RegisteredServiceImpl;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * Tests for the serialization and de-serialization of ServiceRegistry classes.
 * 
 * @author Fredrik Jönsson "fjo@kth.se"
 * @since 4.0
 */
public class ServiceJsonSerializerTests {
    private static Gson GSON;

    @BeforeClass
    public static void beforeClass() {
        final GsonBuilder gsonBilder = new GsonBuilder();
        gsonBilder.registerTypeAdapter(
                AbstractRegisteredService.class, 
                new AbstractRegisteredServiceJsonSerializer());
        GSON = gsonBilder.create();
    }


    /*
     * Verify that serialization/deserialization of RegisteredServiceImpl is reflexive.
     */
    @Test
    public void registeredServiceImpl() {
        final RegisteredServiceImpl service = new RegisteredServiceImpl();
        setProperties(service);

        final String json = GSON.toJson(service, AbstractRegisteredService.class);
        final RegisteredService deserializedService = GSON.fromJson(json, AbstractRegisteredService.class);

        assertTrue(deserializedService instanceof RegisteredServiceImpl);
        assertPropertiesEqual(service, (RegisteredServiceImpl) deserializedService);
    }


    /*
     * Verify that serialization/deserialization of RegexRegisteredService is reflexive.
     */
    @Test
    public void regexRegisteredService() {
        final RegexRegisteredService service = new RegexRegisteredService();
        setProperties(service);

        final String json = GSON.toJson(service, AbstractRegisteredService.class);
        final RegisteredService deserializedService = GSON.fromJson(json, AbstractRegisteredService.class);

        assertTrue(deserializedService instanceof RegexRegisteredService);
        assertPropertiesEqual(service, (RegexRegisteredService) deserializedService);
    }


    public static void setProperties(final AbstractRegisteredService service) {
        service.setAllowedToProxy(false);
        service.setDescription("description");
        service.setEnabled(true);
        service.setEvaluationOrder(2);
        service.setName("service");
        service.setServiceId("http://foo.bar");
        service.setSsoEnabled(true);
        service.setTheme("theme");
        service.setUsernameAttribute("kthid");
    }


    public static void assertPropertiesEqual(final RegisteredService s1, final RegisteredService s2) {
        assertEquals(s1.isAllowedToProxy(), s2.isAllowedToProxy());
        assertEquals(s1.getDescription(), s2.getDescription());
        assertEquals(s1.getEvaluationOrder(), s2.getEvaluationOrder());
        assertEquals(s1.getId(), s2.getId());
        assertEquals(s1.getName(), s2.getName());
        assertEquals(s1.getServiceId(), s2.getServiceId());
        assertEquals(s1.isSsoEnabled(), s2.isSsoEnabled());
        assertEquals(s1.getTheme(), s2.getTheme());
        assertEquals(s1.getUsernameAttribute(), s2.getUsernameAttribute());
    }
}
