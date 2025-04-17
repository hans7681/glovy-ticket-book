package com.backend.backend.runner;

import com.backend.backend.service.tmdb.DataPopulationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 应用启动时数据初始化
 * 只在开发环境（dev/test）下启用
 */
@Slf4j
@Component
@Profile({"dev", "test"}) // 只在开发和测试环境下启用
public class DataInitializationRunner implements CommandLineRunner {

    @Autowired
    private DataPopulationService dataPopulationService;
    
    @Value("${app.data.init-on-startup:false}")
    private boolean initOnStartup;
    
    @Value("${app.data.max-movies-per-category:5}")
    private int maxMoviesPerCategory;

    @Override
    public void run(String... args) {
        if (!initOnStartup) {
            log.info("自动数据初始化已禁用，跳过 TMDB 数据导入");
            return;
        }
        
        log.info("==== 开始应用启动时数据初始化 ====");
        
        try {
            // 填充电影类型数据
            dataPopulationService.populateMovieTypes();
            
            // 如果需要自动填充电影数据，则调用
            if (maxMoviesPerCategory > 0) {
                log.info("开始从 TMDB 导入电影数据 (每类最多 {} 部)...", maxMoviesPerCategory);
                int importedMovies = dataPopulationService.populateMoviesFromTmdb(maxMoviesPerCategory);
                log.info("成功从 TMDB 导入 {} 部电影", importedMovies);
            }
        } catch (Exception e) {
            log.error("数据初始化过程中出错", e);
        }
        
        log.info("==== 数据初始化完成 ====");
    }
} 