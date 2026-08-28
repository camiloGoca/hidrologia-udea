package edu.udea.hidrologia.shared.turnstile;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class CloudflareTurnstileVerifier implements TurnstileVerifier {

    public static final String STUDENT_QUESTION_ACTION = "student_question";
    static final String INVALID_CHALLENGE_MESSAGE =
            "Completa nuevamente la verificación y vuelve a intentarlo.";
    static final String UNAVAILABLE_MESSAGE =
            "No pudimos verificar el envío en este momento. Intenta nuevamente.";

    private final TurnstileProperties properties;
    private final RestClient restClient;

    public CloudflareTurnstileVerifier(TurnstileProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public void verifyStudentQuestion(String token) {
        if (!properties.isEnabled()) {
            return;
        }

        if (!StringUtils.hasText(token)) {
            throw new TurnstileChallengeException(INVALID_CHALLENGE_MESSAGE);
        }

        Map<String, Object> response = siteverify(token);

        if (!Boolean.TRUE.equals(response.get("success"))) {
            throw new TurnstileChallengeException(INVALID_CHALLENGE_MESSAGE);
        }

        String expectedAction = properties.getExpectedAction();
        if (StringUtils.hasText(expectedAction) && !expectedAction.equals(response.get("action"))) {
            throw new TurnstileChallengeException(INVALID_CHALLENGE_MESSAGE);
        }

        Set<String> expectedHostnames = properties.expectedHostnameSet();
        String hostname = Objects.toString(response.get("hostname"), "").trim().toLowerCase(Locale.ROOT);
        if (!expectedHostnames.isEmpty() && !expectedHostnames.contains(hostname)) {
            throw new TurnstileChallengeException(INVALID_CHALLENGE_MESSAGE);
        }
    }

    private Map<String, Object> siteverify(String token) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", properties.getSecretKey());
        body.add("response", token.trim());

        try {
            ResponseEntity<Map<String, Object>> responseEntity = restClient.post()
                    .uri("/siteverify")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {
                    });

            Map<String, Object> response = responseEntity.getBody();
            if (response == null) {
                throw new TurnstileUnavailableException(UNAVAILABLE_MESSAGE);
            }

            return response;
        } catch (TurnstileUnavailableException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new TurnstileUnavailableException(UNAVAILABLE_MESSAGE, exception);
        } catch (RestClientException exception) {
            throw new TurnstileUnavailableException(UNAVAILABLE_MESSAGE, exception);
        }
    }
}
