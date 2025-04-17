package com.backend.backend.controller;

import com.backend.backend.service.tmdb.DataPopulationService;
import com.backend.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员数据管理控制器
 * 提供数据填充、导入等管理功能
 */
@Slf4j
@Tag(name = "系统管理员 - 数据管理", description = "提供数据填充和导入功能")
@RestController
@RequestMapping("/api/admin/data")
public class AdminDataController {

    @Autowired
    private DataPopulationService dataPopulationService;

    /**
     * 从TMDB导入电影数据
     * @param maxMoviesPerCategory 每类最多导入的电影数量，默认为10
     * @return 导入结果
     */
    @Operation(summary = "从TMDB导入电影数据", description = "从TMDB API导入电影、类型和演职员数据 (需要系统管理员权限)")
    @PostMapping("/populate-tmdb")
    public ResponseEntity<?> populateDataFromTmdb(
            @Parameter(description = "每个类别（正在上映/即将上映/流行）最多导入的电影数量")
            @RequestParam(defaultValue = "10") int maxMoviesPerCategory) {
        
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
        }
        
        try {
            log.info("开始从TMDB导入电影数据，请求用户ID: {}, 每类最多: {} 部", currentUserId, maxMoviesPerCategory);
            
            int importedMovies = dataPopulationService.populateMoviesFromTmdb(maxMoviesPerCategory);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("importedMovies", importedMovies);
            result.put("message", "成功从TMDB导入了 " + importedMovies + " 部电影及相关数据");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("从TMDB导入数据时出错", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("导入数据时出错: " + e.getMessage());
        }
    }
} 