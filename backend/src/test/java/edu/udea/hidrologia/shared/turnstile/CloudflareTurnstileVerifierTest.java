package edu.udea.hidrologia.shared.turnstile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CloudflareTurnstileVerifierTest {

    @Test
    void skipsSiteverifyWhenTurnstileIsDisabled() {
        TurnstileProperties properties = properties(false);
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://challenges.cloudflare.com/turnstile/v0");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        CloudflareTurnstileVerifier verifier =
                new CloudflareTurnstileVerifier(properties, builder.build());

        assertThatCode(() -> verifier.verifyStudentQuestion(null)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void acceptsSuccessfulChallengeWithoutOptionalMetadataExpectations() {
        VerifierFixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("secret=test-secret"),
                        containsString("response=valid-token"))))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "hostname": "example.com",
                          "action": null,
                          "error-codes": []
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatCode(() -> fixture.verifier.verifyStudentQuestion("valid-token")).doesNotThrowAnyException();

        fixture.server.verify();
    }

    @Test
    void acceptsSuccessfulStudentQuestionChallengeWhenExpectedActionMatches() {
        TurnstileProperties properties = properties(true);
        properties.setExpectedAction("student_question");
        VerifierFixture fixture = fixture(properties);
        fixture.server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "hostname": "localhost",
                          "action": "student_question"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatCode(() -> fixture.verifier.verifyStudentQuestion("valid-token")).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingTokenWhenEnabled() {
        CloudflareTurnstileVerifier verifier =
                new CloudflareTurnstileVerifier(properties(true), RestClient.create());

        assertThatThrownBy(() -> verifier.verifyStudentQuestion("   "))
                .isInstanceOf(TurnstileChallengeException.class)
                .hasMessage("Completa nuevamente la verificación y vuelve a intentarlo.");
    }

    @Test
    void rejectsUnsuccessfulChallenge() {
        TurnstileProperties properties = properties(true);
        properties.setExpectedAction("student_question");
        VerifierFixture fixture = fixture(properties);
        fixture.server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withSuccess("""
                        {
                          "success": false,
                          "hostname": "localhost",
                          "action": "student_question",
                          "error-codes": ["invalid-input-response"]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.verifier.verifyStudentQuestion("invalid-token"))
                .isInstanceOf(TurnstileChallengeException.class)
                .hasMessage("Completa nuevamente la verificación y vuelve a intentarlo.");
    }

    @Test
    void rejectsUnexpectedAction() {
        TurnstileProperties properties = properties(true);
        properties.setExpectedAction("student_question");
        VerifierFixture fixture = fixture(properties);
        fixture.server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "hostname": "localhost",
                          "action": "test"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.verifier.verifyStudentQuestion("valid-token"))
                .isInstanceOf(TurnstileChallengeException.class);
    }

    @Test
    void acceptsConfiguredDummyTestAction() {
        TurnstileProperties properties = properties(true);
        properties.setExpectedAction("test");
        properties.setExpectedHostnames("localhost");
        VerifierFixture fixture = fixture(properties);
        fixture.server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "challenge_ts": "2022-02-28T15:14:30.096Z",
                          "hostname": "localhost",
                          "error-codes": [],
                          "action": "test",
                          "cdata": "test-data"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatCode(() -> fixture.verifier.verifyStudentQuestion("valid-token")).doesNotThrowAnyException();
    }

    @Test
    void normalizesQuotedEnvironmentValuesBeforeCallingSiteverify() {
        TurnstileProperties properties = properties(true);
        properties.setSecretKey(" \"test-secret\" ");
        properties.setExpectedAction(" 'test' ");
        properties.setExpectedHostnames(" \"localhost\" ");
        VerifierFixture fixture = fixture(properties);
        fixture.server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(allOf(
                        containsString("secret=test-secret"),
                        containsString("response=valid-token"))))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "hostname": "LOCALHOST",
                          "action": "test",
                          "error-codes": []
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatCode(() -> fixture.verifier.verifyStudentQuestion(" valid-token ")).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingActionInSiteverifyResponse() {
        TurnstileProperties properties = properties(true);
        properties.setExpectedAction("student_question");
        VerifierFixture fixture = fixture(properties);
        fixture.server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "hostname": "localhost"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.verifier.verifyStudentQuestion("valid-token"))
                .isInstanceOf(TurnstileChallengeException.class);
    }

    @Test
    void rejectsUnexpectedHostnameWhenWhitelistIsConfigured() {
        TurnstileProperties properties = properties(true);
        properties.setExpectedAction("student_question");
        properties.setExpectedHostnames("hidrologia.example.edu,localhost");
        VerifierFixture fixture = fixture(properties);
        fixture.server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "hostname": "evil.example.com",
                          "action": "student_question"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.verifier.verifyStudentQuestion("valid-token"))
                .isInstanceOf(TurnstileChallengeException.class);
    }

    @Test
    void failsClosedWhenSiteverifyIsUnavailable() {
        VerifierFixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> fixture.verifier.verifyStudentQuestion("valid-token"))
                .isInstanceOf(TurnstileUnavailableException.class)
                .hasMessage("No pudimos verificar el envío en este momento. Intenta nuevamente.");
    }

    @Test
    void failsClosedWhenSiteverifyResponseIsMalformed() {
        VerifierFixture fixture = fixture(properties(true));
        fixture.server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.verifier.verifyStudentQuestion("valid-token"))
                .isInstanceOf(TurnstileUnavailableException.class)
                .hasMessage("No pudimos verificar el envío en este momento. Intenta nuevamente.");
    }

    private VerifierFixture fixture(TurnstileProperties properties) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://challenges.cloudflare.com/turnstile/v0");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        return new VerifierFixture(
                new CloudflareTurnstileVerifier(properties, builder.build()),
                server);
    }

    private TurnstileProperties properties(boolean enabled) {
        TurnstileProperties properties = new TurnstileProperties();
        properties.setEnabled(enabled);
        properties.setSecretKey("test-secret");
        return properties;
    }

    private record VerifierFixture(
            CloudflareTurnstileVerifier verifier,
            MockRestServiceServer server) {
    }
}
