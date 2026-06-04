package com.hong.xin.stock.data.api;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpClientFactory {

    private static final String TAG = "HttpClientFactory";
    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1000L;

    private static volatile OkHttpClient instance;

    private HttpClientFactory() {}

    public static OkHttpClient getClient() {
        if (instance == null) {
            synchronized (HttpClientFactory.class) {
                if (instance == null) {
                    instance = buildClient();
                }
            }
        }
        return instance;
    }

    private static OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(8, 5, TimeUnit.MINUTES))
                .dns(createCachedDns())
                .addInterceptor(new RetryInterceptor())
                .addInterceptor(new LoggingInterceptor())
                .retryOnConnectionFailure(true)
                .build();
    }

    private static okhttp3.Dns createCachedDns() {
        return new okhttp3.Dns() {
            private final java.util.Map<String, java.util.List<java.net.InetAddress>> cache =
                    new java.util.LinkedHashMap<String, java.util.List<java.net.InetAddress>>() {
                        @Override
                        protected boolean removeEldestEntry(java.util.Map.Entry<String, java.util.List<java.net.InetAddress>> eldest) {
                            return size() > 64;
                        }
                    };
            private final java.util.Map<String, Long> cacheTime = new java.util.LinkedHashMap<>();
            private static final long CACHE_TTL_MS = 300_000L;

            @Override
            public java.util.List<InetAddress> lookup(String hostname) throws UnknownHostException {
                synchronized (this) {
                    Long cached = cacheTime.get(hostname);
                    java.util.List<InetAddress> addrs = cache.get(hostname);
                    if (addrs != null && cached != null && System.currentTimeMillis() - cached < CACHE_TTL_MS) {
                        return addrs;
                    }
                }
                java.util.List<InetAddress> addresses = okhttp3.Dns.SYSTEM.lookup(hostname);
                synchronized (this) {
                    cache.put(hostname, addresses);
                    cacheTime.put(hostname, System.currentTimeMillis());
                }
                return addresses;
            }
        };
    }

    private static class RetryInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            IOException lastException = null;

            for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
                try {
                    Response response = chain.proceed(request);
                    if (isRecoverable(response)) {
                        response.close();
                        long delay = BASE_BACKOFF_MS * (1L << attempt);
                        Log.w(TAG, "Retry attempt " + attempt + " for " + request.url()
                                + " code=" + response.code() + " delay=" + delay + "ms");
                        sleep(delay);
                        continue;
                    }
                    return response;
                } catch (IOException e) {
                    lastException = e;
                    if (attempt < MAX_RETRIES && isRecoverableException(e)) {
                        long delay = BASE_BACKOFF_MS * (1L << attempt);
                        Log.w(TAG, "Retry attempt " + attempt + " for " + request.url()
                                + " err=" + e.getMessage() + " delay=" + delay + "ms");
                        sleep(delay);
                    } else {
                        break;
                    }
                }
            }

            if (lastException != null) {
                throw lastException;
            }
            throw new IOException("Request failed after " + MAX_RETRIES + " retries: " + request.url());
        }

        private boolean isRecoverable(Response response) {
            int code = response.code();
            return code >= 500 || code == 429 || code == 408;
        }

        private boolean isRecoverableException(IOException e) {
            String msg = e.getMessage();
            if (msg == null) return true;
            String lower = msg.toLowerCase();
            return lower.contains("timeout")
                    || lower.contains("connection")
                    || lower.contains("eof")
                    || lower.contains("reset")
                    || lower.contains("broken pipe")
                    || lower.contains("unexpected end");
        }

        private void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long start = System.currentTimeMillis();
            try {
                Response response = chain.proceed(request);
                long duration = System.currentTimeMillis() - start;
                Log.d(TAG, request.method() + " " + request.url() + " " + response.code() + " " + duration + "ms");
                return response;
            } catch (IOException e) {
                long duration = System.currentTimeMillis() - start;
                Log.w(TAG, request.method() + " " + request.url() + " FAILED " + duration + "ms: " + e.getMessage());
                throw e;
            }
        }
    }
}
