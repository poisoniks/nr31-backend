package org.nr31.backend.service;

import org.nr31.backend.dto.PermissionDTO;
import org.nr31.backend.dto.RoleDTO;
import org.nr31.backend.dto.RoleRequest;
import java.util.List;

public interface AccessControlService {
    void assignPermissionToRole(Long roleId, Long permissionId);
    List<RoleDTO> getAllRoles();
    RoleDTO getRole(Long id);
    RoleDTO createRole(RoleRequest request);
    RoleDTO updateRole(Long id, RoleRequest request);
    void deleteRole(Long id);
    void assignRoleToUser(Long userId, Long roleId);
    List<PermissionDTO> getAllPermissions();
}
