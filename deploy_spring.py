"""Deploy script — builds and deploys Spring Boot + Angular to VPS."""
import io
import os
import sys
import tarfile
import paramiko
from dotenv import load_dotenv

load_dotenv(".env.deploy")

HOST = os.environ.get("DEPLOY_HOST", "37.60.225.35")
USER = os.environ.get("DEPLOY_USER", "root")
PASSWORD = os.environ.get("DEPLOY_PASSWORD")
REMOTE_DIR = os.environ.get("DEPLOY_REMOTE_DIR", "/opt/ee-it-jobs")
DB_NAME = os.environ.get("DEPLOY_DB_NAME", "ee_it_jobs")
DB_USER = os.environ.get("DEPLOY_DB_USER", "eitjobs")
DB_PASS = os.environ.get("DEPLOY_DB_PASS")

if not PASSWORD:
    sys.exit("ERROR: DEPLOY_PASSWORD environment variable is required. Set it in .env.deploy or export it.")
if not DB_PASS:
    sys.exit("ERROR: DEPLOY_DB_PASS environment variable is required. Set it in .env.deploy or export it.")

JWT_SECRET = os.environ.get("DEPLOY_JWT_SECRET", "CHANGE-ME-generate-a-64-char-random-secret")

SRC_DIR = os.path.dirname(os.path.abspath(__file__))
BACKEND_DIR = os.path.join(SRC_DIR, "ee-it-jobs-backend")
FRONTEND_DIR = os.path.join(SRC_DIR, "ee-it-jobs-frontend")


def ssh_connect():
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASSWORD, timeout=15)
    return client


def run(client, cmd, check=True, timeout=600):
    print(f"  $ {cmd}")
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    code = stdout.channel.recv_exit_status()
    if out.strip():
        lines = out.strip().split("\n")
        for line in lines[-10:]:
            safe = line.encode("ascii", errors="replace").decode()
            print(f"    {safe}")
    if err.strip() and code != 0:
        lines = err.strip().split("\n")
        for line in lines[-10:]:
            safe = line.encode("ascii", errors="replace").decode()
            print(f"    ERR: {safe}")
    if check and code != 0:
        raise RuntimeError(f"Command failed ({code}): {cmd}")
    return out.strip()


def write_remote_file(client, path, content):
    sftp = client.open_sftp()
    with sftp.open(path, "w") as f:
        f.write(content)
    sftp.close()


def build_frontend():
    """Build Angular frontend locally."""
    print("\n=== Building Angular frontend ===")
    os.system(f'cd "{FRONTEND_DIR}" && npx ng build --configuration=production')

    dist_dir = os.path.join(FRONTEND_DIR, "dist", "ee-it-jobs-frontend", "browser")
    if not os.path.isdir(dist_dir):
        # Try alternative dist path
        dist_dir = os.path.join(FRONTEND_DIR, "dist", "ee-it-jobs-frontend")
    if not os.path.isdir(dist_dir):
        raise RuntimeError(f"Angular build output not found at {dist_dir}")
    return dist_dir


def copy_frontend_to_backend(dist_dir):
    """Copy Angular dist to Spring Boot static resources."""
    import shutil
    static_dir = os.path.join(BACKEND_DIR, "src", "main", "resources", "static")
    if os.path.isdir(static_dir):
        shutil.rmtree(static_dir)
    shutil.copytree(dist_dir, static_dir)
    print(f"  Copied Angular dist to {static_dir}")


def create_tar():
    """Create tar of backend project."""
    buf = io.BytesIO()
    with tarfile.open(fileobj=buf, mode="w:gz") as tar:
        for root, dirs, files in os.walk(BACKEND_DIR):
            dirs[:] = [d for d in dirs if d not in ("target", ".mvn", "node_modules", ".idea")]
            for f in files:
                full = os.path.join(root, f)
                arcname = os.path.relpath(full, SRC_DIR)
                tar.add(full, arcname=arcname)
    buf.seek(0)
    return buf


