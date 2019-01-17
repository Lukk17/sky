package com.basic.service;

import com.basic.entity.Role;
import com.basic.repository.RoleRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Log4j2
public class RoleService implements IRoleService
{
    @Autowired
    private RoleRepository roleRepository;

    @Override
    public Role findByName(String name)
    {
        return roleRepository.findByName(name);
    }

    @Override
    public List<Role> findAll()
    {
        return roleRepository.findAll();
    }

    @Override
    public Role findById(Long id) { return roleRepository.findById(id).get(); }

    @Override
    public void removeRole(Long id) { roleRepository.deleteById(id); }
}
