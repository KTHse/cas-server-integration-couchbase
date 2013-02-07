package se.kth.infosys.login.couchbase;

import static org.junit.Assert.*;

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
 */
public class ServiceJsonSerializerTests {
	private static Gson gson;
	
	@BeforeClass
	public static void beforeClass() {
		GsonBuilder gsonBilder = new GsonBuilder();
		gsonBilder.registerTypeAdapter(
				AbstractRegisteredService.class, 
				new AbstractRegisteredServiceJsonSerializer());
		gson = gsonBilder.create();
	}

	
	/*
	 * Verify that serialization/deserialization of RegisteredServiceImpl is reflexive.
	 */
	@Test
	public void testRegisteredServiceImpl() {
		RegisteredServiceImpl service = new RegisteredServiceImpl();
		setProperties(service);
		
		String json = gson.toJson(service, AbstractRegisteredService.class);
		RegisteredService deserializedService = gson.fromJson(json, AbstractRegisteredService.class);
		
		assertTrue(deserializedService instanceof RegisteredServiceImpl);
		assertPropertiesEqual(service, (RegisteredServiceImpl) deserializedService);
	}

	
	/*
	 * Verify that serialization/deserialization of RegexRegisteredService is reflexive.
	 */
	@Test
	public void testRegexRegisteredService() {
		RegexRegisteredService service = new RegexRegisteredService();
		setProperties(service);
		
		String json = gson.toJson(service, AbstractRegisteredService.class);
		RegisteredService deserializedService = gson.fromJson(json, AbstractRegisteredService.class);
		
		assertTrue(deserializedService instanceof RegexRegisteredService);
		assertPropertiesEqual(service, (RegexRegisteredService) deserializedService);
	}

	
	public static void setProperties(AbstractRegisteredService service) {
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

	
	public static void assertPropertiesEqual(RegisteredService s1, RegisteredService s2) {
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
