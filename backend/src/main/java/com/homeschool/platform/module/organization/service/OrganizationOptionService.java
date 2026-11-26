package com.homeschool.platform.module.organization.service;

import com.homeschool.platform.module.organization.mapper.OrganizationQueryMapper;
import com.homeschool.platform.module.organization.vo.EntityOptionVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationOptionService {

    private final OrganizationQueryMapper queryMapper;

    public OrganizationOptionService(OrganizationQueryMapper queryMapper) {
        this.queryMapper = queryMapper;
    }

    public List<EntityOptionVO> teachers() {
        return queryMapper.selectTeacherOptions();
    }

    public List<EntityOptionVO> students() {
        return queryMapper.selectStudentOptions();
    }

    public List<EntityOptionVO> parents() {
        return queryMapper.selectParentOptions();
    }

    public List<EntityOptionVO> classes() {
        return queryMapper.selectClassOptions();
    }

    public List<EntityOptionVO> subjects() {
        return queryMapper.selectSubjectOptions();
    }
}

