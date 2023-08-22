ALTER TABLE `users` add `name` varchar(510) DEFAULT NULL;
UPDATE `users` set `name` = (SELECT CONCAT(given_name, ' ', family_name));
