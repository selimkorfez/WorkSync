package com.worksync.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
public class FirebaseInitializer {

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount = FirebaseInitializer.class
                    .getClassLoader()
                    .getResourceAsStream("serviceAccountKey.json");

            if (serviceAccount == null) {
                throw new IllegalStateException("Could not find serviceAccountKey.json in resources folder.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
