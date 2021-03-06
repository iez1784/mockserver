package org.mockserver.client;

import com.google.common.annotations.VisibleForTesting;
import io.netty.channel.nio.NioEventLoopGroup;
import org.mockserver.client.MockServerEventBus.EventType;
import org.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.mock.Expectation;
import org.mockserver.mock.action.ExpectationCallback;
import org.mockserver.mock.action.ExpectationForwardAndResponseCallback;
import org.mockserver.mock.action.ExpectationForwardCallback;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.*;
import org.mockserver.scheduler.Scheduler;
import org.mockserver.closurecallback.websocketclient.WebSocketClient;
import org.mockserver.closurecallback.websocketclient.WebSocketException;

import java.util.UUID;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockserver.configuration.ConfigurationProperties.maxFutureTimeout;

/**
 * @author jamesdbloom
 */
public class ForwardChainExpectation {

    private final MockServerLogger mockServerLogger;
    private final MockServerClient mockServerClient;
    private final Expectation expectation;
    private final MockServerEventBus mockServerEventBus;

    ForwardChainExpectation(MockServerLogger mockServerLogger, MockServerEventBus mockServerEventBus, MockServerClient mockServerClient, Expectation expectation) {
        this.mockServerLogger = mockServerLogger;
        this.mockServerEventBus = mockServerEventBus;
        this.mockServerClient = mockServerClient;
        this.expectation = expectation;
    }

    /**
     * Return response when expectation is matched
     *
     * @param httpResponse response to return
     */
    public void respond(final HttpResponse httpResponse) {
        expectation.thenRespond(httpResponse);
        mockServerClient.upsert(expectation);
    }

    /**
     * Evaluate Velocity or JavaScript template to generate response
     * to return when expectation is matched
     *
     * @param httpTemplate Velocity or JavaScript template used to generate response
     */
    public void respond(final HttpTemplate httpTemplate) {
        expectation.thenRespond(httpTemplate);
        mockServerClient.upsert(expectation);
    }

    /**
     * Call method on local class in same JVM implementing ExpectationResponseCallback
     * to generate response to return when expectation is matched
     * <p>
     * The callback class must:
     * - implement org.mockserver.mock.action.ExpectationResponseCallback
     * - have a zero argument constructor
     * - be available in the classpath of the MockServer
     *
     * @param httpClassCallback class to callback as a fully qualified class name, i.e. "com.foo.MyExpectationResponseCallback"
     */
    public void respond(final HttpClassCallback httpClassCallback) {
        expectation.thenRespond(httpClassCallback);
        mockServerClient.upsert(expectation);
    }

    /**
     * Call method on object locally or remotely (over web socket)
     * to generate response to return when expectation is matched
     *
     * @param expectationResponseCallback object to call locally or remotely to generate response
     */
    public void respond(final ExpectationResponseCallback expectationResponseCallback) {
        expectation.thenRespond(new HttpObjectCallback().withClientId(registerWebSocketClient(expectationResponseCallback, null)));
        mockServerClient.upsert(expectation);
    }

    /**
     * Call method on object locally or remotely (over web socket)
     * to generate response to return when expectation is matched
     *
     * @param expectationResponseCallback object to call locally or remotely to generate response
     */
    public void respond(final ExpectationResponseCallback expectationResponseCallback, Delay delay) {
        expectation
            .thenRespond(
                new HttpObjectCallback()
                    .withClientId(registerWebSocketClient(expectationResponseCallback, null))
                    .withDelay(delay)
            );
        mockServerClient.upsert(expectation);
    }

    /**
     * Forward request to the specified host and port when expectation is matched
     *
     * @param httpForward host and port to forward to
     */
    public void forward(final HttpForward httpForward) {
        expectation.thenForward(httpForward);
        mockServerClient.upsert(expectation);
    }

    /**
     * Evaluate Velocity or JavaScript template to generate
     * request to forward when expectation is matched
     *
     * @param httpTemplate Velocity or JavaScript template used to generate response
     */
    public void forward(final HttpTemplate httpTemplate) {
        expectation.thenForward(httpTemplate);
        mockServerClient.upsert(expectation);
    }

    /**
     * Call method on local class in same JVM implementing ExpectationResponseCallback
     * to generate request to forward when expectation is matched
     * <p>
     * The callback class must:
     * - implement org.mockserver.mock.action.ExpectationForwardCallback or org.mockserver.mock.action.ExpectationForwardAndResponseCallback
     * - have a zero argument constructor
     * - be available in the classpath of the MockServer
     *
     * @param httpClassCallback class to callback as a fully qualified class name, i.e. "com.foo.MyExpectationResponseCallback"
     */
    public void forward(final HttpClassCallback httpClassCallback) {
        expectation.thenForward(httpClassCallback);
        mockServerClient.upsert(expectation);
    }

