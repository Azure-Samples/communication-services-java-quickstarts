import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests()
            .requestMatchers("/ws/**").permitAll() // WebSocket handshake is handled separately
            .anyRequest()
            .and()
            .oauth2ResourceServer()
            .jwt()
            .decoder(jwtDecoder());

        return http.build();
    }

    class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private String audience;

        OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);

        public AudienceValidator(String audience) {
            this.audience = audience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            if (token.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            } else {
                return OAuth2TokenValidatorResult.failure(error);
            }
        }
    }

    JwtDecoder jwtDecoder() 
    { 
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(audience);         
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);        
         OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withAudience, withIssuer);         
         NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(issuer);         
         jwtDecoder.setJwtValidator(validator);         
         
         return jwtDecoder;     
    }
 
}
