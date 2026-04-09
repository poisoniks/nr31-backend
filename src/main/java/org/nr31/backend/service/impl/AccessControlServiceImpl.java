package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.stream.Collectors;

import org.nr31.backend.dto.PermissionDTO;
import org.nr31.backend.dto.PermissionUpdateRequest;
import org.nr31.backend.dto.RoleDTO;
import org.nr31.backend.dto.RoleRequest;
import org.nr31.backend.dto.UserDTO;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.Permission;
import org.nr31.backend.model.Role;
import org.nr31.backend.model.User;
import org.nr31.backend.repository.PermissionRepository;
import org.nr31.backend.repository.RoleRepository;
import org.nr31.backend.repository.UserRepository;
import org.nr31.backend.service.AccessControlService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class AccessControlServiceImpl implements AccessControlService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

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
    public Page<RoleDTO> getAllRoles(Pageable pageable) {
        return roleRepository.findAll(pageable)
                .map(this::convertToDTO);
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
        role.setLocalizedName(request.getLocalizedName());
        return convertToDTO(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleDTO updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Role not found with id: " + id));
        role.setName(request.getName());
        role.setLocalizedName(request.getLocalizedName());
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

    @Override
    @Transactional
    public void assignRoleToUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ElementNotFoundException("User not found with id: " + userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ElementNotFoundException("Role not found with id: " + roleId));

        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unassignRoleFromUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ElementNotFoundException("User not found with id: " + userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ElementNotFoundException("Role not found with id: " + roleId));

        user.getRoles().remove(role);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unassignPermissionFromRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ElementNotFoundException("Role not found with id: " + roleId));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ElementNotFoundException("Permission not found with id: " + permissionId));

        role.getPermissions().remove(permission);
        roleRepository.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PermissionDTO> getAllPermissions(Pageable pageable) {
        return permissionRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional
    public PermissionDTO updatePermission(Long id, PermissionUpdateRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Permission not found with id: " + id));
        permission.setDescription(request.getDescription());
        return convertToDTO(permissionRepository.save(permission));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::convertToUserDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTO> searchUsersByUsername(String username, Pageable pageable) {
        return userRepository.findByUsernameContainingIgnoreCase(username, pageable)
                .map(this::convertToUserDTO);
    }

    private RoleDTO convertToDTO(Role role) {
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .localizedName(role.getLocalizedName())
                .permissions(role.getPermissions().stream().filter(Objects::nonNull).map(this::convertToDTO).toList())
                .build();
    }

    private PermissionDTO convertToDTO(Permission permission) {
        return PermissionDTO.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }

    private UserDTO convertToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toSet()))
                .build();
    }
}
