package com.backend.backend.controller;

import com.backend.backend.entity.MovieType;
import com.backend.backend.service.MovieTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理员 - 电影类型管理", description = "管理电影类型信息 (需要系统管理员权限)")
@RestController
@RequestMapping("/api/admin/movie-types") // 暂定管理员路径，后续添加权限控制
public class MovieTypeController {

    @Autowired
    private MovieTypeService movieTypeService;

    @Operation(
        summary = "添加新电影类型",
        description = "添加一个新的电影类型，例如 '科幻'、'爱情' 等。类型名称需唯一。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "添加成功", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovieType.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 4,\n" +
                           "  \"name\": \"科幻\",\n" +
                           "  \"tmdbGenreId\": 878\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "添加失败，例如类型名称已存在",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"电影类型名称已存在\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限")
    })
    @PostMapping
    public ResponseEntity<MovieType> addMovieType(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "电影类型信息，必须包含类型名称。tmdbGenreId 为可选，用于关联 TMDB 的类型 ID。",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MovieType.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"name\": \"科幻\",\n" +
                               "  \"tmdbGenreId\": 878\n" +
                               "}"
                    )
                )
            )
            @RequestBody MovieType movieType) {
        // TODO: Service 层应添加名称唯一性校验
        boolean success = movieTypeService.save(movieType);
        return success ? ResponseEntity.ok(movieType) : ResponseEntity.badRequest().build();
    }

    @Operation(
        summary = "获取所有电影类型列表",
        description = "获取系统中定义的所有电影类型。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取类型列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "array", implementation = MovieType.class),
                examples = @ExampleObject(
                    value = "[\n" +
                           "  { \"id\": 1, \"name\": \"喜剧\", \"tmdbGenreId\": 35 },\n" +
                           "  { \"id\": 2, \"name\": \"动作\", \"tmdbGenreId\": 28 },\n" +
                           "  { \"id\": 3, \"name\": \"剧情\", \"tmdbGenreId\": 18 }\n" +
                           "]"
                )
            )
        )
        // 此接口通常不需要 401/403，因为是公共信息，但路径设在 admin 下，保留可能性
    })
    @GetMapping
    public ResponseEntity<List<MovieType>> getAllMovieTypes() {
        List<MovieType> types = movieTypeService.list();
        return ResponseEntity.ok(types);
    }

    @Operation(
        summary = "根据 ID 获取电影类型详情",
        description = "获取指定 ID 的电影类型详细信息。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取类型详情", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovieType.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"name\": \"喜剧\",\n" +
                           "  \"tmdbGenreId\": 35\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "电影类型不存在"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限")
    })
    @GetMapping("/{id}")
    public ResponseEntity<MovieType> getMovieTypeById(
            @Parameter(description = "电影类型 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int32")) 
            @PathVariable Integer id) {
        MovieType type = movieTypeService.getById(id);
        return type != null ? ResponseEntity.ok(type) : ResponseEntity.notFound().build();
    }

    @Operation(
        summary = "根据 ID 更新电影类型信息",
        description = "更新指定 ID 的电影类型信息，例如修改名称或关联的 TMDB ID。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "更新成功", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = MovieType.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "更新失败，例如类型名称已存在",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"电影类型名称已存在\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "电影类型不存在")
    })
    @PutMapping("/{id}")
    public ResponseEntity<MovieType> updateMovieType(
            @Parameter(description = "要更新的电影类型 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int32")) 
            @PathVariable Integer id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "更新后的电影类型信息。ID 字段会被忽略。",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MovieType.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"name\": \"喜剧片\",\n" +
                               "  \"tmdbGenreId\": 35\n" +
                               "}"
                    )
                )
            )
            @RequestBody MovieType movieType) {
        movieType.setId(id); // 确保 ID 一致
        // TODO: Service 层应添加名称唯一性校验
        boolean success = movieTypeService.updateById(movieType);
        return success ? ResponseEntity.ok(movieType) : ResponseEntity.badRequest().build();
    }

    @Operation(
        summary = "根据 ID 删除电影类型",
        description = "删除指定 ID 的电影类型。如果该类型已被电影关联，可能无法删除。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(
            responseCode = "400", 
            description = "删除失败，例如该类型已被电影关联",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"该类型已被电影关联，无法删除\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "电影类型不存在")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovieType(
            @Parameter(description = "要删除的电影类型 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int32")) 
            @PathVariable Integer id) {
        // TODO: Service 层应添加关联检查
        boolean success = movieTypeService.removeById(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}

/**
 * 公共电影类型接口，不需要权限控制
 */
@Tag(name = "电影类型", description = "公共电影类型信息获取")
@RestController
@RequestMapping("/api/movie-types")
class PublicMovieTypeController {

    @Autowired
    private MovieTypeService movieTypeService;

    @Operation(
        summary = "获取所有电影类型列表",
        description = "获取系统中定义的所有电影类型，无需管理员权限。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取类型列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "array", implementation = MovieType.class),
                examples = @ExampleObject(
                    value = "[\n" +
                           "  { \"id\": 1, \"name\": \"喜剧\", \"tmdbGenreId\": 35 },\n" +
                           "  { \"id\": 2, \"name\": \"动作\", \"tmdbGenreId\": 28 },\n" +
                           "  { \"id\": 3, \"name\": \"剧情\", \"tmdbGenreId\": 18 }\n" +
                           "]"
                )
            )
        )
    })
    @GetMapping
    public ResponseEntity<List<MovieType>> getAllMovieTypes() {
        List<MovieType> types = movieTypeService.list();
        return ResponseEntity.ok(types);
    }
} 