APPLICATION_YML_PROD = f"""\
spring:
  application:
    name: ee-it-jobs

  datasource:
    url: jdbc:postgresql://localhost:5432/{DB_NAME}
    username: {DB_USER}
    password: {DB_PASS}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    locations: classpath:db/migration

  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

  jackson:
    serialization:
      write-dates-as-timestamps: false

server:
  port: 8080
  forward-headers-strategy: native

app:
  jwt:
    secret: "{JWT_SECRET}"
    expiration-ms: 86400000
    refresh-expiration-ms: 604800000

  cors:
    allowed-origins: "http://{HOST},https://{HOST},http://{HOST}:80,http://{HOST}:8080"

  base-url: http://{HOST}
  mail-from: noreply@eeitjobs.ee

  scraper:
    rate-limit: 2.0
    max-concurrency: 4
    request-timeout-seconds: 30
    user-agent: "EE-IT-Jobs-Crawler/1.0"
    cron: "0 0 8 * * MON-FRI"

  storage:
    cv-dir: "/opt/ee-it-jobs/uploads/cv"

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html

logging:
  level:
    ee.itjobs: INFO
"""

NGINX_CONF = f"""\
server {{
    listen 80;
    server_name {HOST};

    client_max_body_size 10M;

    location / {{
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }}
}}
"""

SYSTEMD_SERVICE = """\
[Unit]
Description=EE IT Jobs Spring Boot Application
After=network.target postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/ee-it-jobs
ExecStart=/usr/bin/java -jar /opt/ee-it-jobs/app.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
Environment=JAVA_OPTS=-Xmx512m

[Install]
WantedBy=multi-user.target
"""

MAVEN_WRAPPER_PROPS = """\
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.tar.gz
"""


