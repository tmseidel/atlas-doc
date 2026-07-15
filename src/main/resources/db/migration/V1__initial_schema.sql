-- Complete baseline schema for a new Docs Portal installation.

CREATE TABLE projects (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE repositories (
    id               VARCHAR(36) PRIMARY KEY,
    project_id       VARCHAR(36) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    clone_url        VARCHAR(1024) NOT NULL,
    branch           VARCHAR(255) NOT NULL DEFAULT 'main',
    subdirectories   TEXT,
    auth_token       TEXT,
    ssh_key          TEXT,
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    last_sync_at     TIMESTAMP,
    last_sync_status VARCHAR(20),
    last_sync_error  TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_repositories_project
        FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE mkdocs_config_repo (
    id               VARCHAR(36) PRIMARY KEY,
    project_id       VARCHAR(36) NOT NULL,
    clone_url        VARCHAR(1024) NOT NULL,
    branch           VARCHAR(255) NOT NULL DEFAULT 'main',
    auth_token       VARCHAR(1024),
    last_sync_at     TIMESTAMP,
    last_sync_status VARCHAR(20),
    last_sync_error  TEXT,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mkdocs_config_project
        FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT uk_mkdocs_config_project UNIQUE (project_id)
);

CREATE TABLE build_records (
    id          VARCHAR(36) PRIMARY KEY,
    project_id  VARCHAR(36) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    started_at  TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    log_output  TEXT,
    trigger     VARCHAR(20),
    CONSTRAINT fk_build_records_project
        FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE INDEX idx_repositories_project ON repositories(project_id);
CREATE INDEX idx_build_records_project_started ON build_records(project_id, started_at);

INSERT INTO projects (id, name, description)
VALUES ('00000000-0000-0000-0000-000000000001', 'Default Project', 'Default project created during initialization');
