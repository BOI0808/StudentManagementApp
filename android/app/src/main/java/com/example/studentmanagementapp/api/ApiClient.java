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
            
            // Interceptor tự động thêm Header Authorization và xử lý tự động Refresh Token khi gặp lỗi 401/403
            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();
                    Request.Builder builder = originalRequest.newBuilder();

                    // 1. Đính kèm Access Token hiện tại vào Header
                    Context context = MyApplication.getAppContext();
                    if (context != null) {
                        SharedPreferences sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                        String token = sharedPref.getString("access_token", "");
                        if (token != null && !token.trim().isEmpty()) {
                            builder.header("Authorization", "Bearer " + token);
                        }
                    }

                    Response response = chain.proceed(builder.build());

                    // 2. Bắt mã lỗi 401 (Unauthorized) hoặc 403 (Forbidden) khi Access Token hết hạn
                    if (response.code() == 401 || response.code() == 403) {
                        String urlPath = originalRequest.url().encodedPath();
                        
                        // Tránh đệ quy vô hạn đối với các API đăng nhập và đổi/làm mới token
                        if (urlPath.contains("api/auths/dang-nhap") || urlPath.contains("api/auths/refresh-token")) {
                            return response;
                        }

                        // Sử dụng cơ chế đồng bộ hóa (synchronized block) để tránh xung đột luồng khi nhiều request đồng thời hết hạn
                        synchronized (lock) {
                            if (context != null) {
                                SharedPreferences sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                                String currentToken = sharedPref.getString("access_token", "");
                                String originalAuthHeader = originalRequest.header("Authorization");
                                String currentAuthHeader = "Bearer " + currentToken;

                                // Nếu Token trong SharedPreferences đã khác so với lúc gửi Request (luồng khác đã làm mới thành công)
                                // Ta chỉ việc thử lại Request ban đầu với Token mới vừa nhận được
                                if (originalAuthHeader != null && !originalAuthHeader.equals(currentAuthHeader)) {
                                    response.close(); // Đóng Response trước khi Retry
                                    return chain.proceed(originalRequest.newBuilder()
                                            .header("Authorization", currentAuthHeader)
                                            .build());
                                }

                                // Nếu chưa có luồng nào làm mới Token, tiến hành gọi API đổi Token mới đồng bộ (Synchronous)
                                String refreshToken = sharedPref.getString("refresh_token", "");
                                if (refreshToken != null && !refreshToken.trim().isEmpty()) {
                                    String newToken = performTokenRefresh(refreshToken);
                                    if (newToken != null && !newToken.isEmpty()) {
                                        response.close(); // Đóng Response trước khi Retry
                                        return chain.proceed(originalRequest.newBuilder()
                                                .header("Authorization", "Bearer " + newToken)
                                                .build());
                                    } else {
                                        // Trường hợp Refresh Token không hợp lệ hoặc đã hết hạn -> Đăng xuất người dùng
                                        handleLogout(context);
                                    }
                                } else {
                                    // Không tìm thấy Refresh Token -> Đăng xuất người dùng
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
            // Tạo một OkHttpClient phụ hoàn toàn độc lập, không gắn AuthInterceptor để tránh lặp đệ quy vô hạn
            OkHttpClient baseClient = new OkHttpClient.Builder().build();

            // Khởi tạo JSON Request chứa Refresh Token
            String jsonBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonBody
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "api/auths/refresh-token")
                    .post(body)
                    .build();

            // Thực hiện cuộc gọi đồng bộ để đổi token ngay lập tức
            try (Response response = baseClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseString = response.body().string();
                    Gson gson = new Gson();
                    LoginResponse loginResponse = gson.fromJson(responseString, LoginResponse.class);

                    if (loginResponse != null && loginResponse.getAccessToken() != null) {
                        String newAccessToken = loginResponse.getAccessToken();
                        String newRefreshToken = loginResponse.getRefreshToken();

                        // Cập nhật SharedPreferences mới
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

    /**
     * Xử lý xóa sạch phiên đăng nhập và đưa người dùng về màn hình đăng nhập khi Refresh Token thất bại
     */
    private static void handleLogout(Context context) {
        // Xóa thông tin cũ đã lưu trong SharedPreferences
        SharedPreferences sharedPref = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();

        // Hiển thị thông báo Toast trên UI Thread
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();
        });

        // Chuyển hướng người dùng về LoginActivity và xóa sạch Stack của Task cũ
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
