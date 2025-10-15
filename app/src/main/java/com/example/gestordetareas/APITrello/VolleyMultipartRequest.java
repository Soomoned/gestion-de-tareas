package com.example.gestordetareas.APITrello;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public abstract class VolleyMultipartRequest extends Request<NetworkResponse> {

    private final Response.Listener<NetworkResponse> mListener;
    private final Map<String, String> mHeaders;

    public VolleyMultipartRequest(int method, String url,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mHeaders = null;
    }

    public VolleyMultipartRequest(int method, String url,
                                  Map<String, String> headers,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mHeaders = headers;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return mHeaders != null ? mHeaders : super.getHeaders();
    }

    @Override
    public String getBodyContentType() {
        return getMultipartBody().getContentType();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            getMultipartBody().writeTo(bos);
        } catch (IOException e) {
            throw new AuthFailureError("IOException writing to ByteArrayOutputStream");
        }
        return bos.toByteArray();
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    /**
     * Subclases deben implementar esto para construir el cuerpo multipart.
     */
    protected abstract MultipartBody getMultipartBody();

    /**
     * Clase interna que arma el cuerpo del formulario multipart.
     */
    public static class MultipartBody {
        private static final String BOUNDARY = "apiclient-" + System.currentTimeMillis();
        private static final String LINE_FEED = "\r\n";
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        public void addFormField(String name, String value) {
            try {
                outputStream.write(("--" + BOUNDARY + LINE_FEED).getBytes());
                outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_FEED).getBytes());
                outputStream.write(("Content-Type: text/plain; charset=UTF-8" + LINE_FEED).getBytes());
                outputStream.write(LINE_FEED.getBytes());
                outputStream.write(value.getBytes("UTF-8"));
                outputStream.write(LINE_FEED.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void addFilePart(String fieldName, String fileName, byte[] fileData, String mimeType) {
            try {
                outputStream.write(("--" + BOUNDARY + LINE_FEED).getBytes());
                outputStream.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"" + LINE_FEED).getBytes());
                outputStream.write(("Content-Type: " + mimeType + LINE_FEED).getBytes());
                outputStream.write(LINE_FEED.getBytes());
                outputStream.write(fileData);
                outputStream.write(LINE_FEED.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getContentType() {
            return "multipart/form-data; boundary=" + BOUNDARY;
        }

        public void writeTo(ByteArrayOutputStream outputStream) throws IOException {
            outputStream.write(this.outputStream.toByteArray());
            outputStream.write(("--" + BOUNDARY + "--" + LINE_FEED).getBytes());
        }
    }
}
