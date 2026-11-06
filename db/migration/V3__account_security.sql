-- Run once against the dedicated myblog database BEFORE starting the upgraded services.
-- Existing accounts keep their passwords; a successful MD5 login upgrades the stored hash.
ALTER TABLE `tb_user`
  MODIFY COLUMN `password` varchar(255) NOT NULL COMMENT 'Versioned salted password hash',
  ADD COLUMN `role` varchar(16) NOT NULL DEFAULT 'USER' COMMENT 'USER or ADMIN';

-- Administrator bootstrap (operator action; never part of public registration):
-- 1. Register your own account using /page/register (password at least 8 characters).
-- 2. Verify the exact account: SELECT id, username, role FROM tb_user WHERE username = 'your_username';
-- 3. Promote that account only:
--    UPDATE tb_user SET role = 'ADMIN', updated = NOW() WHERE username = 'your_username' AND role = 'USER';
-- 4. Sign in and open manager-web. Role changes are checked against MySQL on every token lookup.
-- To revoke access: UPDATE tb_user SET role = 'USER', updated = NOW() WHERE username = 'your_username';
