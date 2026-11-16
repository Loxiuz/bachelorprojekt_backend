
INSERT INTO roles (id, role_name) VALUES (1,'ROLE_ADMIN'),(2,'ROLE_USER');

INSERT INTO tenants(id, tenant_name) VALUES (uuid_in('24917aa9-db47-45aa-ba24-44f0ee2f6eca'), 'Test Tenant 1'),
                                            (uuid_in('74bc7cc2-c403-4ffb-bad9-ca92950001ff'), 'Test Tenant 2');

INSERT INTO doors (id, tenant_id, name, location) VALUES
    (uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a111'), uuid_in('24917aa9-db47-45aa-ba24-44f0ee2f6eca'), 'Simulator door 1', 'Simulator'),
    (uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a222'), uuid_in('74bc7cc2-c403-4ffb-bad9-ca92950001ff'), 'Simulator door 2', 'Simulator'),
    (uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a113'), uuid_in('24917aa9-db47-45aa-ba24-44f0ee2f6eca'), 'Main entrance', 'Ground floor'),
    (uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a114'), uuid_in('24917aa9-db47-45aa-ba24-44f0ee2f6eca'), 'Storage room', 'Basement'),
    (uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a223'), uuid_in('74bc7cc2-c403-4ffb-bad9-ca92950001ff'), 'Reception entrance', 'Ground floor'),
    (uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a224'), uuid_in('74bc7cc2-c403-4ffb-bad9-ca92950001ff'), 'Back entrance', 'Loading area');

INSERT INTO devices (id, door_id, name, credential_hash) VALUES
    (uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a111'), uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a111'), 'Simulator device 1', 'a839fdae05426a951f397f45dcdf85fe0493304acafc7d6e4f8ada69dc23f9e3'),
    (uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a222'), uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a222'), 'Simulator device 2', 'd952a1f2807354db6702dd7b77db0cd7f78678908ad9016e7c981e40fa2f898e'),
    (uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a113'), uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a113'), 'Main entrance sensor', 'c34dfb61a85c92afa181698c2a84e676cdad430b0c6bc7f18d1cbba204ba2104'),
    (uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a114'), uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a114'), 'Storage room sensor', '969b4913c5fddcd1ded8af2f28e1fb049f2f3aeb50bb1264f7fe9ee92bd141c1'),
    (uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a223'), uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a223'), 'Reception sensor', 'bc63e59ea5a08d8ff41cca4b431a32a019a091c75d3a86781cd9444130f9eb43'),
    (uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a224'), uuid_in('c06c72ee-56f1-4b6a-9a7a-18b58e36a224'), 'Back entrance sensor', '42363e02d0cc15217d919dbcc5321126daa67462df14f5695707619ddfb96419');

INSERT INTO current_status_readings (id, device_id, read_at, battery_level, locked, door_open, is_online) VALUES
    (uuid_in('e06c72ee-56f1-4b6a-9a7a-18b58e36a113'), uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a113'), '2026-01-02T12:00:00Z', 92, true, false, true),
    (uuid_in('e06c72ee-56f1-4b6a-9a7a-18b58e36a114'), uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a114'), '2026-01-02T12:00:00Z', 18, true, false, true),
    (uuid_in('e06c72ee-56f1-4b6a-9a7a-18b58e36a223'), uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a223'), '2026-01-02T12:00:00Z', 78, false, true, true),
    (uuid_in('e06c72ee-56f1-4b6a-9a7a-18b58e36a224'), uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a224'), '2026-01-02T12:00:00Z', 46, true, false, false);

INSERT INTO status_readings_history (id, device_id, read_at, battery_level, locked, door_open, is_online) VALUES
    (uuid_in('f06c72ee-56f1-4b6a-9a7a-18b58e36a113'), uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a113'), '2026-01-01T12:00:00Z', 95, false, true, true),
    (uuid_in('f06c72ee-56f1-4b6a-9a7a-18b58e36a114'), uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a114'), '2026-01-01T12:00:00Z', 24, true, false, true),
    (uuid_in('f06c72ee-56f1-4b6a-9a7a-18b58e36a223'), uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a223'), '2026-01-01T12:00:00Z', 81, true, false, true),
    (uuid_in('f06c72ee-56f1-4b6a-9a7a-18b58e36a224'), uuid_in('d06c72ee-56f1-4b6a-9a7a-18b58e36a224'), '2026-01-01T12:00:00Z', 49, true, false, true);

INSERT INTO users (id,username, password, role_id, tenant_id) VALUES
    (uuid_in('cf42006d-eb56-42d8-a959-c171af9be80e'),'admin','$2a$12$UjaMRw0tknm8BeXbD8MVp.g42jcWFKYy3L0dQdds3BIf4zlnR2Dvm', 1, uuid_in('24917aa9-db47-45aa-ba24-44f0ee2f6eca')),
    (uuid_in('8b5e291c-0e68-4038-81f2-b82963608d62'),'malte', '$2a$12$VxaQgWyJdReC.tJvFQC79OQM710shzYgjKNQ0iS16J3qMMKf6n8F.', 2, uuid_in('24917aa9-db47-45aa-ba24-44f0ee2f6eca'));

-- Muligvis nødvendig i frontend
INSERT INTO permissions (id, permission_name) VALUES
    (1, 'MANAGE_USERS');

INSERT INTO roles_permissions(role_id, permission_id) VALUES
(1, 1)