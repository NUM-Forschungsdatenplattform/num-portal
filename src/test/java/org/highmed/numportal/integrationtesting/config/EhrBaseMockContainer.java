package org.highmed.numportal.integrationtesting.config;

import org.mockserver.client.MockServerClient;
import org.mockserver.model.*;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.utility.DockerImageName;

import static org.highmed.numportal.integrationtesting.tests.IntegrationTest.IDENTITY_PROVIDER_TOKEN_ENDPOINT;

public class EhrBaseMockContainer extends MockServerContainer {
    private static final String IMAGE_VERSION = "mockserver/mockserver:5.15.0";
    private static final String EHR_BASE_URL = "/ehrbase/rest/openehr/v1/definition/template/adl1.4/";
    private static EhrBaseMockContainer container;

    private EhrBaseMockContainer() {
        super(DockerImageName.parse(IMAGE_VERSION));
    }

    public static EhrBaseMockContainer getInstance() {
        if (container == null) {
            container = new EhrBaseMockContainer();
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("EHRBASE_URL", "http://localhost:" + container.getServerPort());

        MockServerClient client = new MockServerClient("localhost", container.getServerPort());
        client
                .when(HttpRequest.request().withMethod("GET").withPath(EHR_BASE_URL))
                .respond(HttpResponse.response().withStatusCode(HttpStatusCode.OK_200.code()).withBody("[{\"template_id\": \"IDCR - Immunisation summary.v0\",\"concept\": \"IDCR - Immunisation summary.v0\",\"archetype_id\": \"openEHR-EHR-COMPOSITION.health_summary.v1\",\"created_timestamp\": \"2020-11-25T16:19:37.812Z\"}]", MediaType.JSON_UTF_8));
    }

    @Override
    public void stop() {
        // do nothing, JVM handles shut down
    }
}
