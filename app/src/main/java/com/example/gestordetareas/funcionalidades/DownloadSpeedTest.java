package com.example.gestordetareas.funcionalidades;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadSpeedTest {

    public interface SpeedTestCallback {
        void onSuccess(float speedMbps);
        void onError(String error);
    }

    public static void testDownloadSpeed(String fileUrl, SpeedTestCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setUseCaches(false);
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    callback.onError("HTTP Error: " + responseCode);
                    return;
                }

                long startTime = System.currentTimeMillis();

                InputStream input = new BufferedInputStream(connection.getInputStream());
                byte[] buffer = new byte[1024 * 1024]; // 1MB
                int bytesRead;
                long totalBytes = 0;

                while ((bytesRead = input.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                }

                input.close();
                connection.disconnect();

                long endTime = System.currentTimeMillis();
                float timeTakenSeconds = (endTime - startTime) / 1000f;

                float speedMbps = (totalBytes * 8f) / (timeTakenSeconds * 1024 * 1024); // Mbps

                callback.onSuccess(speedMbps);

            } catch (Exception e) {
                Log.e("SpeedTest", "Error descargando archivo", e);
                callback.onError("Error en test de velocidad: " + e.getMessage());
            }
        }).start();
    }
}