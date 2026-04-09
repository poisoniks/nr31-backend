ALTER TABLE user_roles DROP CONSTRAINT user_roles_role_id_fkey;
ALTER TABLE user_roles
    ADD CONSTRAINT user_roles_role_id_fkey
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;

ALTER TABLE role_permissions DROP CONSTRAINT role_permissions_role_id_fkey;
ALTER TABLE role_permissions
    ADD CONSTRAINT role_permissions_role_id_fkey
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;

ALTER TABLE events DROP CONSTRAINT IF EXISTS events_type_id_fkey;
ALTER TABLE events
    ADD CONSTRAINT events_type_id_fkey
    FOREIGN KEY (type_id) REFERENCES event_types(id) ON DELETE SET NULL;

ALTER TABLE event_exceptions DROP CONSTRAINT IF EXISTS event_exceptions_new_type_id_fkey;
ALTER TABLE event_exceptions
    ADD CONSTRAINT event_exceptions_new_type_id_fkey
    FOREIGN KEY (new_type_id) REFERENCES event_types(id) ON DELETE SET NULL;
