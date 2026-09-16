CREATE DATABASE IF NOT EXISTS auth_service;
CREATE DATABASE IF NOT EXISTS product_service;
CREATE DATABASE IF NOT EXISTS order_service;
CREATE DATABASE IF NOT EXISTS notification_service;

GRANT ALL PRIVILEGES ON auth_service.* TO 'ecom'@'%';
GRANT ALL PRIVILEGES ON product_service.* TO 'ecom'@'%';
GRANT ALL PRIVILEGES ON order_service.* TO 'ecom'@'%';
GRANT ALL PRIVILEGES ON notification_service.* TO 'ecom'@'%';
GRANT SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'ecom'@'%';
