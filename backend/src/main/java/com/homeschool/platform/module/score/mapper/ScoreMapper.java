package com.homeschool.platform.module.score.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homeschool.platform.module.score.entity.Score;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface ScoreMapper extends BaseMapper<Score> {

    @Insert("""
            INSERT INTO score
                (exam_id, student_id, subject_id, class_id, score, is_absent,
                 remark, entered_by, updated_by)
            VALUES
                (#{examId}, #{studentId}, #{subjectId}, #{classId}, #{score},
                 #{isAbsent}, #{remark}, #{operatorId}, #{operatorId})
            ON DUPLICATE KEY UPDATE
                class_id = VALUES(class_id),
                score = VALUES(score),
                is_absent = VALUES(is_absent),
                remark = VALUES(remark),
                updated_by = #{operatorId}
            """)
    int upsert(
            @Param("examId") Long examId,
            @Param("studentId") Long studentId,
            @Param("subjectId") Long subjectId,
            @Param("classId") Long classId,
            @Param("score") BigDecimal score,
            @Param("isAbsent") int isAbsent,
            @Param("remark") String remark,
            @Param("operatorId") Long operatorId);
}

