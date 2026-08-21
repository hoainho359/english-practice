package vn.edu.iuh.english_practice.service;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import vn.edu.iuh.english_practice.dto.request.RoleRequest;
import vn.edu.iuh.english_practice.dto.response.RoleResponse;
import vn.edu.iuh.english_practice.entity.Permission;
import vn.edu.iuh.english_practice.entity.Role;
import vn.edu.iuh.english_practice.repository.PermissionRepository;
import vn.edu.iuh.english_practice.repository.RoleRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    public RoleResponse create(RoleRequest request){
         val allByNameIn = permissionRepository.findAllByNameIn(request.getPermissions());
        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissions(new HashSet<>(allByNameIn))
                .build();
        roleRepository.save(role);
        return  RoleResponse.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissions(request.getPermissions())
                .build();
    }
    public List<RoleResponse> getAllRole(String userName) {
        return roleRepository.findAllByUserName(userName)
                .stream()
                .map(role -> RoleResponse.builder()
                        .name(role.getName())
                        .description(role.getDescription())
                        .permissions(
                                role.getPermissions()
                                        .stream()
                                        .map(Permission::getName)
                                        .collect(Collectors.toSet())
                        )
                        .build())
                .toList();
    }
}