    /**
     * Call method on object locally or remotely (over web socket)
     * to generate request to forward when expectation is matched
     *
     * @param expectationForwardCallback object to call locally or remotely to generate request
     */
    public void forward(final ExpectationForwardCallback expectationForwardCallback) {
        expectation
            .thenForward(
                new HttpObjectCallback()
                    .withClientId(registerWebSocketClient(expectationForwardCallback, null))
            );
        mockServerClient.upsert(expectation);
    }

    /**
     * Call method on object locally or remotely (over web socket)
     * to generate request to forward when expectation is matched
     *
     * @param expectationForwardCallback object to call locally or remotely to generate request
     */
    public void forward(final ExpectationForwardCallback expectationForwardCallback, final ExpectationForwardAndResponseCallback expectationForwardResponseCallback) {
        expectation
            .thenForward(
                new HttpObjectCallback()
                    .withResponseCallback(true)
                    .withClientId(registerWebSocketClient(expectationForwardCallback, expectationForwardResponseCallback))
            );
        mockServerClient.upsert(expectation);
    }

    /**
     * Call method on object locally or remotely (over web socket)
     * to generate request to forward when expectation is matched
     *
     * @param expectationForwardCallback object to call locally or remotely to generate request
     */
    public void forward(final ExpectationForwardCallback expectationForwardCallback, final Delay delay) {
        expectation
            .thenForward(
                new HttpObjectCallback()
                    .withClientId(registerWebSocketClient(expectationForwardCallback, null))
                    .withDelay(delay)
            );
        mockServerClient.upsert(expectation);
    }

    /**
     * Call method on object locally or remotely (over web socket)
     * to generate request to forward when expectation is matched
     *
     * @param expectationForwardCallback object to call locally or remotely to generate request
     */
    public void forward(final ExpectationForwardCallback expectationForwardCallback, final ExpectationForwardAndResponseCallback expectationForwardResponseCallback, final Delay delay) {
        expectation
            .thenForward(
                new HttpObjectCallback()
                    .withResponseCallback(true)
                    .withClientId(registerWebSocketClient(expectationForwardCallback, expectationForwardResponseCallback))
                    .withDelay(delay)
            );
        mockServerClient.upsert(expectation);
    }

    /**
     * Override fields, headers, and cookies etc in request being forwarded with
     * specified fields, headers and cookies, etc in the specified request
     * when expectation is matched
     *
     * @param httpOverrideForwardedRequest contains request to override request being forwarded
     */
    public void forward(final HttpOverrideForwardedRequest httpOverrideForwardedRequest) {
        expectation.thenForward(httpOverrideForwardedRequest);
        mockServerClient.upsert(expectation);
    }

    /**
     * Return error when expectation is matched
     *
     * @param httpError error to return
     */
    public void error(final HttpError httpError) {
        expectation.thenError(httpError);
        mockServerClient.upsert(expectation);
    }

    @SuppressWarnings("rawtypes")
    private <T extends HttpObject> String registerWebSocketClient(ExpectationCallback<T> expectationCallback, ExpectationForwardAndResponseCallback expectationForwardResponseCallback) {
        try {
            String clientId = UUID.randomUUID().toString();
            LocalCallbackRegistry.registerCallback(clientId, expectationCallback);
            LocalCallbackRegistry.registerCallback(clientId, expectationForwardResponseCallback);
            final WebSocketClient<T> webSocketClient = new WebSocketClient<>(
                new NioEventLoopGroup(ConfigurationProperties.webSocketClientEventLoopThreadCount(), new Scheduler.SchedulerThreadFactory(WebSocketClient.class.getSimpleName() + "-eventLoop")),
                clientId,
                mockServerLogger
            );
            final Future<String> register = webSocketClient.registerExpectationCallback(
                expectationCallback,
                expectationForwardResponseCallback,
                mockServerClient.remoteAddress(),
                mockServerClient.contextPath(),
                mockServerClient.isSecure()
            );
            mockServerEventBus.subscribe(webSocketClient::stopClient, EventType.STOP, EventType.RESET);
            return register.get(maxFutureTimeout(), MILLISECONDS);
        } catch (Exception e) {
            if (e.getCause() instanceof WebSocketException) {
                throw new ClientException(e.getCause().getMessage(), e);
            } else {
                throw new ClientException("Unable to retrieve client registration id", e);
            }
        }
    }

    @VisibleForTesting
    Expectation getExpectation() {
        return expectation;
    }

}
