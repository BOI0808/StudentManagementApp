package com.example.studentmanagementapp.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.example.studentmanagementapp.LoginActivity;
import com.example.studentmanagementapp.MyApplication;
import com.example.studentmanagementapp.model.LoginResponse;
import com.google.gson.Gson;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2:3000/";
    private static Retrofit retrofit = null;
    private static final Object lock = new Object();

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();
                    Request.Builder builder = originalRequest.newBuilder();

                    Context context = MyApplication.getAppContext();
                    if (context != null) {
                        SharedPreferences sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                        String token = sharedPref.getString("access_token", "");
                        if (token != null && !token.trim().isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                    }

                    Response response = chain.proceed(builder.build());

                    if (response.code() == 401 || response.code() == 403) {
                        String urlPath = originalRequest.url().encodedPath();
                        
                        if (urlPath.contains("api/auths/dang-nhap") || urlPath.contains("api/auths/refresh-token")) {
                            return response;
                        }

                        synchronized (lock) {
                            if (context != null) {
                                SharedPreferences sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                                String currentToken = sharedPref.getString("access_token", "");
                                String originalAuthHeader = originalRequest.header("Authorization");
                                String currentAuthHeader = "Bearer " + currentToken;

                                if (originalAuthHeader != null && !originalAuthHeader.equals(currentAuthHeader)) {
                                    response.close();
                                    return chain.proceed(originalRequest.newBuilder()
                                            .header("Authorization", currentAuthHeader)
                                            .build());
                                }

                                String refreshToken = sharedPref.getString("refresh_token", "");
                                if (refreshToken != null && !refreshToken.trim().isEmpty()) {
                                    String newToken = performTokenRefresh(refreshToken);
                                    if (newToken != null && !newToken.isEmpty()) {
                                        response.close();
                                        return chain.proceed(originalRequest.newBuilder()
                                                .header("Authorization", "Bearer " + newToken)
                                                .build());
                                    } else {
                                        handleLogout(context);
                                    }
                                } else {
                                    handleLogout(context);
                                }
                            }
                        }
                    }

                    return response;
                }
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    /**
     * Thực hiện cuộc gọi đồng bộ (Synchronous Call) lên Endpoint /api/auths/refresh-token để lấy Access Token mới
     */
    private static String performTokenRefresh(String refreshToken) {
        try {
            OkHttpClient baseClient = new OkHttpClient.Builder().build();

            String jsonBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "api/auths/refresh-token")
                    .post(body)
                    .build();

            try (Response response = baseClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString = response.body().string();
                    Gson gson = new Gson();
                    LoginResponse loginResponse = gson.fromJson(responseString, LoginResponse.class);

                    if (loginResponse != null && loginResponse.getAccessToken() != null) {
                        String newAccessToken = loginResponse.getAccessToken();
                        String newRefreshToken = loginResponse.getRefreshToken();

                        Context context = MyApplication.getAppContext();
                        if (context != null) {
                            SharedPreferences sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString("access_token", newAccessToken);
                            if (newRefreshToken != null && !newRefreshToken.isEmpty()) {
                                editor.putString("refresh_token", newRefreshToken);
                            }
                            editor.apply();
                        }
                        return newAccessToken;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void handleLogout(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();

        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();
        });

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
