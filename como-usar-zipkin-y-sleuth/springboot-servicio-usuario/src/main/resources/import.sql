INSERT INTO usuarios (username, password, enabled, nombre, apellido, email) VALUES ('andres', '$2a$10$MxA/vIved59UVWIrK8uPLOSiQTVihMYtauJQLOQcCBqmVnIhmiP8G', TRUE, 'Andres', 'Guzman', 'profesor@gmail.com');
INSERT INTO usuarios (username, password, enabled, nombre, apellido, email) VALUES ('admin', '$2a$10$WmlSjISts9kZvfCGGiGY8OaXpHf6KKM20PCAuuwmac3Ef08XjUIlq', TRUE, 'John', 'Doe', 'jhon.doe@gmail.com');


INSERT INTO roles (nombre) VALUES ('ROLE_USER');
INSERT INTO roles (nombre) VALUES ('ROLE_ADMIN');


INSERT INTO usuarios_to_roles (role_id, user_id) VALUES (1, 1);
INSERT INTO usuarios_to_roles (role_id, user_id) VALUES (2, 2);
INSERT INTO usuarios_to_roles (role_id, user_id) VALUES (1, 2);