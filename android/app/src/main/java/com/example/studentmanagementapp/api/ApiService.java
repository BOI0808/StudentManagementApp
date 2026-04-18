package com.example.studentmanagementapp.api;

import com.example.studentmanagementapp.model.Block;
import com.example.studentmanagementapp.model.ClassModel;
import com.example.studentmanagementapp.model.LoginResponse;
import com.example.studentmanagementapp.model.Student;
import com.example.studentmanagementapp.model.Subject;
import com.example.studentmanagementapp.model.User;
import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

public interface ApiService {

    // I. Quản lý tài khoản
    @GET("api/users/danh-sach-tai-khoan")
    Call<List<User>> getAccountList();

    @POST("api/users/tao-tai-khoan")
    Call<Map<String, String>> createAccount(@Body User user);

    @PUT("api/users/cap-nhat-tai-khoan/{id}")
    Call<Map<String, String>> updateAccount(@Path("id") String id, @Body User user);

    @DELETE("api/users/xoa-tai-khoan/{id}")
    Call<Map<String, String>> deleteAccount(@Path("id") String id);

    @Multipart
    @POST("api/users/import-excel")
    Call<Map<String, String>> importExcel(@Part MultipartBody.Part file);

    // II. Đăng nhập & đổi mật khẩu
    @POST("api/auths/dang-nhap")
    Call<LoginResponse> login(@Body Map<String, String> loginData);

    @POST("api/auths/doi-mat-khau")
    Call<Map<String, String>> changePassword(@Body Map<String, String> changePasswordData);

    // III. Lập danh mục khối lớp
    @GET("api/blocks/danh-sach-khoi-lop")
    Call<List<Block>> getBlockList();

    @POST("api/blocks/lap-khoi-lop")
    Call<Map<String, String>> createBlock(@Body Block block);

    @PUT("api/blocks/cap-nhat-khoi-lop/{MaKhoiLop}")
    Call<Map<String, String>> updateBlockStatus(@Path("MaKhoiLop") String maKhoiLop, @Body Map<String, Integer> status);

    // IV. Lập danh mục môn học
    @GET("api/subjects/danh-sach-mon-hoc")
    Call<List<Subject>> getSubjectList();

    @POST("api/subjects/lap-mon-hoc")
    Call<Map<String, String>> createSubject(@Body Subject subject);

    @PUT("api/subjects/cap-nhat-mon-hoc/{MaMonHoc}")
    Call<Map<String, String>> updateSubjectStatus(@Path("MaMonHoc") String maMonHoc, @Body Map<String, Integer> status);

    // V. Tiếp nhận học sinh
    @POST("api/students/tiep-nhan-hoc-sinh")
    Call<Map<String, String>> receiveStudent(@Body Student student);

    @GET("api/students/search")
    Call<List<Student>> searchStudent(@Query("key") String key);

    @PUT("api/students/cap-nhat-hoc-sinh")
    Call<Map<String, String>> updateStudent(@Body Student student);

    // VI. Lập danh mục học kỳ năm học
    @GET("api/semesters/danh-sach-hoc-ky-nam-hoc")
    Call<List<Map<String, String>>> getSemesterList();

    @POST("api/semesters/tao-hoc-ky-nam-hoc")
    Call<Map<String, String>> createSemester(@Body Map<String, Object> data);

    @DELETE("api/semesters/xoa-hoc-ky-nam-hoc/{MaHocKyNamHoc}")
    Call<Map<String, String>> deleteSemester(@Path("MaHocKyNamHoc") String maHocKy);

    // VII. Lập lớp học & Lập danh sách học sinh cho lớp
    @POST("api/classes/lap-danh-sach-lop")
    Call<Map<String, String>> createClass(@Body ClassModel classModel);

    @GET("api/classes/danh-sach-lop")
    Call<List<ClassModel>> getClassList();

    @GET("api/classes/tim-kiem-ma-lop")
    Call<List<ClassModel>> suggestClass(@Query("key") String key);

    @GET("api/classes/danh-sach-hoc-sinh-theo-lop/{MaLop}")
    Call<List<Map<String, String>>> getStudentsByClass(@Path("MaLop") String maLop);

    @POST("api/classes/luu-danh-sach-lop")
    Call<Map<String, String>> saveClassList(@Body ClassModel classModel);

    // VIII. Tra cứu học sinh
    @GET("api/students/ket-qua-tra-cuu")
    Call<List<Map<String, Object>>> getSearchResult(
        @Query("maLop") String maLop,
        @Query("hoTen") String hoTen,
        @Query("maHocSinh") String maHocSinh
    );

    // IX. Nhập danh sách các loại kiểm tra
    @GET("api/test-types/danh-sach-loai-kiem-tra")
    Call<List<Map<String, Object>>> getTestTypeList();

    @POST("api/test-types/lap-loai-kiem-tra")
    Call<Map<String, Object>> createTestType(@Body Map<String, Object> data);

    @PATCH("api/test-types/xoa-loai-kiem-tra/{MaLoaiKiemTra}")
    Call<Map<String, String>> deleteTestType(@Path("MaLoaiKiemTra") String maLoai);

    @PATCH("api/test-types/cap-nhat-he-so/{MaLoaiKiemTra}")
    Call<Map<String, String>> updateTestTypeWeight(@Path("MaLoaiKiemTra") String maLoai, @Body Map<String, Object> weight);

    // X. Nhập bảng điểm môn
    @GET("api/grades/nhap-diem/danh-sach")
    Call<List<Map<String, Object>>> getHocSinhNhapDiem(
        @Query("MaLop") String maLop,
        @Query("MaMonHoc") String maMonHoc,
        @Query("MaLoaiKiemTra") String maLoai,
        @Query("MaHocKyNamHoc") String maHK
    );

    // Endpoint sửa từ luu-bang-diem sang nhap-diem cho khớp Backend
    @POST("api/grades/nhap-diem")
    Call<Map<String, String>> saveGrades(@Body Map<String, Object> data);

    // XI & XII. Báo cáo
    @GET("api/reports/bao-cao-mon")
    Call<List<Map<String, Object>>> getSubjectReport(
        @Query("MaHocKyNamHoc") String maHK,
        @Query("MaMonHoc") String maMon
    );

    @GET("api/reports/bao-cao-hoc-ky")
    Call<List<Map<String, Object>>> getTermReport(@Query("MaHocKyNamHoc") String maHK);

    // XIII. Thay đổi quy định
    @GET("api/configs/danh-sach-tham-so")
    Call<Map<String, Object>> getSystemParameters();

    @POST("api/configs/cap-nhat-tham-so")
    Call<Map<String, Object>> updateSystemParameters(@Body Map<String, Object> parameters);
}
