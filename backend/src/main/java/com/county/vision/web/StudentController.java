package com.county.vision.web;

import com.county.vision.common.ApiResponse;
import com.county.vision.dto.StudentDtos;
import com.county.vision.service.QueryService;
import com.county.vision.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Tag(name = "学生名单与健康档案")
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final QueryService queryService;

    public StudentController(StudentService studentService, QueryService queryService) {
        this.studentService = studentService;
        this.queryService = queryService;
    }

    @Operation(summary = "学生名单查询（按学校/年级/片区/状态/关键字；仅看导入错误）")
    @GetMapping
    public ApiResponse<List<StudentDtos.StudentListItem>> search(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "false") boolean onlyImportError) {
        return ApiResponse.ok(studentService.search(schoolId, grade, regionId, stage, keyword, onlyImportError));
    }

    @Operation(summary = "学生完整健康管理档案（时间线+全流程记录）")
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(queryService.detail(id));
    }

    @Operation(summary = "学生健康管理时间线")
    @GetMapping("/{id}/timeline")
    public ApiResponse<List<Map<String, Object>>> timeline(@PathVariable Long id) {
        return ApiResponse.ok(queryService.timeline(id));
    }

    @Operation(summary = "手工新增/编辑学生")
    @PostMapping
    public ApiResponse<Long> save(@RequestBody StudentDtos.StudentSaveRequest req) {
        return ApiResponse.ok(studentService.saveStudent(req).getId());
    }

    @Operation(summary = "下载导入模板（CSV，UTF-8 带 BOM，Excel 直接打开）")
    @GetMapping("/import-template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=student-import-template.csv");
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        PrintWriter writer = new PrintWriter(response.getOutputStream(), false, StandardCharsets.UTF_8);
        writer.println("年级,班级,学号,姓名,性别,出生日期,家长姓名,家长电话,既往视力档案");
        writer.println("三年级,2班,20230201,示例学生,女,2014-05-12,张父,13800000000,2025年筛查裸眼视力4.9");
        writer.flush();
    }

    @Operation(summary = "批量导入学生名单（.xlsx 或 .csv），自动校验并登记导入错误")
    @PostMapping("/import")
    public ApiResponse<StudentDtos.ImportResult> importStudents(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long schoolId) {
        return ApiResponse.ok(studentService.importStudents(file, schoolId));
    }

    @Operation(summary = "修正批量导入错误信息并闭环异常事项")
    @PostMapping("/fix-import")
    public ApiResponse<Void> fixImport(@RequestBody com.county.vision.dto.RecordDtos.FixImportRequest req) {
        studentService.fixImportError(req.getStudentId(), req);
        return ApiResponse.ok();
    }
}
