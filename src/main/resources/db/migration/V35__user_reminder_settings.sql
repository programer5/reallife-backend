ALTER TABLE users
    ADD COLUMN pin_remind_browser_notify BIT(1) NOT NULL DEFAULT b'0',
    ADD COLUMN pin_remind_sound BIT(1) NOT NULL DEFAULT b'0',
    ADD COLUMN pin_remind_vibrate BIT(1) NOT NULL DEFAULT b'0';
