package com.bachelorprojekt_backend.repository;

import com.bachelorprojekt_backend.model.Permission;
import com.bachelorprojekt_backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID>{

    @Query("""
        SELECT p
        FROM Role r
        JOIN r.permissions p
        WHERE r.id = :roleId
    """)
    Set<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);

    Role findRoleByRoleName(String roleName);
}
