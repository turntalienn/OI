package io.oi.core.agent;

// A dummy annotation for testing purposes. In a real scenario, this would be org.springframework.stereotype.Service.
@interface Service {}

@Service
public class SampleService {

    public String greet(String name) {
        if (name == null || name.isEmpty()) {
            return "Hello, Guest!";
        }
        return "Hello, " + name;
    }

    public void exceptionalMethod() {
        throw new IllegalArgumentException("This is a test exception");
    }

    public int add(int a, int b) {
        return a + b;
    }
} 