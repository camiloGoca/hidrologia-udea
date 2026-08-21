package edu.udea.hidrologia.shared.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(FirebaseAuth.class)
class FirebaseAuthTokenVerifier implements FirebaseTokenVerifier {

    private final FirebaseAuth firebaseAuth;

    FirebaseAuthTokenVerifier(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public VerifiedFirebaseToken verify(String idToken) {
        try {
            FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(idToken, true);

            return new VerifiedFirebaseToken(firebaseToken.getUid());
        } catch (FirebaseAuthException exception) {
            throw new FirebaseTokenVerificationException("Firebase ID token could not be verified", exception);
        }
    }
}
