package com.homeschool.platform.module.score.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homeschool.platform.common.api.PageResponse;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.organization.entity.Grade;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import com.homeschool.platform.module.organization.entity.Subject;
import com.homeschool.platform.module.organization.entity.TeacherClass;
import com.homeschool.platform.module.organization.mapper.GradeMapper;
import com.homeschool.platform.module.organization.mapper.SchoolClassMapper;
import com.homeschool.platform.module.organization.mapper.SubjectMapper;
import com.homeschool.platform.module.organization.mapper.TeacherClassMapper;
import com.homeschool.platform.module.score.dto.ExamQuery;
import com.homeschool.platform.module.score.dto.ExamRequest;
import com.homeschool.platform.module.score.dto.SubjectRequest;
import com.homeschool.platform.module.score.entity.Examination;
import com.homeschool.platform.module.score.entity.Score;
import com.homeschool.platform.module.score.mapper.ExaminationMapper;
import com.homeschool.platform.module.score.mapper.ScoreMapper;
import com.homeschool.platform.module.score.mapper.ScoreQueryMapper;
import com.homeschool.platform.module.score.vo.ExamVO;
import com.homeschool.platform.module.score.vo.SubjectVO;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AcademicAdminService {

    private final ExaminationMapper examMapper;
    private final ScoreMapper scoreMapper;
    private final ScoreQueryMapper queryMapper;
    private final SubjectMapper subjectMapper;
    private final GradeMapper gradeMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final TeacherClassMapper teacherClassMapper;

    public AcademicAdminService(
            ExaminationMapper examMapper,
            ScoreMapper scoreMapper,
            ScoreQueryMapper queryMapper,
            SubjectMapper subjectMapper,
            GradeMapper gradeMapper,
            SchoolClassMapper schoolClassMapper,
            TeacherClassMapper teacherClassMapper) {
        this.examMapper = examMapper;
        this.scoreMapper = scoreMapper;
        this.queryMapper = queryMapper;
        this.subjectMapper = subjectMapper;
        this.gradeMapper = gradeMapper;
        this.schoolClassMapper = schoolClassMapper;
        this.teacherClassMapper = teacherClassMapper;
    }

    public PageResponse<ExamVO> examPage(ExamQuery query) {
        if (StringUtils.hasText(query.getStatus())
                && !java.util.Set.of("DRAFT", "PUBLISHED", "ARCHIVED")
                .contains(query.getStatus())) {
            throw new BusinessException("考试状态筛选值不正确");
        }
        query.setKeyword(StringUtils.hasText(query.getKeyword())
                ? query.getKeyword().trim()
                : null);
        query.setAcademicYear(StringUtils.hasText(query.getAcademicYear())
                ? query.getAcademicYear().trim()
                : null);
        IPage<ExamVO> result = queryMapper.selectExamPage(
                new Page<>(query.normalizedPage(), query.normalizedSize()),
                query);
        return PageResponse.from(result);
    }

    @Transactional
    public Long createExam(ExamRequest request, UserPrincipal principal) {
        validateExamScope(request.gradeId(), request.classId());
        Examination exam = new Examination();
        exam.setCreatedBy(principal.getUserId());
        applyExam(exam, request);
        examMapper.insert(exam);
        return exam.getId();
    }

    @Transactional
    public void updateExam(Long examId, ExamRequest request) {
        Examination exam = requireExam(examId);
        validateExamScope(request.gradeId(), request.classId());
        applyExam(exam, request);
        examMapper.updateById(exam);
    }

    @Transactional
    public void deleteExam(Long examId) {
        requireExam(examId);
        long scoreCount = scoreMapper.selectCount(
                Wrappers.<Score>lambdaQuery().eq(Score::getExamId, examId));
        if (scoreCount > 0) {
            throw new BusinessException(409, "考试已有成绩，不能删除");
        }
        examMapper.deleteById(examId);
    }

    public PageResponse<SubjectVO> subjectPage(
            String keyword,
            String status,
            long page,
            long size) {
        var wrapper = Wrappers.<Subject>lambdaQuery()
                .like(StringUtils.hasText(keyword), Subject::getSubjectName, keyword)
                .eq(StringUtils.hasText(status), Subject::getStatus, status)
                .orderByAsc(Subject::getSortOrder)
                .orderByAsc(Subject::getId);
        IPage<Subject> source = subjectMapper.selectPage(
                new Page<>(Math.max(1, page), Math.min(100, Math.max(1, size))),
                wrapper);
        Page<SubjectVO> target = new Page<>(
                source.getCurrent(),
                source.getSize(),
                source.getTotal());
        target.setRecords(source.getRecords().stream().map(this::toSubjectVO).toList());
        return PageResponse.from(target);
    }

    @Transactional
    public Long createSubject(SubjectRequest request) {
        Subject subject = new Subject();
        applySubject(subject, request);
        subjectMapper.insert(subject);
        return subject.getId();
    }

    @Transactional
    public void updateSubject(Long subjectId, SubjectRequest request) {
        Subject subject = requireSubject(subjectId);
        applySubject(subject, request);
        subjectMapper.updateById(subject);
    }

    @Transactional
    public void deleteSubject(Long subjectId) {
        requireSubject(subjectId);
        long scoreCount = scoreMapper.selectCount(
                Wrappers.<Score>lambdaQuery().eq(Score::getSubjectId, subjectId));
        long teachingCount = teacherClassMapper.selectCount(
                Wrappers.<TeacherClass>lambdaQuery()
                        .eq(TeacherClass::getSubjectId, subjectId));
        if (scoreCount > 0 || teachingCount > 0) {
            throw new BusinessException(409, "学科已有成绩或教师关联，不能删除");
        }
        subjectMapper.deleteById(subjectId);
    }

    private void validateExamScope(Long gradeId, Long classId) {
        Grade grade = gradeMapper.selectById(gradeId);
        if (grade == null) {
            throw new BusinessException(404, "考试年级不存在");
        }
        if (classId != null) {
            SchoolClass schoolClass = schoolClassMapper.selectById(classId);
            if (schoolClass == null || !schoolClass.getGradeId().equals(gradeId)) {
                throw new BusinessException("考试班级不属于所选年级");
            }
        }
    }

    private void applyExam(Examination exam, ExamRequest request) {
        exam.setName(request.name().trim());
        exam.setExamType(request.examType().trim());
        exam.setGradeId(request.gradeId());
        exam.setClassId(request.classId());
        exam.setAcademicYear(request.academicYear().trim());
        exam.setTerm(request.term().trim());
        exam.setExamDate(request.examDate());
        exam.setStatus(request.status());
    }

    private void applySubject(Subject subject, SubjectRequest request) {
        subject.setSubjectCode(request.subjectCode().trim().toUpperCase());
        subject.setSubjectName(request.subjectName().trim());
        subject.setDefaultFullScore(request.defaultFullScore());
        subject.setSortOrder(request.sortOrder());
        subject.setStatus(StringUtils.hasText(request.status())
                ? request.status()
                : "ENABLED");
    }

    private Examination requireExam(Long examId) {
        Examination exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException(404, "考试不存在");
        }
        return exam;
    }

    private Subject requireSubject(Long subjectId) {
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null) {
            throw new BusinessException(404, "学科不存在");
        }
        return subject;
    }

    private SubjectVO toSubjectVO(Subject subject) {
        return new SubjectVO(
                subject.getId(),
                subject.getSubjectCode(),
                subject.getSubjectName(),
                subject.getDefaultFullScore(),
                subject.getSortOrder(),
                subject.getStatus(),
                subject.getCreatedAt());
    }
}

