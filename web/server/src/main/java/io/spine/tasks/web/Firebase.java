/*
 * Copyright 2019, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tasks.web;

import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.util.ExponentialBackOff;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;
import io.spine.net.Url;
import io.spine.web.firebase.FirebaseClient;
import io.spine.web.firebase.rest.RemoteDatabaseClient;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * A factory of Firebase Realtime Database clients.
 *
 * <p>Connects to a database emulator started at port {@code 5000}.
 *
 * <p>Supplies fake credentials to Firebase RDB, as the {@code firebase-server} emulator used
 * does not support authentication anyway.
 *
 * <p>Notice that the emulator used is an
 * <a href="https://github.com/urish/firebase-server">unofficial</a> one. It is simple in its
 * installation and provides REST API, sufficient for a quick start application.
 *
 * <p>In production projects, a real Firebase RDB instance should be configured.
 */
final class Firebase {

    private static final Url EMULATOR_URL = Url
            .newBuilder()
            .setSpec("http://127.0.0.1:5000/")
            .vBuild();

    private static final FirebaseClient client = createClient();

    /**
     * Prevents the utility class instantiation.
     */
    private Firebase() {
    }

    static FirebaseClient client() {
        return client;
    }

    private static FirebaseClient createClient() {
        FirebaseDatabase database = emulatorDatabase();
        HttpRequestFactory requestFactory = transportWithBackoff();
        RemoteDatabaseClient client = RemoteDatabaseClient.create(database, requestFactory);
        return client;
    }

    /**
     * Initializes the {@code FirebaseDatabase} instance by establishing a connection to
     * the Firebase RDB emulator.
     */
    private static FirebaseDatabase emulatorDatabase() {
        AccessToken token = tokenForEmulator();
        GoogleCredentials credentials = GoogleCredentials.newBuilder()
                                                         .setAccessToken(token)
                                                         .build();
        FirebaseOptions options =
                FirebaseOptions.builder()
                               .setCredentials(credentials)
                               .setDatabaseUrl(EMULATOR_URL.getSpec())
                               .build();
        FirebaseApp app = FirebaseApp.initializeApp(options);
        return FirebaseDatabase.getInstance(app);
    }

    /**
     * Creates an HTTP request factory which by default uses {@linkplain ExponentialBackOff
     * exponential back-off} strategy upon an {@code IOException}.
     */
    private static HttpRequestFactory transportWithBackoff() {
        HttpTransport transport = new ApacheHttpTransport();
        return transport.createRequestFactory(
                request -> request.setIOExceptionHandler(
                        new HttpBackOffIOExceptionHandler(new ExponentialBackOff())));
    }

    /**
     * Creates a fake {@code AccessToken} suitable for the Firebase RDB emulator.
     */
    private static AccessToken tokenForEmulator() {
        Date expirationDate = Date.from(Instant.now()
                                               .plus(Duration.of(1, ChronoUnit.DAYS)));
        return new AccessToken("emulator-does-not-support-authentication",
                               expirationDate);
    }
}