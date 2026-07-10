-- KART 21.5: Eksik şablon eklentisi
INSERT INTO notification_templates (id, code, channel, locale, subject, body_template)
VALUES
(gen_random_uuid(), 'INVOICE_OVERDUE_SMS', 'SMS', 'tr', NULL,
 'Sayin {customerName}, {dueDate} tarihinde vadesi gelen {amount} TL tutarindaki faturaniz hala odenmemistir. Lutfen en kisa surede odeme yapiniz.');
