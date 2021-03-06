/*
 * Copyright 2018 Alfresco Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.mockeventgenerator;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.alfresco.event.databind.EventObjectMapperFactory;
import org.alfresco.event.model.EventV1;
import org.alfresco.event.model.ResourceV1;
import org.alfresco.mockeventgenerator.EventController.CloudConnectorPayload;
import org.alfresco.mockeventgenerator.EventController.EventRequestPayload;
import org.alfresco.mockeventgenerator.EventMaker.PublicActivitiEventInstance;
import org.alfresco.mockeventgenerator.config.EventConfig;
import org.alfresco.mockeventgenerator.model.CloudConnectorIntegrationRequest;
import org.alfresco.sync.events.types.RepositoryEvent;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jamal Kaabi-Mofrad
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class AbstractCamelTest
{
    private static final String ROUTE_ID = "MOCK-ID";
    private static final ObjectMapper PUBLIC_OBJECT_MAPPER = EventObjectMapperFactory.createInstance();
    private static final ObjectMapper RAW_OBJECT_MAPPER = EventConfig.createAcsRawEventObjectMapper();
    private static final String BASE_URL = "http://localhost:{0}/alfresco/mock/";

    @Autowired
    protected CamelContext camelContext;

    @EndpointInject(uri = "mock:resultEndpoint")
    protected MockEndpoint mockEndpoint;

    @Autowired
    protected EventSender eventSender;

    @Autowired
    protected CamelMessageProducer camelMessageProducer;

    @Autowired
    protected TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    private ObjectMapper defaultObjectMapper;
    private String baseUrl;

    @Before
    public void setUp() throws Exception
    {
        this.defaultObjectMapper = camelMessageProducer.getObjectMapper();
        MessageFormat messageFormat = new MessageFormat(BASE_URL);
        this.baseUrl = messageFormat.format(new String[] { Integer.toString(port) });
        // Configure route
        configureRoute();
    }

    @After
    public void tearDown() throws Exception
    {
        MockEndpoint.resetMocks(camelContext);
        camelContext.removeRoute(ROUTE_ID);
        camelMessageProducer.setObjectMapper(defaultObjectMapper);
    }

    @Test
    public void testSendAndReceiveMockAcsRawEvent() throws Exception
    {
        // Override camelMessageProducer mapper
        camelMessageProducer.setObjectMapper(RAW_OBJECT_MAPPER);

        // Generate random events
        RepositoryEvent event1 = EventMaker.getRandomRawAcsEvent();
        RepositoryEvent event2 = EventMaker.getRandomRawAcsEvent();

        // Set the expected messages
        mockEndpoint.expectedBodiesReceived(
                    Arrays.asList(RAW_OBJECT_MAPPER.writeValueAsString(event1), RAW_OBJECT_MAPPER.writeValueAsString(event2)));
        // Set the expected number of messages
        mockEndpoint.expectedMessageCount(2);

        // Send the 1st event
        eventSender.sendEvent(event1);
        // Send the 2nd event
        eventSender.sendEvent(event2);

        // Checks that the received message count is equal to the number of messages sent
        // Also, checks the received message body is equal to the sent message
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testSendAndReceiveMockAcsPublicEvent() throws Exception
    {
        // Generate random events
        EventV1<? extends ResourceV1> event1 = EventMaker.getRandomPublicAcsEvent();
        EventV1<? extends ResourceV1> event2 = EventMaker.getRandomPublicAcsEvent();

        // Set the expected messages
        mockEndpoint.expectedBodiesReceived(
                    Arrays.asList(PUBLIC_OBJECT_MAPPER.writeValueAsString(event1), PUBLIC_OBJECT_MAPPER.writeValueAsString(event2)));
        // Set the expected number of messages
        mockEndpoint.expectedMessageCount(2);

        // Send the 1st event
        eventSender.sendEvent(event1);
        // Send the 2nd event
        eventSender.sendEvent(event2);

        // Checks that the received message count is equal to the number of messages sent
        // Also, checks the received message body is equal to the sent message
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testSendAndReceiveMockActivitiRawEvent() throws Exception
    {
        // Generate random events
        String event1 = EventMaker.getRandomRawActivitiEvent();
        String event2 = EventMaker.getRandomRawActivitiEvent();

        // Set the expected messages
        mockEndpoint.expectedBodiesReceived(Arrays.asList(event1, event2));
        // Set the expected number of messages
        mockEndpoint.expectedMessageCount(2);

        // Send the 1st event
        eventSender.sendEvent(event1);
        // Send the 2nd event
        eventSender.sendEvent(event2);

        // Checks that the received message count is equal to the number of messages sent
        // Also, checks the received message body is equal to the sent message
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testSendAndReceiveMockActivitiPublicEvent() throws Exception
    {
        // Generate random events
        List<EventV1<? extends ResourceV1>> events1 = PublicActivitiEventInstance.PROCESS_CREATED.getEvents();
        List<EventV1<? extends ResourceV1>> events2 = PublicActivitiEventInstance.TASK_ASSIGNED.getEvents();

        List<EventV1<? extends ResourceV1>> allEvents = new ArrayList<>(events1);
        allEvents.addAll(events2);
        List<String> expectedBodies = new ArrayList<>();
        for (EventV1<? extends ResourceV1> event : allEvents)
        {
            expectedBodies.add(PUBLIC_OBJECT_MAPPER.writeValueAsString(event));
        }

        // Set the expected messages
        mockEndpoint.expectedBodiesReceived(expectedBodies);
        // Set the expected number of messages
        mockEndpoint.expectedMessageCount(12);

        // Send the 1st event. This should generate 11 events.
        eventSender.sendEvent(events1);
        // Send the 2nd event. This should generate 1 event
        eventSender.sendEvent(events2);

        // Checks that the received message count is equal to the number of messages sent
        // Also, checks the received message body is equal to the sent message
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testSendAndReceiveMockConnectorEvent() throws Exception
    {
        // Generate random events
        CloudConnectorIntegrationRequest event1 = EventMaker.getRandomCloudConnectorEvent();
        CloudConnectorIntegrationRequest event2 = EventMaker.getRandomCloudConnectorEvent();

        // Set the expected messages
        mockEndpoint.expectedBodiesReceived(
                    Arrays.asList(PUBLIC_OBJECT_MAPPER.writeValueAsString(event1), PUBLIC_OBJECT_MAPPER.writeValueAsString(event2)));
        // Set the expected number of messages
        mockEndpoint.expectedMessageCount(2);

        // Send the 1st event
        eventSender.sendEvent(event1);
        // Send the 2nd event
        eventSender.sendEvent(event2);

        // Checks that the received message count is equal to the number of messages sent
        // Also, checks the received message body is equal to the sent message
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testMockEventsViaRestApi() throws Exception
    {
        final int numOfEvents = 2;
        EventRequestPayload payload = new EventRequestPayload();
        payload.setNumOfEvents(numOfEvents);
        payload.setPauseTimeInMillis(-1L);

        // Set the expected number of messages
        mockEndpoint.expectedMessageCount(numOfEvents);
        // Send event via Rest API
        restTemplate.postForLocation(baseUrl + "events", payload);

        // Checks that the received message count is equal to the number of messages sent
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testCustomConnectorEventViaRestApi_inBoundVars() throws Exception
    {
        Map<String, Object> inBoundVariables = new HashMap<>();

        CloudConnectorPayload payload = new CloudConnectorPayload();
        inBoundVariables.put("properties", Collections.singletonMap("cm:title", "Test Title"));
        inBoundVariables.put("nodeId", UUID.randomUUID().toString());
        payload.setInBoundVariables(inBoundVariables);

        // Set the expected number of messages
        mockEndpoint.expectedMessageCount(1);
        // Send event via Rest API
        restTemplate.postForLocation(baseUrl + "connector-event", payload);

        String receivedEvent = getBody(mockEndpoint, 0);
        assertNotNull(receivedEvent);
        assertTrue(receivedEvent.contains(PUBLIC_OBJECT_MAPPER.writeValueAsString(inBoundVariables)));

        // Checks that the received message count is equal to the number of messages sent
        mockEndpoint.assertIsSatisfied();
    }

    @Test
    public void testCustomConnectorEventViaRestApi_inBoundAndOutBoundVars() throws Exception
    {
        String nodeId = UUID.randomUUID().toString();
        Map<String, Object> inBoundVariables = new HashMap<>();
        Map<String, Object> outBoundVariables = new HashMap<>();

        CloudConnectorPayload payload = new CloudConnectorPayload();
        // Set inBoundVariables
        inBoundVariables.put("properties", Collections.singletonMap("cm:description", "Test Description."));
        inBoundVariables.put("nodeId", nodeId);
        payload.setInBoundVariables(inBoundVariables);
        // Set outBoundVariables
        outBoundVariables.put("nodeId", nodeId);
        payload.setOutBoundVariables(outBoundVariables);

        // Set the expected number of messages
        mockEndpoint.expectedMessageCount(1);
        // Send event via Rest API
        restTemplate.postForLocation(baseUrl + "connector-event", payload);

        String receivedEvent = getBody(mockEndpoint, 0);
        assertNotNull(receivedEvent);
        assertTrue(receivedEvent.contains(PUBLIC_OBJECT_MAPPER.writeValueAsString(inBoundVariables)));
        assertTrue(receivedEvent.contains(PUBLIC_OBJECT_MAPPER.writeValueAsString(outBoundVariables)));

        // Checks that the received message count is equal to the number of messages sent
        mockEndpoint.assertIsSatisfied();
    }

    protected void configureRoute() throws Exception
    {
        camelContext.addRoutes(new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from(getRoute())
                            .id(ROUTE_ID)
                            .to(mockEndpoint);
            }
        });
    }

    protected String getBody(MockEndpoint mockEndpoint, int index)
    {
        List<Exchange> list = mockEndpoint.getExchanges();
        if (list.size() <= index)
        {
            return null;
        }
        return list.get(index).getIn().getBody().toString();
    }

    protected abstract String getRoute();
}
