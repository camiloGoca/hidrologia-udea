package edu.udea.hidrologia.shared.firebase;

import java.io.IOException;

import com.google.auth.oauth2.GoogleCredentials;

@FunctionalInterface
interface FirebaseCredentialsProvider {

    GoogleCredentials getApplicationDefault() throws IOException;
}
