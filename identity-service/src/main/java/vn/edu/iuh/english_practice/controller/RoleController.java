package vn.edu.iuh.english_practice.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.english_practice.dto.request.RoleRequest;
import vn.edu.iuh.english_practice.dto.response.ApiResponse;
import vn.edu.iuh.english_practice.dto.response.RoleResponse;
import vn.edu.iuh.english_practice.service.RoleService;

import java.util.List;

@RestController
@RequestMapping("/role")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class RoleController {
    RoleService roleService;
    @PostMapping
    public ResponseEntity<RoleResponse> create(@RequestBody RoleRequest request){
        log.info("role controller");
        return  ResponseEntity.status(HttpStatus.OK).body(roleService.create(request));
    }
    @GetMapping
    public ApiResponse<List<RoleResponse>> getAllPermission(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ApiResponse.<List<RoleResponse>>builder()
                .code(200)
                .success(true)
                .message("list role of user "+ authentication.getName())
                .result(roleService.getAllRole(authentication.getName()))
                .build();
    }
}
