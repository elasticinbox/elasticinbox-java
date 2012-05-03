package com.elasticinbox.itests;

import static com.jayway.restassured.RestAssured.expect;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.repository;
import static org.ops4j.pax.exam.CoreOptions.scanDir;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIntegrationTest
{
	static final String TEST_ACCOUNT = "test@elasticinbox.com";
	static final String REST_PATH = "/rest/v2/elasticinbox.com/test";
	static final String EMAIL_LARGE_ATT = "/01-attach-utf8.eml";
	static final String EMAIL_REGULAR = "/01-headers-utf8.eml";

	static Boolean initialized = false;

	static final Logger logger = 
			LoggerFactory.getLogger(AbstractIntegrationTest.class);

	@Inject
	private BundleContext bc;

	@Configuration()
	public Option[] config()
	{
		return options(
				//junitBundles(),
				felix().version("3.2.2"),
				workingDirectory("target/paxrunner/"),
				repository("https://repository.apache.org/snapshots/").allowSnapshots(),
	
				// Configs
				systemProperty("elasticinbox.config").value("../test-classes/elasticinbox.yaml"),
				systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
	
				// PAX Exam Bundles
				mavenBundle().groupId("org.mortbay.jetty").artifactId("servlet-api").versionAsInProject(),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-api").versionAsInProject(),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-spi").versionAsInProject(),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-jetty-bundle").versionAsInProject(),
				mavenBundle().groupId("org.ops4j.pax.web").artifactId("pax-web-extender-war").versionAsInProject(),

				// Logging
				mavenBundle().groupId("ch.qos.logback").artifactId("logback-core").versionAsInProject(),
				mavenBundle().groupId("ch.qos.logback").artifactId("logback-classic").versionAsInProject(),
	
				// REST-Assured Bundles
				wrappedBundle(mavenBundle().groupId("com.jayway.restassured").artifactId("rest-assured").versionAsInProject()).
					imports("org.apache.http.impl.conn,org.apache.http.impl.client,org.apache.commons.lang3,org.apache.commons.lang3.math,org.codehaus.jackson.map,groovy.lang,org.hamcrest,*"),
				wrappedBundle(mavenBundle().groupId("org.hamcrest").artifactId("hamcrest-all").versionAsInProject()),
				mavenBundle().groupId("org.codehaus.groovy").artifactId("groovy-all").version("1.8.6"),
				mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").version("3.1"),
				mavenBundle().groupId("commons-collections").artifactId("commons-collections").version("3.2.1"),
				mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpcore-osgi").version("4.1.4"),
				mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpclient-osgi").version("4.1.3"),
				wrappedBundle(mavenBundle().groupId("org.apache.httpcomponents").artifactId("httpmime").version("4.1.3")),
				wrappedBundle(mavenBundle("oauth.signpost", "signpost-commonshttp4", "1.2.1.2")),
				wrappedBundle(mavenBundle("oauth.signpost", "signpost-core", "1.2.1.2")),
				wrappedBundle(mavenBundle("org.ccil.cowan.tagsoup", "tagsoup", "1.2.1")),
	
				// jClouds and dependencies
				mavenBundle().groupId("com.google.inject").artifactId("guice").versionAsInProject(),
				mavenBundle().groupId("org.jclouds").artifactId("jclouds-core").versionAsInProject(),
				mavenBundle().groupId("org.jclouds").artifactId("jclouds-blobstore").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.aopalliance").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-io").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.commons-lang").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.javax-inject").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.oauth-commons").versionAsInProject(),
				mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.java-xmlbuilder").versionAsInProject(),
				mavenBundle().groupId("com.google.inject.extensions").artifactId("guice-assistedinject").versionAsInProject(),
				mavenBundle().groupId("com.google.code.gson").artifactId("gson").versionAsInProject(),
				mavenBundle("org.99soft.guice", "rocoto", "6.1"),

				// ElasticInbox Bundles
				mavenBundle().groupId("com.googlecode.guava-osgi").artifactId("guava-osgi").versionAsInProject(),
				mavenBundle().groupId("org.codehaus.jackson").artifactId("jackson-core-asl").versionAsInProject(),
				mavenBundle().groupId("org.codehaus.jackson").artifactId("jackson-mapper-asl").versionAsInProject(),
				mavenBundle().groupId("org.codehaus.jackson").artifactId("jackson-jaxrs").versionAsInProject(),
				mavenBundle().groupId("com.ning").artifactId("compress-lzf").versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-core").versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-server").versionAsInProject(),
				mavenBundle().groupId("com.sun.jersey").artifactId("jersey-servlet").versionAsInProject(),
				mavenBundle().groupId("javax.mail").artifactId("mail").versionAsInProject(),
				scanDir("../bundles/com.ecyrd.speed4j/target/"),
				scanDir("../modules/common/target/"),
				scanDir("../modules/config/target/"),
				scanDir("../modules/core/target/"),
				scanDir("../modules/rest/target/")
				
				// remote debugging
				//, vmOption( "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" )
		);
	}

	public BundleContext getBundleContext() {
		return bc;
	}

	/**
	 * This is hack which initializes account. Ideally we should put this into @BeforeClass
	 * and @AfterClass. However PaxExam does not support it at this moment (see PAXEXAM-288).
	 * 
	 * This method should be called at the beginning of each test manually.
	 */
	public static synchronized void initAccount()
	{
		if (!initialized) {
			// delete account
			expect().statusCode(204).when().delete(REST_PATH);

			// create account
			expect().statusCode(201).when().post(REST_PATH);

			initialized = true;
		}
	}

	//@Test
	public void bundleContextTest()
	{
		logger.info("BundleContext of bundle injected: {}", getBundleContext().getBundle().getSymbolicName());

		for (Bundle b : getBundleContext().getBundles()) {
			logger.info("Bundle {} [state={}]", b.getSymbolicName(), b.getState());
		}
	}

	/**
	 * Returns resource size
	 *  
	 * @param name
	 * @return
	 * @throws IOException 
	 */
	public long getResourceSize(String messageFile) throws IOException
	{
		InputStream in = null;

		try {
			in = this.getClass().getResourceAsStream(messageFile);
			return in.available();
		} finally {
			in.close();
		}
	}
}
