package com.example.backend.scheduler;

import com.example.backend.controller.DocsTestSupport;
import com.example.backend.domain.message.Conversation;
import com.example.backend.domain.message.ConversationMember;
import com.example.backend.domain.notification.NotificationType;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.repository.message.ConversationMemberRepository;
import com.example.backend.repository.message.ConversationRepository;
import com.example.backend.repository.notification.NotificationRepository;
import com.example.backend.repository.pin.ConversationPinRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConversationPinRemindSchedulerTest {

    @Autowired ConversationPinRemindScheduler scheduler;
    @Autowired DocsTestSupport docs;
    @Autowired ConversationRepository conversationRepository;
    @Autowired ConversationMemberRepository memberRepository;
    @Autowired ConversationPinRepository pinRepository;
    @Autowired NotificationRepository notificationRepository;

    @PersistenceContext
    EntityManager em;

    @Test
    void 예정된_핀은_시간이_되면_PIN_REMIND_알림을_생성한다() {
        var me = docs.saveUser("remindme", "리마인드유저");
        var peer = docs.saveUser("remindpeer", "상대유저");

        Conversation conversation = conversationRepository.saveAndFlush(Conversation.direct());
        memberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), me.getId()));
        memberRepository.saveAndFlush(ConversationMember.join(conversation.getId(), peer.getId()));

        LocalDateTime startAt = LocalDateTime.now().plusMinutes(1);
        ConversationPin pin = ConversationPin.createSchedule(
                conversation.getId(),
                me.getId(),
                null,
                "종합시장 약속",
                "종합시장",
                startAt,
                5
        );
        pinRepository.saveAndFlush(pin);

        scheduler.run();

        var myNotifications = notificationRepository.findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(me.getId());
        var peerNotifications = notificationRepository.findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(peer.getId());

        assertThat(myNotifications)
                .anySatisfy(n -> {
                    assertThat(n.getType()).isEqualTo(NotificationType.PIN_REMIND);
                    assertThat(n.getRefId()).isEqualTo(pin.getId());
                    assertThat(n.getBody()).contains("종합시장 약속");
                    assertThat(n.getBody()).contains("종합시장");
                });

        assertThat(peerNotifications)
                .anySatisfy(n -> {
                    assertThat(n.getType()).isEqualTo(NotificationType.PIN_REMIND);
                    assertThat(n.getRefId()).isEqualTo(pin.getId());
                });

        // claimRemind는 벌크 update라서, 같은 영속성 컨텍스트에 남아 있는 pin 엔티티는 stale 상태일 수 있다.
        em.flush();
        em.clear();

        var refreshed = pinRepository.findById(pin.getId()).orElseThrow();
        assertThat(refreshed.getRemindedAt()).isNotNull();

        scheduler.run();

        assertThat(notificationRepository.findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(me.getId())
                .stream().filter(n -> n.getType() == NotificationType.PIN_REMIND && n.getRefId().equals(pin.getId())).count())
                .isEqualTo(1);
        assertThat(notificationRepository.findTop50ByUserIdAndDeletedFalseOrderByCreatedAtDesc(peer.getId())
                .stream().filter(n -> n.getType() == NotificationType.PIN_REMIND && n.getRefId().equals(pin.getId())).count())
                .isEqualTo(1);
    }
}
