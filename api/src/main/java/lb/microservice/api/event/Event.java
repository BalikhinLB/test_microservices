package lb.microservice.api.event;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Getter
@NoArgsConstructor
public class Event <K,T> {
    private Type eventType;
    private K key;
    private T data;
    private ZonedDateTime eventCreatedAt;

    public Event(Type eventType, K key, T data) {
        this.eventType = eventType;
        this.key = key;
        this.data = data;
        eventCreatedAt = ZonedDateTime.now();
    }

    public enum Type{
        CREATE, DELETE
    }
}
