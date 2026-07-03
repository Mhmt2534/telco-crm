INSERT INTO notification_templates (id, code, channel, locale, subject, body_template) VALUES
(gen_random_uuid(), 'WELCOME_SMS', 'SMS', 'tr', NULL, 'Merhaba {customerName}, TelcoX ailesine hos geldiniz! Aboneliginiz basariyla baslatildi.'),
(gen_random_uuid(), 'WELCOME_EMAIL', 'EMAIL', 'tr', 'TelcoX Ailesine Hos Geldiniz!', '<h1>Merhaba {customerName},</h1><p>TelcoX ailesine hos geldiniz. Aboneliginiz basariyla baslatilmistir.</p>'),
(gen_random_uuid(), 'INVOICE_EMAIL', 'EMAIL', 'tr', 'Faturaniz Kesildi', '<p>Sayin {customerName},</p><p>{invoiceMonth} donemi faturaniz kesilmistir. Tutar: {amount} TL. Faturanizi asagidaki linkten indirebilirsiniz:</p><p><a href="{pdfUrl}">{pdfUrl}</a></p>'),
(gen_random_uuid(), 'QUOTA_WARNING_SMS', 'SMS', 'tr', NULL, 'Sayin {customerName}, {usageType} kotanizin %80''ini kullandiniz.'),
(gen_random_uuid(), 'QUOTA_EXCEEDED_SMS', 'SMS', 'tr', NULL, 'Sayin {customerName}, {usageType} kotaniz bitmistir. Kullanimlariniz paket asim tarifesi uzerinden ucretlendirilecektir.');
