package lb.microservice.product.composite;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lb.microservice.api.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

@Slf4j
public class IsSameEvent extends TypeSafeMatcher<String> {


    private ObjectMapper mapper = new ObjectMapper();

    private Event expectedEvent;


    private IsSameEvent(Event expectedEvent) {
        mapper.registerModule(new JavaTimeModule());
        this.expectedEvent = expectedEvent;
    }

    @Override
    protected boolean matchesSafely(String eventAsJson) {

        if (expectedEvent == null) {
            return false;
        }

        log.trace("Convert the following json string to a map: {}", eventAsJson);
        Map<String, Object> mapEvent = convertJsonStringToMap(eventAsJson);
        mapEvent.remove("eventCreatedAt");

        Map<String, Object> mapExpectedEvent = getMapWithoutCreatedAt(expectedEvent);

        log.trace("Got the map: {}", mapEvent);
        log.trace("Compare to the expected map: {}", mapExpectedEvent);
        return mapEvent.equals(mapExpectedEvent);
    }

    @Override
    public void describeTo(Description description) {
        String expectedJson = convertObjectToJsonString(expectedEvent);
        description.appendText("expected to look like " + expectedJson);
    }

    public static Matcher<String> sameEventExceptCreatedAt(Event expectedEvent) {
        return new IsSameEvent(expectedEvent);
    }

    private Map<String, Object> getMapWithoutCreatedAt(Event event) {
        Map<String, Object> mapEvent = convertObjectToMap(event);
        mapEvent.remove("eventCreatedAt");
        return mapEvent;
    }

    private Map convertObjectToMap(Object object) {
        JsonNode node = mapper.convertValue(object, JsonNode.class);
        return mapper.convertValue(node, Map.class);
    }

    private String convertObjectToJsonString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> convertJsonStringToMap(String eventAsJson) {
        try {
            return mapper.readValue(eventAsJson, new TypeReference<HashMap>(){});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}