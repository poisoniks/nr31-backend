package org.nr31.backend.service;

import org.nr31.backend.dto.user.PermissionDTO;
import org.nr31.backend.dto.user.PermissionUpdateRequest;
import org.nr31.backend.dto.user.RoleDTO;
import org.nr31.backend.dto.user.RoleRequest;
import org.nr31.backend.dto.user.UserDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccessControlService {
    void assignPermissionToRole(Long roleId, Long permissionId);

    Page<RoleDTO> getAllRoles(Pageable pageable);

    RoleDTO getRole(Long id);

    RoleDTO createRole(RoleRequest request);

    RoleDTO updateRole(Long id, RoleRequest request);

    void deleteRole(Long id);

    void assignRoleToUser(Long userId, Long roleId);

    void unassignRoleFromUser(Long userId, Long roleId);

    void unassignPermissionFromRole(Long roleId, Long permissionId);

    Page<PermissionDTO> getAllPermissions(Pageable pageable);

    PermissionDTO updatePermission(Long id, PermissionUpdateRequest request);

    Page<UserDTO> getAllUsers(Pageable pageable);

    Page<UserDTO> searchUsersByUsername(String username, Pageable pageable);

    UserDTO getUserByUsername(String username);
}
