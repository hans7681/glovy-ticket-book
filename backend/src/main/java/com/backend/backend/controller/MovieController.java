package com.backend.backend.controller;

import com.backend.backend.entity.Movie;
import com.backend.backend.service.MovieService;
import com.backend.backend.service.ScreeningService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import com.backend.backend.dto.RatingRequestDTO;
import com.backend.backend.entity.Rating;
import com.backend.backend.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.HashMap;
import java.util.Map;
import com.backend.backend.service.FavoriteService;
import com.backend.backend.service.RatingService;
import jakarta.validation.Valid;
import com.backend.backend.util.SecurityUtil;

@Tag(name = "电影信息管理", description = "包含管理员操作和用户查看电影信息的接口")
@RestController
@RequestMapping("/api") // 基础路径
public class MovieController {

    private static final Logger log = LoggerFactory.getLogger(MovieController.class);

    @Autowired
    private MovieService movieService;

    @Autowired
    private ScreeningService screeningService; // 注入 ScreeningService

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private RatingService ratingService;

    // --- 管理员接口 (/api/admin/movies) ---
    @Tag(name = "管理员 - 电影信息管理") // 嵌套 Tag，将管理员接口分组
    @Operation(
        summary = "添加新电影", 
        description = "添加电影基本信息并关联电影类型。一部电影至少需要关联一个类型，最多可关联三个类型。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功添加电影", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Movie.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"title\": \"功夫\",\n" +
                           "  \"director\": \"周星驰\",\n" +
                           "  \"actors\": \"周星驰,元秋,元华,黄圣依\",\n" +
                           "  \"duration\": 95,\n" +
                           "  \"description\": \"1940年代的上海，小混混阿星（周星驰 饰）...\",\n" +
                           "  \"posterUrl\": \"https://example.com/posters/kungfu-hustle.jpg\",\n" +
                           "  \"releaseDate\": \"2004-12-23\",\n" +
                           "  \"country\": \"中国香港\",\n" +
                           "  \"status\": \"NOW_PLAYING\",\n" +
                           "  \"movieTypes\": [\n" +
                           "    { \"id\": 1, \"name\": \"喜剧\" },\n" +
                           "    { \"id\": 2, \"name\": \"动作\" }\n" +
                           "  ],\n" +
                           "  \"createTime\": \"2023-05-01T12:30:45\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误，例如电影类型数量不符合要求或缺少必要信息",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"电影至少需要关联一个类型\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限")
    })
    @PostMapping("/admin/movies")
    public ResponseEntity<?> addMovie(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "电影信息，必须包含电影基本信息和类型ID列表",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Movie.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"title\": \"功夫\",\n" +
                               "  \"director\": \"周星驰\",\n" +
                               "  \"actors\": \"周星驰,元秋,元华,黄圣依\",\n" +
                               "  \"duration\": 95,\n" +
                               "  \"description\": \"1940年代的上海，小混混阿星（周星驰 饰）...\",\n" +
                               "  \"posterUrl\": \"https://example.com/posters/kungfu-hustle.jpg\",\n" +
                               "  \"releaseDate\": \"2004-12-23\",\n" +
                               "  \"country\": \"中国香港\",\n" +
                               "  \"status\": \"NOW_PLAYING\",\n" +
                               "  \"movieTypeIds\": [1, 2]\n" +
                               "}"
                    )
                )
            )
            @RequestBody Movie movie) {
        // 参数校验：至少需要一个类型
        if (movie.getMovieTypeIds() == null || movie.getMovieTypeIds().isEmpty()) {
            return ResponseEntity.badRequest().body("电影至少需要关联一个类型");
        }
        try {
            boolean success = movieService.saveMovieWithTypes(movie);
            return success ? ResponseEntity.ok(movie) : ResponseEntity.badRequest().body("添加电影失败");
        } catch (RuntimeException e) {
             return ResponseEntity.badRequest().body(e.getMessage()); // 返回具体错误信息，如类型数量超限
        }
    }

    @Tag(name = "管理员 - 电影信息管理")
    @Operation(
        summary = "更新电影信息", 
        description = "根据ID更新电影信息并处理类型关联。一部电影至少需要关联一个类型，最多可关联三个类型。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功更新电影", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Movie.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误，例如电影类型数量不符合要求",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"电影最多关联3个类型\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "电影不存在")
    })
    @PutMapping("/admin/movies/{id}")
    public ResponseEntity<?> updateMovie(
            @Parameter(
                description = "要更新的电影 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "更新后的电影信息，必须包含电影类型ID列表",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Movie.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"title\": \"功夫 (重映版)\",\n" +
                               "  \"director\": \"周星驰\",\n" +
                               "  \"actors\": \"周星驰,元秋,元华,黄圣依\",\n" +
                               "  \"duration\": 95,\n" +
                               "  \"description\": \"1940年代的上海，小混混阿星（周星驰 饰）想要成为黑社会...\",\n" +
                               "  \"posterUrl\": \"https://example.com/posters/kungfu-hustle-remastered.jpg\",\n" +
                               "  \"releaseDate\": \"2023-10-01\",\n" +
                               "  \"country\": \"中国香港\",\n" +
                               "  \"status\": \"NOW_PLAYING\",\n" +
                               "  \"movieTypeIds\": [1, 2, 3]\n" +
                               "}"
                    )
                )
            )
            @RequestBody Movie movie) {
        movie.setId(id);
        // 参数校验：至少需要一个类型
        if (movie.getMovieTypeIds() == null || movie.getMovieTypeIds().isEmpty()) {
            return ResponseEntity.badRequest().body("电影至少需要关联一个类型");
        }
         try {
            boolean success = movieService.updateMovieWithTypes(movie);
            return success ? ResponseEntity.ok(movie) : ResponseEntity.badRequest().body("更新电影失败");
        } catch (RuntimeException e) {
             return ResponseEntity.badRequest().body(e.getMessage()); // 返回具体错误信息，如类型数量超限
        }
    }

    @Tag(name = "管理员 - 电影信息管理")
    @Operation(
        summary = "删除电影", 
        description = "根据ID删除电影及其关联信息。如果电影有关联的排片信息，可能无法删除。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(
            responseCode = "400", 
            description = "操作失败，例如电影存在关联的场次数据",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"该电影存在关联的场次数据，无法删除\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "电影不存在")
    })
    @DeleteMapping("/admin/movies/{id}")
    public ResponseEntity<Void> deleteMovie(
            @Parameter(
                description = "要删除的电影 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id) {
        boolean success = movieService.deleteMovieWithRelations(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // 管理员也可以调用用户的查询接口，或者可以单独为管理员提供更详细的查询接口

    // --- 用户接口 (/api/movies) ---
    @Tag(name = "用户 - 电影浏览") // 用户接口分组
    @Operation(
        summary = "获取电影列表 (分页)", 
        description = "获取公开的电影列表，支持按标题和状态筛选。返回分页结果，包含电影基本信息和关联类型。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取电影列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"records\": [\n" +
                           "    {\n" +
                           "      \"id\": 1,\n" +
                           "      \"title\": \"功夫\",\n" +
                           "      \"director\": \"周星驰\",\n" +
                           "      \"actors\": \"周星驰,元秋,元华,黄圣依\",\n" +
                           "      \"duration\": 95,\n" +
                           "      \"posterUrl\": \"https://example.com/posters/kungfu-hustle.jpg\",\n" +
                           "      \"releaseDate\": \"2004-12-23\",\n" +
                           "      \"status\": \"NOW_PLAYING\",\n" +
                           "      \"movieTypes\": [\n" +
                           "        { \"id\": 1, \"name\": \"喜剧\" },\n" +
                           "        { \"id\": 2, \"name\": \"动作\" }\n" +
                           "      ]\n" +
                           "    }\n" +
                           "  ],\n" +
                           "  \"total\": 1,\n" +
                           "  \"size\": 10,\n" +
                           "  \"current\": 1,\n" +
                           "  \"pages\": 1\n" +
                           "}"
                )
            )
        )
    })
    @GetMapping("/movies")
    public ResponseEntity<Page<Movie>> listMovies(
            @Parameter(
                description = "当前页码", 
                example = "1", 
                schema = @Schema(type = "integer", minimum = "1"), 
                in = ParameterIn.QUERY
            ) 
            @RequestParam(defaultValue = "1") int current,
            
            @Parameter(
                description = "每页条数", 
                example = "10", 
                schema = @Schema(type = "integer", minimum = "1", maximum = "100"), 
                in = ParameterIn.QUERY
            ) 
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(
                description = "按电影标题模糊搜索", 
                example = "功夫", 
                in = ParameterIn.QUERY
            ) 
            @RequestParam(required = false) String title,
            
            @Parameter(
                description = "按电影状态筛选", 
                example = "NOW_PLAYING", 
                schema = @Schema(type = "string", allowableValues = {"COMING_SOON", "NOW_PLAYING", "OFFLINE"}), 
                in = ParameterIn.QUERY
            ) 
            @RequestParam(required = false) String status,

            @Parameter(
                description = "按电影类型 ID 筛选",
                example = "1", // Assuming 1 represents a genre like '喜剧'
                schema = @Schema(type = "integer", format = "int32"),
                in = ParameterIn.QUERY
            )
            @RequestParam(required = false) Integer movieTypeId
    ) {
        Page<Movie> page = new Page<>(current, size);
        Page<Movie> resultPage = movieService.listMoviesWithTypes(page, title, status, movieTypeId);
        return ResponseEntity.ok(resultPage);
    }

    @Tag(name = "用户 - 电影浏览")
    @Operation(
        summary = "获取电影详情",
        description = "根据ID获取单个电影的详细信息，包含所有基本信息和关联类型。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功获取电影详情",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Movie.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"title\": \"功夫\",\n" +
                           "  \"director\": \"周星驰\",\n" +
                           "  \"actors\": \"周星驰,元秋,元华,黄圣依\",\n" +
                           "  \"duration\": 95,\n" +
                           "  \"description\": \"1940年代的上海，小混混阿星（周星驰 饰）想要成为黑社会...\",\n" +
                           "  \"posterUrl\": \"https://example.com/posters/kungfu-hustle.jpg\",\n" +
                           "  \"releaseDate\": \"2004-12-23\",\n" +
                           "  \"country\": \"中国香港\",\n" +
                           "  \"status\": \"NOW_PLAYING\",\n" +
                           "  \"trailerUrl\": \"https://example.com/trailers/kungfu-hustle.mp4\",\n" +
                           "  \"tmdbId\": 9470,\n" +
                           "  \"tmdbVoteAverage\": 7.7,\n" +
                           "  \"tmdbVoteCount\": 2345,\n" +
                           "  \"tmdbPopularity\": 42.5,\n" +
                           "  \"totalBoxOffice\": 17000000,\n" +
                           "  \"averageRating\": 4.8,\n" +
                           "  \"createTime\": \"2023-05-01T12:30:45\",\n" +
                           "  \"updateTime\": \"2023-05-01T12:30:45\",\n" +
                           "  \"movieTypes\": [\n" +
                           "    { \"id\": 1, \"name\": \"喜剧\" },\n" +
                           "    { \"id\": 2, \"name\": \"动作\" }\n" +
                           "  ]\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "电影不存在")
    })
    @GetMapping("/movies/{id}")
    public ResponseEntity<Movie> getMovieById(
            @Parameter(
                description = "电影 ID",
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            )
            @PathVariable Long id) {
        Movie movie = movieService.getMovieWithTypesById(id);
        return movie != null ? ResponseEntity.ok(movie) : ResponseEntity.notFound().build();
    }

    @Tag(name = "用户 - 电影浏览") // 或放在 ScreeningController 更合适?
    @Operation(
        summary = "获取电影最早有效排期日期",
        description = "根据电影ID返回该电影未来最早有有效排期（状态为 APPROVED 且 start_time 在当前时间之后）的日期。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功找到最早排期日期",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", format = "date", example = "2025-08-15")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "未找到该电影未来的有效排期"
        )
    })
    @GetMapping("/movies/{movieId}/first-screening-date")
    public ResponseEntity<String> getFirstAvailableScreeningDate(
        @Parameter(
            description = "电影 ID",
            required = true,
            example = "1",
            schema = @Schema(type = "integer", format = "int64")
        )
        @PathVariable Long movieId) {
        
        String dateString = screeningService.findFirstAvailableScreeningDate(movieId);
        
        return dateString != null ? ResponseEntity.ok(dateString) : ResponseEntity.notFound().build();
    }

    // --- 电影排行榜接口 --- 
    @Tag(name = "用户 - 电影浏览")
    @Operation(
        summary = "获取电影排行榜",
        description = "获取热映口碑榜或最受期待榜。type 参数指定榜单类型 ('hot' 或 'upcoming')，limit 参数指定返回数量 (默认 10)。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功获取排行榜",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "array", implementation = Movie.class)
                // 示例省略，因为会根据 type 不同而不同
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "请求参数错误 (无效的 type)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"无效的排行榜类型: invalid_type\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        )
    })
    @GetMapping("/movies/rankings")
    public ResponseEntity<?> getMovieRankings(
            @Parameter(
                description = "排行榜类型: 'hot' (热映口碑榜) 或 'upcoming' (最受期待榜)",
                required = true,
                example = "hot",
                schema = @Schema(type = "string", allowableValues = {"hot", "upcoming"}),
                in = ParameterIn.QUERY
            )
            @RequestParam String type,

            @Parameter(
                description = "返回的电影数量",
                required = false,
                example = "10",
                schema = @Schema(type = "integer", format = "int32", defaultValue = "10"),
                in = ParameterIn.QUERY
            )
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Movie> movies;
        if ("hot".equalsIgnoreCase(type)) {
            movies = movieService.getHotPlayingMoviesRankedByRating(limit);
        } else if ("upcoming".equalsIgnoreCase(type)) {
            movies = movieService.getUpcomingMoviesRankedByPopularity(limit);
        } else {
            return ResponseEntity.badRequest().body("无效的排行榜类型: " + type);
        }
        return ResponseEntity.ok(movies);
    }

    // --- 电影收藏接口 ---
    @Tag(name = "用户 - 收藏与评分")
    @Operation(summary = "收藏电影", description = "将指定电影添加到当前用户的收藏列表。需要用户认证。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "收藏成功"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "404", description = "电影不存在"),
        @ApiResponse(responseCode = "409", description = "已收藏") // 可选
    })
    @PostMapping("/movies/{movieId}/favorite")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addFavorite(
            @Parameter(description = "电影 ID", required = true) @PathVariable Long movieId
    ) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录或无法识别");
        }
        try {
             boolean executed = favoriteService.addFavorite(currentUserId, movieId);
             return ResponseEntity.ok().body("操作成功");
        } catch (ResourceNotFoundException e) {
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
             log.error("Error adding favorite for user {} movie {}: {}", currentUserId, movieId, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("收藏失败，请稍后重试");
        }
    }

    @Tag(name = "用户 - 收藏与评分")
    @Operation(summary = "取消收藏电影", description = "将指定电影从当前用户的收藏列表中移除。需要用户认证。")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "取消收藏成功"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "404", description = "未找到收藏记录")
    })
    @DeleteMapping("/movies/{movieId}/favorite")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFavorite(
            @Parameter(description = "电影 ID", required = true) @PathVariable Long movieId
    ) {
         Long currentUserId = SecurityUtil.getCurrentUserId();
         if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean removed = favoriteService.removeFavorite(currentUserId, movieId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Tag(name = "用户 - 收藏与评分")
    @Operation(summary = "获取电影收藏状态", description = "检查当前用户是否收藏了指定电影。需要用户认证。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "返回收藏状态", content = @Content(mediaType = "application/json", schema = @Schema(type="object", example = "{\"isFavorited\": true}"))), // Corrected example syntax
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/movies/{movieId}/favorite/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> getFavoriteStatus(
            @Parameter(description = "电影 ID", required = true) @PathVariable Long movieId
    ) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
         if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
         }
        boolean isFavorited = favoriteService.isFavorite(currentUserId, movieId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isFavorited", isFavorited);
        return ResponseEntity.ok(response);
    }

    @Tag(name = "用户 - 收藏与评分")
    @Operation(summary = "获取我的收藏列表", description = "分页获取当前用户收藏的电影列表。需要用户认证。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功获取收藏列表", content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/profile/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Movie>> getMyFavorites(
            @Parameter(description = "当前页码", example = "1", schema = @Schema(type = "integer", minimum = "1"), in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页条数", example = "10", schema = @Schema(type = "integer", minimum = "1", maximum = "100"), in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "10") int size
    ) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Page<Movie> page = new Page<>(current, size);
        Page<Movie> favoriteMovies = favoriteService.getFavoriteMovies(page, currentUserId);
        return ResponseEntity.ok(favoriteMovies);
    }

    // --- 电影评分接口 ---
    @Tag(name = "用户 - 收藏与评分")
    @Operation(summary = "添加或更新电影评分/评论", description = "用户对电影进行评分(1-10)和评论。如果已评分则更新。需要用户认证。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "评分成功", content = @Content(schema = @Schema(implementation = Rating.class))),
        @ApiResponse(responseCode = "400", description = "请求参数无效 (如评分超出范围)"),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "404", description = "电影不存在")
    })
    @PostMapping("/movies/{movieId}/rate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> rateMovie(
            @Parameter(description = "电影 ID", required = true) @PathVariable Long movieId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "评分信息", required = true, content = @Content(schema = @Schema(implementation = RatingRequestDTO.class)))
            @Valid @RequestBody RatingRequestDTO ratingRequest
    ) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
         if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录或无法识别");
        }
        try {
            Rating savedRating = ratingService.addOrUpdateRating(currentUserId, movieId, ratingRequest);
            return ResponseEntity.ok(savedRating);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (jakarta.validation.ConstraintViolationException e) {
             String errorMessages = e.getConstraintViolations().stream()
                 .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                 .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                 .orElse("参数校验失败");
            log.warn("Validation failed for rating request: {}", errorMessages);
            return ResponseEntity.badRequest().body(errorMessages);
        } catch (Exception e) {
            log.error("Error rating movie {} by user {}: {}", movieId, currentUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("评分失败，请稍后重试");
        }
    }

    @Tag(name = "用户 - 收藏与评分")
    @Operation(summary = "获取我的评分", description = "获取当前用户对指定电影的评分信息。需要用户认证。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功获取评分信息", content = @Content(schema = @Schema(implementation = Rating.class))),
        @ApiResponse(responseCode = "401", description = "未授权"),
        @ApiResponse(responseCode = "404", description = "用户尚未对该电影评分")
    })
    @GetMapping("/movies/{movieId}/rating/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Rating> getMyRating(
            @Parameter(description = "电影 ID", required = true) @PathVariable Long movieId
    ) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Rating myRating = ratingService.getMyRatingForMovie(currentUserId, movieId);
        return myRating != null ? ResponseEntity.ok(myRating) : ResponseEntity.notFound().build();
    }

    @Tag(name = "用户 - 电影浏览") // 这个接口通常是公开的
    @Operation(summary = "获取电影的评分列表", description = "分页获取指定电影的所有评分和评论列表 (包含用户信息)。")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功获取评分列表", content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "404", description = "电影不存在") // 可选，如果 Service 层做了检查
    })
    @GetMapping("/movies/{movieId}/ratings")
    public ResponseEntity<Page<Rating>> getMovieRatings(
            @Parameter(description = "电影 ID", required = true) @PathVariable Long movieId,
            @Parameter(description = "当前页码", example = "1", schema = @Schema(type = "integer", minimum = "1"), in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页条数", example = "10", schema = @Schema(type = "integer", minimum = "1", maximum = "100"), in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Rating> page = new Page<>(current, size);
        Page<Rating> ratings = ratingService.getRatingsForMovie(page, movieId);
        return ResponseEntity.ok(ratings);
    }
} 