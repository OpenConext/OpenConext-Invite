ALTER TABLE `roles` add `identifier` varchar(255) DEFAULT NULL;
ALTER TABLE `roles` add `applications` json DEFAULT NULL;