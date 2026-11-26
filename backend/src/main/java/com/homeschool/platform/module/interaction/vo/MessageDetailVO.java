package com.homeschool.platform.module.interaction.vo;

import java.util.List;

public record MessageDetailVO(
        MessageVO message,
        List<MessageReplyVO> replies) {
}

