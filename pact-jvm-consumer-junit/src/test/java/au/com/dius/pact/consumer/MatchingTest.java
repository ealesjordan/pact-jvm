package au.com.dius.pact.consumer;

import au.com.dius.pact.model.MockProviderConfig;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MatchingTest {

    @Test
    public void testRegexpMatchingOnBody() {
        PactDslJsonBody body = new PactDslJsonBody()
            .stringMatcher("name", "\\w+", "harry")
            .stringMatcher("position", "staff|contactor");

        PactDslJsonBody responseBody = new PactDslJsonBody()
            .stringMatcher("name", "\\w+", "harry");

        HashMap expectedResponse = new HashMap();
        expectedResponse.put("name", "harry");
        runTest(buildPactFragment(body, responseBody, "a test interaction that requires regex matching"),
            "{\"name\": \"Arnold\", \"position\": \"staff\"}", expectedResponse, "/hello");
    }

    @Test
    public void testMatchingByTypeOnBody() {
        PactDslJsonBody body = new PactDslJsonBody()
                .stringType("name")
                .booleanType("happy")
                .hexValue("hexCode")
                .id()
                .ipAddress("localAddress")
                .numberValue("age", 100)
                .timestamp();

        PactDslJsonBody responseBody = new PactDslJsonBody();

        HashMap expectedResponse = new HashMap();
        runTest(buildPactFragment(body, responseBody, "a test interaction that requires type matching"),
            new JSONObject()
                .put("name", "Giggle and Hoot")
                .put("happy", true)
                .put("hexCode", "abcdef0123456789")
                .put("id", 1234567890)
                .put("localAddress", "192.168.0.1")
                .put("age", 100)
                .put("timestamp", DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date()))
                .toString(),
            expectedResponse, "/hello");
    }

    @Test
    public void testRegexpMatchingOnPath() {
        ConsumerPactBuilder.PactDslResponse fragment = ConsumerPactBuilder
            .consumer("test_consumer")
            .hasPactWith("test_provider")
            .uponReceiving("a request to match on path")
            .matchPath("/hello/[0-9]{4}")
            .method("POST")
            .body("{}", ContentType.APPLICATION_JSON)
            .willRespondWith()
            .status(200);
        Map expectedResponse = new HashMap();
        runTest(fragment, "{}", expectedResponse, "/hello/1234");
    }

    private void runTest(ConsumerPactBuilder.PactDslResponse pactFragment, final String body, final Map expectedResponse, final String path) {
        MockProviderConfig config = MockProviderConfig.createDefault();
        VerificationResult result = pactFragment.toFragment().runConsumer(config, new TestRun() {
            @Override
            public void run(MockProviderConfig config) {
                try {
                    assertEquals(new ConsumerClient(config.url()).post(path, body, ContentType.APPLICATION_JSON), expectedResponse);
                } catch (IOException e) {
                }
            }
        });

        if (result instanceof PactError) {
            throw new RuntimeException(((PactError)result).error());
        }

        assertEquals(ConsumerPactTest.PACT_VERIFIED, result);
    }

    private ConsumerPactBuilder.PactDslResponse buildPactFragment(PactDslJsonBody body, PactDslJsonBody responseBody, String description) {
        return ConsumerPactBuilder
            .consumer("test_consumer")
            .hasPactWith("test_provider")
            .uponReceiving(description)
                .path("/hello")
                .method("POST")
                .body(body)
            .willRespondWith()
                .status(200)
                .body(responseBody);
    }

}