def main():
    # Step 1: Build Angular
    dist_dir = build_frontend()
    copy_frontend_to_backend(dist_dir)

    # Step 2: Create tar of backend
    print("\n=== Packaging backend ===")
    tar_buf = create_tar()
    print(f"  Archive size: {tar_buf.getbuffer().nbytes / 1024:.0f} KB")

    # Step 3: Connect to server
    print(f"\n=== Connecting to {HOST} ===")
    client = ssh_connect()
    run(client, "uname -a")

    # Step 4: Install system deps
    print("\n=== Installing system dependencies ===")
    run(client, "apt-get update -qq", check=False)

    # Install Java 17
    print("\nInstalling Java 17...")
    run(client, "apt-get install -y -qq openjdk-17-jdk-headless > /dev/null 2>&1", check=False)
    java_check = run(client, "java -version 2>&1 | head -1", check=False)
    if "17" not in java_check and "21" not in java_check:
        # Try alternative
        run(client, "apt-get install -y -qq default-jdk > /dev/null 2>&1", check=False)
    run(client, "java -version 2>&1 | head -1", check=False)

    # Install Maven
    print("\nInstalling Maven...")
    run(client, "apt-get install -y -qq maven > /dev/null 2>&1", check=False)
    run(client, "mvn --version 2>&1 | head -1", check=False)

    # Install PostgreSQL
    print("\nInstalling PostgreSQL...")
    run(client, "apt-get install -y -qq postgresql postgresql-contrib > /dev/null 2>&1", check=False)
    run(client, "systemctl enable --now postgresql", check=False)

    # Install nginx
    print("\nInstalling nginx...")
    run(client, "apt-get install -y -qq nginx > /dev/null 2>&1", check=False)

    # Step 5: Setup PostgreSQL database
    print("\n=== Setting up PostgreSQL ===")
    pg_cmds = [
        f"sudo -u postgres psql -tc \"SELECT 1 FROM pg_roles WHERE rolname='{DB_USER}'\" | grep -q 1 || sudo -u postgres createuser {DB_USER}",
        f"sudo -u postgres psql -c \"ALTER USER {DB_USER} WITH PASSWORD '{DB_PASS}';\"",
        f"sudo -u postgres psql -tc \"SELECT 1 FROM pg_database WHERE datname='{DB_NAME}'\" | grep -q 1 || sudo -u postgres createdb -O {DB_USER} {DB_NAME}",
        f"sudo -u postgres psql -c \"GRANT ALL PRIVILEGES ON DATABASE {DB_NAME} TO {DB_USER};\"",
        f"sudo -u postgres psql -d {DB_NAME} -c \"GRANT ALL ON SCHEMA public TO {DB_USER};\"",
    ]
    for cmd in pg_cmds:
        run(client, cmd, check=False)

    # Step 6: Upload project
    print("\n=== Uploading project ===")
    run(client, f"mkdir -p {REMOTE_DIR}/uploads/cv")
    sftp = client.open_sftp()
    sftp.putfo(tar_buf, f"{REMOTE_DIR}/project.tar.gz")
    sftp.close()
    run(client, f"cd {REMOTE_DIR} && tar xzf project.tar.gz && rm project.tar.gz")

    # Step 7: Write production application.yml
    print("\n=== Writing production config ===")
    write_remote_file(client,
        f"{REMOTE_DIR}/ee-it-jobs-backend/src/main/resources/application-prod.yml",
        APPLICATION_YML_PROD)

    # Step 8: Build JAR
    print("\n=== Building Spring Boot JAR (this takes a few minutes) ===")
    run(client,
        f"cd {REMOTE_DIR}/ee-it-jobs-backend && mvn clean package -DskipTests -q 2>&1 | tail -5",
        timeout=600)

    # Copy JAR
    run(client,
        f"cp {REMOTE_DIR}/ee-it-jobs-backend/target/*.jar {REMOTE_DIR}/app.jar")

    # Step 9: Setup nginx (only if config doesn't exist yet)
    print("\n=== Configuring nginx ===")
    existing = run(client, "test -f /etc/nginx/sites-available/ee-it-jobs && echo exists || echo missing", check=False)
    if "missing" in existing:
        write_remote_file(client, "/etc/nginx/sites-available/ee-it-jobs", NGINX_CONF)
        print("  Wrote new nginx config")
    else:
        print("  Preserving existing nginx config (SSL)")
    run(client, "ln -sf /etc/nginx/sites-available/ee-it-jobs /etc/nginx/sites-enabled/ee-it-jobs", check=False)
    run(client, "rm -f /etc/nginx/sites-enabled/default /etc/nginx/sites-enabled/dev-doraaudit", check=False)
    run(client, "nginx -t && systemctl reload nginx", check=False)

    # Step 10: Setup systemd service
    print("\n=== Setting up systemd service ===")
    write_remote_file(client, "/etc/systemd/system/ee-it-jobs.service", SYSTEMD_SERVICE)
    run(client, "systemctl daemon-reload")

    # Stop old Python services if running
    run(client, "systemctl stop ee-it-web.service 2>/dev/null", check=False)
    run(client, "systemctl disable ee-it-web.service 2>/dev/null", check=False)

    # Start Spring Boot
    run(client, "systemctl restart ee-it-jobs.service")
    run(client, "systemctl enable ee-it-jobs.service")

    # Wait for startup
    import time
    print("\n  Waiting for application to start...")
    time.sleep(10)

    # Step 11: Verify
    print("\n=== Verifying deployment ===")
    run(client, "systemctl status ee-it-jobs.service --no-pager -l", check=False)
    run(client, f"curl -s -o /dev/null -w '%{{http_code}}' http://localhost:8080/api/scrape/status", check=False)

    client.close()

    print(f"\n{'='*55}")
    print(f"  DEPLOYED! Application: http://{HOST}")
    print(f"  Swagger UI: http://{HOST}/swagger-ui.html")
    print(f"  Scraper runs daily at 08:00 MON-FRI")
    print(f"{'='*55}")


if __name__ == "__main__":
    main()
