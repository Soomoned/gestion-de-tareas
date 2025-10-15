package com.example.gestordetareas.APIMaps;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Unshorten {
    public static String expand(String shortUrl) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(shortUrl).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER) {

                String expandedUrl = connection.getHeaderField("Location");
                if (expandedUrl != null) {
                    return expandedUrl;
                }
            }
            return shortUrl;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}