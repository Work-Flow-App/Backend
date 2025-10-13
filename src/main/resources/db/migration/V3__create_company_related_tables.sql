-- V3__create_company_related_tables.sql

-- ----------------------------
-- Table: companies
-- ----------------------------
CREATE TABLE companies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    address_line_3 VARCHAR(255),
    town VARCHAR(100),
    country VARCHAR(100),
    postcode VARCHAR(20),
    telephone VARCHAR(20),
    mobile VARCHAR(20),
    fax VARCHAR(20),
    email VARCHAR(100),
    accounts_email VARCHAR(100),
    accounts_number VARCHAR(50),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY UK_companies_name (name)
) ENGINE=InnoDB;

-- ----------------------------
-- Table: clients
-- ----------------------------
CREATE TABLE clients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    company_id BIGINT NOT NULL,
    email VARCHAR(100),
    telephone VARCHAR(20),
    mobile VARCHAR(20),
    address VARCHAR(255),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT FK_clients_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------
-- Table: company_users
-- ----------------------------
CREATE TABLE company_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    company_role ENUM('ADMIN','MANAGER','STAFF') NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT FK_company_users_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT FK_company_users_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------
-- Table: workers
-- ----------------------------
CREATE TABLE workers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    initials VARCHAR(10),
    company_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,
    telephone VARCHAR(20),
    mobile VARCHAR(20),
    email VARCHAR(100),
    login_locked BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT FK_workers_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT FK_workers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;
