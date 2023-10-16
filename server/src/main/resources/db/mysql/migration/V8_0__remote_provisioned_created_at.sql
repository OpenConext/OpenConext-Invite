ALTER TABLE `remote_provisioned_groups`
    add `created_at` datetime DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `remote_provisioned_users`
    add `created_at` datetime DEFAULT CURRENT_TIMESTAMP;
