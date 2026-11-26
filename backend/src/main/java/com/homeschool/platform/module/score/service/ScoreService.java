package com.homeschool.platform.module.score.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.homeschool.platform.common.constant.RoleCodes;
import com.homeschool.platform.common.exception.BusinessException;
import com.homeschool.platform.module.assignment.service.AssignmentAccessService;
import com.homeschool.platform.module.assignment.service.AssignmentOptionService;
import com.homeschool.platform.module.organization.entity.SchoolClass;
import com.homeschool.platform.module.organization.entity.Student;
import com.homeschool.platform.module.organization.entity.StudentClass;
import com.homeschool.platform.module.organization.entity.Subject;
import com.homeschool.platform.module.organization.mapper.SchoolClassMapper;
import com.homeschool.platform.module.organization.mapper.StudentClassMapper;
import com.homeschool.platform.module.organization.mapper.StudentMapper;
import com.homeschool.platform.module.organization.mapper.SubjectMapper;
import com.homeschool.platform.module.score.dto.ScoreBatchRequest;
import com.homeschool.platform.module.score.dto.ScoreRecordRequest;
import com.homeschool.platform.module.score.entity.Examination;
import com.homeschool.platform.module.score.mapper.ExaminationMapper;
import com.homeschool.platform.module.score.mapper.ScoreMapper;
import com.homeschool.platform.module.score.mapper.ScoreQueryMapper;
import com.homeschool.platform.module.score.vo.ExamOptionVO;
import com.homeschool.platform.module.score.vo.ScoreEntryRowVO;
import com.homeschool.platform.module.score.vo.ScoreReportVO;
import com.homeschool.platform.module.score.vo.ScoreStatsVO;
import com.homeschool.platform.module.score.vo.ScoreWorkspaceOptionsVO;
import com.homeschool.platform.module.user.entity.SysUser;
import com.homeschool.platform.module.user.mapper.SysUserMapper;
import com.homeschool.platform.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ScoreService {

    private final ExaminationMapper examMapper;
    private final ScoreMapper scoreMapper;
    private final ScoreQueryMapper queryMapper;
    private final SubjectMapper subjectMapper;
    private final SchoolClassMapper schoolClassMapper;
    private final StudentClassMapper studentClassMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper userMapper;
    private final AssignmentAccessService accessService;
    private final AssignmentOptionService optionService;

    public ScoreService(
            ExaminationMapper examMapper,
            ScoreMapper scoreMapper,
            ScoreQueryMapper queryMapper,
            SubjectMapper subjectMapper,
            SchoolClassMapper schoolClassMapper,
            StudentClassMapper studentClassMapper,
            StudentMapper studentMapper,
            SysUserMapper userMapper,
            AssignmentAccessService accessService,
            AssignmentOptionService optionService) {
        this.examMapper = examMapper;
        this.scoreMapper = scoreMapper;
        this.queryMapper = queryMapper;
        this.subjectMapper = subjectMapper;
        this.schoolClassMapper = schoolClassMapper;
        this.studentClassMapper = studentClassMapper;
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
        this.accessService = accessService;
        this.optionService = optionService;
    }

    public ScoreWorkspaceOptionsVO workspaceOptions(UserPrincipal principal) {
        boolean admin = principal.getRoles().contains(RoleCodes.ADMIN);
        var assignmentOptions = optionService.options(principal);
        Set<Long> classIds = new HashSet<>();
        Set<Long> gradeIds = new HashSet<>();
        assignmentOptions.teachings().forEach(item -> classIds.add(item.classId()));
        for (Long classId : classIds) {
            SchoolClass schoolClass = schoolClassMapper.selectById(classId);
            if (schoolClass != null) {
                gradeIds.add(schoolClass.getGradeId());
            }
        }
        List<ExamOptionVO> exams = queryMapper.selectPublishedExamOptions().stream()
                .filter(exam -> admin
                        || (exam.classId() != null && classIds.contains(exam.classId()))
                        || (exam.classId() == null && gradeIds.contains(exam.gradeId())))
                .toList();
        return new ScoreWorkspaceOptionsVO(exams, assignmentOptions.teachings());
    }

    public List<ScoreEntryRowVO> entryRows(
            Long examId,
            Long classId,
            Long subjectId,
            UserPrincipal principal) {
        validateEntryPermission(examId, classId, subjectId, principal);
        return queryMapper.selectEntryRows(examId, classId, subjectId);
    }

    @Transactional
    public void batchSave(ScoreBatchRequest request, UserPrincipal principal) {
        validateEntryPermission(
                request.examId(),
                request.classId(),
                request.subjectId(),
                principal);
        Subject subject = subjectMapper.selectById(request.subjectId());
        if (subject == null || !"ENABLED".equals(subject.getStatus())) {
            throw new BusinessException(404, "学科不存在或已停用");
        }
        Set<Long> currentStudentIds = new HashSet<>();
        studentClassMapper.selectList(
                        Wrappers.<StudentClass>lambdaQuery()
                                .eq(StudentClass::getClassId, request.classId())
                                .eq(StudentClass::getStatus, "CURRENT"))
                .forEach(item -> currentStudentIds.add(item.getStudentId()));

        Set<Long> requestStudentIds = new HashSet<>();
        for (ScoreRecordRequest record : request.records()) {
            if (!requestStudentIds.add(record.studentId())) {
                throw new BusinessException("同一学生不能重复提交多条成绩");
            }
            if (!currentStudentIds.contains(record.studentId())) {
                throw new BusinessException("成绩学生不属于当前班级");
            }
            boolean absent = Boolean.TRUE.equals(record.absent());
            if (absent && record.score() != null) {
                throw new BusinessException("缺考学生不能填写分数");
            }
            if (!absent && record.score() == null) {
                throw new BusinessException("未缺考学生必须填写分数");
            }
            if (!absent
                    && (record.score().compareTo(BigDecimal.ZERO) < 0
                    || record.score().compareTo(subject.getDefaultFullScore()) > 0)) {
                throw new BusinessException(
                        record.studentId() + " 的成绩超出 0 到 "
                                + subject.getDefaultFullScore() + " 的范围");
            }
            scoreMapper.upsert(
                    request.examId(),
                    record.studentId(),
                    request.subjectId(),
                    request.classId(),
                    absent ? null : record.score(),
                    absent ? 1 : 0,
                    StringUtils.hasText(record.remark())
                            ? record.remark().trim()
                            : null,
                    principal.getUserId());
        }
    }

    public ScoreStatsVO statistics(
            Long examId,
            Long classId,
            Long subjectId,
            UserPrincipal principal) {
        validateEntryPermission(examId, classId, subjectId, principal);
        ScoreStatsVO result = queryMapper.selectClassStatistics(examId, classId, subjectId);
        if (result == null) {
            result = new ScoreStatsVO();
        }
        return result;
    }

    public ScoreReportVO report(Long studentId, UserPrincipal principal) {
        Long targetStudentId = accessService.resolveReadStudentId(principal, studentId);
        Student student = studentMapper.selectById(targetStudentId);
        if (student == null) {
            throw new BusinessException(404, "学生不存在");
        }
        SysUser user = userMapper.selectById(student.getUserId());
        return new ScoreReportVO(
                student.getId(),
                user == null ? student.getStudentNo() : user.getRealName(),
                student.getStudentNo(),
                queryMapper.selectStudentScoreItems(student.getId()),
                queryMapper.selectStudentTrends(student.getId()));
    }

    private void validateEntryPermission(
            Long examId,
            Long classId,
            Long subjectId,
            UserPrincipal principal) {
        Examination exam = examMapper.selectById(examId);
        if (exam == null || !"PUBLISHED".equals(exam.getStatus())) {
            throw new BusinessException(404, "考试不存在或未发布");
        }
        SchoolClass schoolClass = schoolClassMapper.selectById(classId);
        if (schoolClass == null || !schoolClass.getGradeId().equals(exam.getGradeId())) {
            throw new BusinessException("成绩班级与考试年级不匹配");
        }
        if (exam.getClassId() != null && !exam.getClassId().equals(classId)) {
            throw new BusinessException(403, "该考试不包含当前班级");
        }
        if (principal.getRoles().contains(RoleCodes.ADMIN)) {
            return;
        }
        var teacher = accessService.requireTeacher(principal);
        accessService.requireTeaching(teacher.getId(), classId, subjectId);
    }
}

