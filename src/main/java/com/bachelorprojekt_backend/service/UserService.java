package com.bachelorprojekt_backend.service;
import com.bachelorprojekt_backend.model.User;
import com.bachelorprojekt_backend.repository.UserRepository;
import java.util.Collections;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //TODO: Eventuelt returnér DTO i stedet
    public List<User> getAllByTenantId(UUID tenantId){return Collections.emptyList();}
    //TODO: Muligvis bare returnér string i stedet for UUID
    public UUID getTenantIdByUser(String username) {return null;}
}
