package vn.edu.iuh.english_practice.service;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import vn.edu.iuh.english_practice.dto.request.PermisstionRequest;
import vn.edu.iuh.english_practice.dto.response.PermisstionResponse;
import vn.edu.iuh.english_practice.entity.Permission;
import vn.edu.iuh.english_practice.repository.PermissionRepository;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PermissionService {
    PermissionRepository permissionRepository;

    public PermisstionResponse create(PermisstionRequest request) {
        Permission permission = Permission.builder()
                .name(request.getName())
                .discription(request.getDiscription())
                .build();

        Permission savedPermission = permissionRepository.save(permission);

        permissionRepository.save(permission);

        return PermisstionResponse.builder()
                .name(savedPermission.getName())
                .discription(savedPermission.getDiscription())
                .build();
    }
    public List<PermisstionResponse> getAllPermission(String userName){
        return permissionRepository.findPermissionsByUsername(userName).stream()
                .map(permission -> PermisstionResponse.builder()
                        .name(permission.getName())
                        .discription(permission.getDiscription())
                        .build())
                .toList();

    }
}
