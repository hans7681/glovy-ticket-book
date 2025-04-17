package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "影院合作入驻申请请求体")
public class CinemaPartnershipApplicationRequestDTO {

    @NotBlank(message = "影院名称不能为空")
    @Size(max = 100, message = "影院名称不能超过100个字符")
    @Schema(description = "影院名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "未来影城")
    private String name;

    @NotBlank(message = "影院地址不能为空")
    @Size(max = 255, message = "影院地址不能超过255个字符")
    @Schema(description = "影院地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "上海市浦东新区世纪大道100号")
    private String address;

    @Size(max = 50, message = "联系电话不能超过50个字符")
    @Schema(description = "联系电话", example = "021-88888888")
    private String phone;

    @Size(max = 255, message = "Logo URL不能超过255个字符")
    @Schema(description = "Logo 图片 URL", example = "https://example.com/logos/future-cinema.png")
    private String logo;

    @Schema(description = "影院介绍")
    private String description;
} 