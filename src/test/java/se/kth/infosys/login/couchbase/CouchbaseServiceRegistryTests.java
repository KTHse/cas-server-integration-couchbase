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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.jasig.cas.services.RegexRegisteredService;
import org.jasig.cas.services.RegisteredService;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.couchbase.client.CouchbaseClient;

/**
 * Tests for the saving and finding ServiceRegistry classes.
 * 
 * @author Fredrik Jönsson "fjo@kth.se"
 * @since 4.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CouchbaseServiceRegistryTests {
    /* The subject for testing */
    private static CouchbaseServiceRegistryDaoImpl SERVICE_REGISTRY = new CouchbaseServiceRegistryDaoImpl();

    /* Mock-ups for the database */
    private static CouchbaseClient CLIENT = mock(CouchbaseClient.class);
    private static CouchbaseClientFactory COUCHBASE = mock(CouchbaseClientFactory.class);

    /* Hash map to store JSON services in for mocking */
    private static HashMap<String, String> SERVICES = new HashMap<String, String>();


    /*
     * Set up mock up behavior for the database.
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        SERVICE_REGISTRY.setCouchbase(COUCHBASE);

        when(COUCHBASE.getClient()).thenReturn(CLIENT);

        when(CLIENT.incr(eq("LAST_ID"), anyInt(), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                return (long) SERVICES.size();
            }});

        when(CLIENT.get(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                return SERVICES.get((String) args[0]);
            }});

        when(CLIENT.set(anyString(), eq(0), anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                SERVICES.put((String) args[0], (String) args[2]);
                return null;
            }});
    }


    /*
     * Store a RegexRegisteredService and see if we get the same back.
     */
    @Test
    public void saveRegexService() {
        final RegexRegisteredService registeredService = new RegexRegisteredService();

        ServiceJsonSerializerTests.setProperties(registeredService);
        assertEquals(RegisteredService.INITIAL_IDENTIFIER_VALUE, registeredService.getId());

        final RegisteredService newService = SERVICE_REGISTRY.save(registeredService);
        assertEquals(0, newService.getId());

        final RegisteredService service = SERVICE_REGISTRY.findServiceById(newService.getId());
        assertNotNull(service);
        assertTrue(service instanceof RegexRegisteredService);
        ServiceJsonSerializerTests.assertPropertiesEqual(newService, service);
    }


    /*
     * Store a RegisteredServiceImpl and see if we get the same back.
     */
    @Test
    public void saveServiceImpl() {
        final RegisteredServiceImpl registeredService = new RegisteredServiceImpl();

        ServiceJsonSerializerTests.setProperties(registeredService);
        assertEquals(RegisteredService.INITIAL_IDENTIFIER_VALUE, registeredService.getId());

        final RegisteredService newService = SERVICE_REGISTRY.save(registeredService);
        assertEquals(1, newService.getId());

        final RegisteredService service = SERVICE_REGISTRY.findServiceById(newService.getId());
        assertNotNull(service);
        assertTrue(service instanceof RegisteredServiceImpl);
        ServiceJsonSerializerTests.assertPropertiesEqual(newService, service);
    }
}
