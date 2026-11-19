package example.orders;

public final class JwtAuthenticationFilter {
    public boolean authenticate(String authorizationHeader) {
        return authorizationHeader != null && authorizationHeader.startsWith("Bearer ");
    }
}
