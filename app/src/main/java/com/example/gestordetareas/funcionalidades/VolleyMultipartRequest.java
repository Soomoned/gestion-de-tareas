package com.example.gestordetareas.funcionalidades;
import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class VolleyMultipartRequest extends Request<NetworkResponse> {

    private final Response.Listener<NetworkResponse> mListener;
    private final Map<String, String> mHeaders;
    private final Map<String, String> mStringParts;
    private final Map<String, File> mFileParts;
    private final String BOUNDARY = "Volley-" + System.currentTimeMillis();
    private final String LINE_FEED = "\r\n";

    public VolleyMultipartRequest(int method,
                                  String url,
                                  Map<String, String> headers,
                                  Map<String, String> stringParts,
                                  Map<String, File> fileParts,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mHeaders = headers != null ? headers : new HashMap<>();
        this.mStringParts = stringParts != null ? stringParts : new HashMap<>();
        this.mFileParts = fileParts != null ? fileParts : new HashMap<>();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        mHeaders.put("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        return mHeaders;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        try {
            // Add string parts
            for (Map.Entry<String, String> entry : mStringParts.entrySet()) {
                buildTextPart(dos, entry.getKey(), entry.getValue());
            }

            // Add file parts
            for (Map.Entry<String, File> entry : mFileParts.entrySet()) {
                buildFilePart(dos, entry.getKey(), entry.getValue());
            }

            // End boundary
            dos.writeBytes("--" + BOUNDARY + "--" + LINE_FEED);

            return bos.toByteArray();

        } catch (IOException e) {
            VolleyLog.e("Error writing multipart body: " + e);
            return null;
        }
    }

    private void buildTextPart(DataOutputStream dataStream, String parameterName, String value) throws IOException {
        dataStream.writeBytes("--" + BOUNDARY + LINE_FEED);
        dataStream.writeBytes("Content-Disposition: form-data; name=\"" + parameterName + "\"" + LINE_FEED);
        dataStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + LINE_FEED);
        dataStream.writeBytes(LINE_FEED);
        dataStream.writeBytes(value + LINE_FEED);
        dataStream.flush();
    }

    private void buildFilePart(DataOutputStream dataStream, String fieldName, File file) throws IOException {
        String fileName = file.getName();
        dataStream.writeBytes("--" + BOUNDARY + LINE_FEED);
        dataStream.writeBytes("Content-Disposition: form-data; name=\"" + fieldName +
                "\"; filename=\"" + fileName + "\"" + LINE_FEED);
        dataStream.writeBytes("Content-Type: application/octet-stream" + LINE_FEED);
        dataStream.writeBytes(LINE_FEED);

        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            dataStream.write(buffer, 0, bytesRead);
        }
        dataStream.writeBytes(LINE_FEED);
        dataStream.flush();
        fileInputStream.close();
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, null);
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    // Método helper para crear el request
    public static class Builder {
        private int method = Request.Method.POST;
        private String url;
        private Map<String, String> headers;
        private Map<String, String> stringParts;
        private Map<String, File> fileParts;
        private Response.Listener<NetworkResponse> listener;
        private Response.ErrorListener errorListener;

        public Builder setMethod(int method) {
            this.method = method;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder addStringParts(Map<String, String> stringParts) {
            this.stringParts = stringParts;
            return this;
        }

        public Builder addFileParts(Map<String, File> fileParts) {
            this.fileParts = fileParts;
            return this;
        }

        public Builder setListener(Response.Listener<NetworkResponse> listener) {
            this.listener = listener;
            return this;
        }

        public Builder setErrorListener(Response.ErrorListener errorListener) {
            this.errorListener = errorListener;
            return this;
        }

        public VolleyMultipartRequest build() {
            return new VolleyMultipartRequest(
                    method,
                    url,
                    headers,
                    stringParts,
                    fileParts,
                    listener,
                    errorListener
            );
        }
    }
}