/**
 * A Service Registry storage backend which uses the memcached protocol. This may 
 * seem like a weird idea until you realize that CouchBase is a multi host 
 * NoSQL database with a memcached interface to persistent storage which also
 * is quite usable as a replicated tickage storage engine for multiple front end
 * CAS servers.
 */
package se.kth.infosys.login.couchbase;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.jasig.cas.services.AbstractRegisteredService;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ServiceRegistryDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewDesign;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class CouchbaseServiceRegistryDaoImpl extends TimerTask implements ServiceRegistryDao {
	private static final Logger log = LoggerFactory.getLogger(CouchbaseServiceRegistryDaoImpl.class);

	private static final Timer timer = new Timer();

	private static final long RETRY_INTERVAL = 10;

	private final Gson gson;

	/* Couchbase client factory */
	@NotNull
	private CouchbaseClientFactory couchbase;

	/* List of statically configured services, to be used at bean instantiation. */
	private final List<RegisteredService> registeredServices = new LinkedList<RegisteredService>();

	/* Initial service id for added services. */
	private int initialId = 0;

	/**
	 * Default constructor.
	 */
	public CouchbaseServiceRegistryDaoImpl() {
		GsonBuilder gsonBilder = new GsonBuilder();
		gsonBilder.registerTypeAdapter(AbstractRegisteredService.class, new AbstractRegisteredServiceJsonSerializer());
		gson = gsonBilder.create();
	}


	/** 
	 * {@inheritDoc}
	 */
	public RegisteredService save(RegisteredService registeredService) {
		log.debug("Saving service {}", registeredService);

		if (registeredService.getId() == -1) {
			long id = couchbase.getClient().incr("LAST_ID", 1, initialId);
			((AbstractRegisteredService) registeredService).setId(id);
		}

		couchbase.getClient().set(
				String.valueOf(registeredService.getId()), 
				0, 
				gson.toJson(registeredService, AbstractRegisteredService.class));
		return registeredService;
	}


	/** 
	 * {@inheritDoc}
	 */
	public boolean delete(final RegisteredService registeredService) {
		log.debug("Deleting service {}", registeredService);
		couchbase.getClient().delete(String.valueOf(registeredService.getId()));
		return true;
	}


	/** 
	 * {@inheritDoc}
	 */
	public List<RegisteredService> load() {
		try {
			log.debug("Loading services");

			View allKeys = couchbase.getClient().getView(UTIL_DOCUMENT, ALL_SERVICES_VIEW.getName());
			Query query = new Query();
			query.setIncludeDocs(true);
			ViewResponse response = couchbase.getClient().query(allKeys, query);
			Iterator<ViewRow> iterator = response.iterator();

			List<RegisteredService> services = new LinkedList<RegisteredService>();
			while (iterator.hasNext()) {
				String json = (String) iterator.next().getDocument();
				log.debug("Found service: " + json);
				services.add((RegisteredService) gson.fromJson(json, AbstractRegisteredService.class));
			}
			return services;
		} catch (RuntimeException e) {
			log.warn("Unable to load services.", e.getMessage());
			return new LinkedList<RegisteredService>();
		}
	}


	/** 
	 * {@inheritDoc}
	 */
	public RegisteredService findServiceById(final long id) {
		try {
			log.debug("Lookup for service {}", id);
			return gson.fromJson(
					(String) couchbase.getClient().get(String.valueOf(id)),
					AbstractRegisteredService.class);
		}
		catch (Exception e) {
			log.error("Unable to get registered service", e);
			return null;
		}
	}


	/**
	 * Used to initialize static services from configuration.
	 * 
	 * @param services List of RegisteredService objects to register.
	 */
	public void setRegisteredServices(final List<RegisteredService> services) {
		this.registeredServices.addAll(services);
		this.initialId = services.size();
		timer.scheduleAtFixedRate(this, new Date(), TimeUnit.SECONDS.toMillis(RETRY_INTERVAL));
	}


	/**
	 * Starts the couchbase client and initialization task.
	 */
	public void initialize() {
		couchbase.ensureIndexes(UTIL_DOCUMENT, ALL_VIEWS);
		couchbase.initialize();
	}


	/**
	 * Stops the couchbase client and cancels the initialization task if uncompleted.
	 */
	public void destroy() throws Exception {
		timer.cancel();
		timer.purge();
		couchbase.shutdown();
	}


	/*
	 * Views, or indexes, in the database.
	 */
	private static final ViewDesign ALL_SERVICES_VIEW = new ViewDesign(
			"all_services",
			"function(d,m) {if (!isNaN(m.id)) {emit(m.id);}}");
	private static final List<ViewDesign> ALL_VIEWS = Arrays.asList(new ViewDesign[] {
			ALL_SERVICES_VIEW
	});
	private final static String UTIL_DOCUMENT = "utils";


	@Override
	public void run() {
		try {
			for (RegisteredService service : registeredServices) {
				save(service);
			}
			timer.cancel();
			log.debug("Stored pre configured services from XML in registry.");
		} catch (RuntimeException e) {
			log.error("Unable to save pre configured services, retrying...", e);
		}
	}


	/**
	 * @param Couchbase client factory to use.
	 */
	public void setCouchbase(final CouchbaseClientFactory couchbase) {
		this.couchbase = couchbase;
	}
}
