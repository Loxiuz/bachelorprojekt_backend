
INSERT INTO roles (id, role_name) VALUES (1,'ROLE_ADMIN'),(2,'ROLE_USER');

INSERT INTO tenants(id, tenant_name) VALUES (uuid_in('24917aa9-db47-45aa-ba24-44f0ee2f6eca'), 'Test Tenant 1'),
                                            (uuid_in('74bc7cc2-c403-4ffb-bad9-ca92950001ff'), 'Test Tenant 2');

INSERT INTO users (id,username, password, role_id, tenant_id) VALUES
    (uuid_in('cf42006d-eb56-42d8-a959-c171af9be80e'),'admin','$2a$12$UjaMRw0tknm8BeXbD8MVp.g42jcWFKYy3L0dQdds3BIf4zlnR2Dvm', 1, uuid_in('24917aa9-db47-45aa-ba24-44f0ee2f6eca')),
    (uuid_in('8b5e291c-0e68-4038-81f2-b82963608d62'),'malte', '$2a$12$VxaQgWyJdReC.tJvFQC79OQM710shzYgjKNQ0iS16J3qMMKf6n8F.', 2, uuid_in('24917aa9-db47-45aa-ba24-44f0ee2f6eca'));

-- Muligvis nødvendig i frontend
INSERT INTO permissions (id, permission_name) VALUES
    (1, 'MANAGE_USERS');

INSERT INTO roles_permissions(role_id, permission_id) VALUES
(1, 1)