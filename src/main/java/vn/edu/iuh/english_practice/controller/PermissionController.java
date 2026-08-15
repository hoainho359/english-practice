package vn.edu.iuh.english_practice.controller;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.english_practice.dto.request.PermisstionRequest;
import vn.edu.iuh.english_practice.dto.response.ApiResponse;
import vn.edu.iuh.english_practice.dto.response.PermisstionResponse;
import vn.edu.iuh.english_practice.service.PermissionService;

import java.util.List;

@RestController
@RequestMapping("/permission")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PermissionController {
    PermissionService permissionService;
    @PostMapping
    public ApiResponse<PermisstionResponse> create(@RequestBody PermisstionRequest request){
        return  ApiResponse.<PermisstionResponse>builder()
                .code(200)
                .success(true)
                .message("create successed")
                .result(permissionService.create(request))
                .build();
    }
    @GetMapping
    public ApiResponse<List<PermisstionResponse>> getAllPermission(){
        val authentication = SecurityContextHolder.getContext().getAuthentication();
        return ApiResponse.<List<PermisstionResponse>>builder()
                .code(200)
                .success(true)
                .message("list permission of user "+authentication.getName())
                .result(permissionService.getAllPermission(authentication.getName()))
                .build();
    }
}
