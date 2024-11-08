package org.userservice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.userservice.model.User;

public interface TestH2Repository extends JpaRepository<User, Long> {
    
}
