package de.vitagroup.num.attachment.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional("attachmentTransactionManager")
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "num", name = "enableAttachmentDatabase", havingValue = "true")
public class AttachmentService {

}
