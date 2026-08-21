package edu.udea.hidrologia.shared.firebase;

public interface FirebaseTokenVerifier {

    VerifiedFirebaseToken verify(String idToken);
}
