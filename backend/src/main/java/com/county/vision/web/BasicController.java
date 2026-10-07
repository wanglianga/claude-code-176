package com.county.vision.web;

import com.county.vision.common.ApiResponse;
import com.county.vision.domain.entity.Region;
import com.county.vision.domain.entity.School;
import com.county.vision.domain.repository.GradeClassRepository;
import com.county.vision.domain.repository.RegionRepository;
import com.county.vision.domain.repository.SchoolRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "基础数据（片区/学校/班级）")
@RestController
@RequestMapping("/api/basic")
public class BasicController {

    private final RegionRepository regionRepo;
    private final SchoolRepository schoolRepo;
    private final GradeClassRepository classRepo;

    public BasicController(RegionRepository regionRepo, SchoolRepository schoolRepo,
                           GradeClassRepository classRepo) {
        this.regionRepo = regionRepo;
        this.schoolRepo = schoolRepo;
        this.classRepo = classRepo;
    }

    @GetMapping("/regions")
    @Operation(summary = "片区列表")
    public ApiResponse<List<Region>> regions() {
        return ApiResponse.ok(regionRepo.findAll());
    }

    @GetMapping("/schools")
    @Operation(summary = "学校列表（可按片区过滤）")
    public ApiResponse<List<Map<String, Object>>> schools(@RequestParam(required = false) Long regionId) {
        List<School> list = regionId == null ? schoolRepo.findAllByOrderByIdAsc()
                : schoolRepo.findByRegionId(regionId);
        return ApiResponse.ok(list.stream().map(s -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("regionId", s.getRegion() == null ? null : s.getRegion().getId());
            m.put("regionName", s.getRegion() == null ? null : s.getRegion().getName());
            m.put("address", s.getAddress());
            m.put("contactPhone", s.getContactPhone());
            m.put("healthTeacher", s.getHealthTeacher());
            return m;
        }).toList());
    }

    @GetMapping("/classes")
    @Operation(summary = "某校年级班级列表")
    public ApiResponse<List<Map<String, Object>>> classes(@RequestParam Long schoolId) {
        return ApiResponse.ok(classRepo.findBySchoolIdOrderByGradeAscClassNameAsc(schoolId).stream()
                .map(c -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("schoolId", schoolId);
                    m.put("grade", c.getGrade());
                    m.put("className", c.getClassName());
                    return m;
                }).toList());
    }
}
