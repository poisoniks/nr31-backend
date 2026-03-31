package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.RoleDTO;
import org.nr31.backend.dto.RoleRequest;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.Permission;
import org.nr31.backend.model.Role;
import org.nr31.backend.repository.PermissionRepository;
import org.nr31.backend.repository.RoleRepository;
import org.nr31.backend.service.AccessControlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccessControlServiceImpl implements AccessControlService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ElementNotFoundException("Role not found with id: " + roleId));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ElementNotFoundException("Permission not found with id: " + permissionId));

        role.getPermissions().add(permission);
        roleRepository.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDTO getRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Role not found with id: " + id));
        return convertToDTO(role);
    }

    @Override
    @Transactional
    public RoleDTO createRole(RoleRequest request) {
        Role role = new Role();
        role.setName(request.getName());
        return convertToDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleDTO updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Role not found with id: " + id));
        role.setName(request.getName());
        return convertToDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ElementNotFoundException("Role not found with id: " + id);
        }
        roleRepository.deleteById(id);
    }

    private RoleDTO convertToDTO(Role role) {
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .build();
    }
}
