"""Deploy script — copies project to server and sets up systemd services."""
import io
import os
import sys
import tarfile
import paramiko

HOST = "37.60.225.35"
USER = "root"
PASSWORD = "irval556"
REMOTE_DIR = "/opt/ee-it-jobs"

SRC_DIR = os.path.dirname(os.path.abspath(__file__))


def ssh_connect():
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASSWORD, timeout=15)
    return client


def run(client, cmd, check=True):
    print(f"  $ {cmd}")
    stdin, stdout, stderr = client.exec_command(cmd, timeout=300)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    code = stdout.channel.recv_exit_status()
    if out.strip():
        # Safe print for Windows
        safe = out.strip().encode("ascii", errors="replace").decode()
        print(f"    {safe}")
    if err.strip() and code != 0:
        safe = err.strip().encode("ascii", errors="replace").decode()
        print(f"    ERR: {safe}")
    if check and code != 0:
        raise RuntimeError(f"Command failed ({code}): {cmd}")
    return out.strip()


def create_tar():
    """Create in-memory tar of project files."""
    buf = io.BytesIO()
    with tarfile.open(fileobj=buf, mode="w:gz") as tar:
        for root, dirs, files in os.walk(os.path.join(SRC_DIR, "src")):
            dirs[:] = [d for d in dirs if d != "__pycache__"]
            for f in files:
                if f.endswith(".pyc"):
                    continue
                full = os.path.join(root, f)
                arcname = os.path.relpath(full, SRC_DIR)
                tar.add(full, arcname=arcname)

        for f in ["pyproject.toml", "config.toml"]:
            p = os.path.join(SRC_DIR, f)
            if os.path.exists(p):
                tar.add(p, arcname=f)

        # Include output data if present
        out_dir = os.path.join(SRC_DIR, "output")
        if os.path.isdir(out_dir):
            for f in os.listdir(out_dir):
                full = os.path.join(out_dir, f)
                if os.path.isfile(full):
                    tar.add(full, arcname=f"output/{f}")

    buf.seek(0)
    return buf


SYSTEMD_CRAWLER = """\
[Unit]
Description=EE IT Jobs Crawler (runs daily)

[Service]
Type=oneshot
WorkingDirectory=/opt/ee-it-jobs
ExecStart=/opt/ee-it-jobs/venv/bin/python -m ee_it_jobs.cli scrape
"""

SYSTEMD_CRAWLER_TIMER = """\
[Unit]
Description=Run EE IT Jobs Crawler daily at 06:00

[Timer]
OnCalendar=*-*-* 06:00:00
Persistent=true

[Install]
WantedBy=timers.target
"""

SYSTEMD_WEB = """\
[Unit]
Description=EE IT Jobs Web UI
After=network.target

[Service]
Type=simple
WorkingDirectory=/opt/ee-it-jobs
ExecStart=/opt/ee-it-jobs/venv/bin/python -m ee_it_jobs.cli web --host 0.0.0.0 --port 80
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
"""


def main():
    print(f"Connecting to {HOST}...")
    client = ssh_connect()

    print("Checking server...")
    run(client, "uname -a")

    print("\nInstalling system dependencies...")
    run(client, "apt-get update -qq", check=False)
    run(client, "apt-get install -y -qq python3 python3-venv python3-pip > /dev/null 2>&1", check=False)

    print(f"\nCreating {REMOTE_DIR}...")
    run(client, f"mkdir -p {REMOTE_DIR}/output")

    print("\nUploading project files...")
    tar_buf = create_tar()
    sftp = client.open_sftp()
    tar_remote = f"{REMOTE_DIR}/project.tar.gz"
    sftp.putfo(tar_buf, tar_remote)
    sftp.close()
    run(client, f"cd {REMOTE_DIR} && tar xzf project.tar.gz && rm project.tar.gz")

    print("\nCreating venv & installing dependencies...")
    run(client, f"python3 -m venv {REMOTE_DIR}/venv")
    run(client, f"{REMOTE_DIR}/venv/bin/pip install --upgrade pip -q")
    run(client, f"cd {REMOTE_DIR} && {REMOTE_DIR}/venv/bin/pip install -e . -q")

    print("\nInstalling Playwright browser...")
    run(client, "apt-get install -y -qq libnss3 libatk1.0-0 libatk-bridge2.0-0 libcups2 libxcomposite1 libxrandr2 libxdamage1 libpango-1.0-0 libcairo2 libasound2t64 libgbm1 > /dev/null 2>&1", check=False)
    run(client, f"{REMOTE_DIR}/venv/bin/playwright install chromium 2>&1 | tail -2", check=False)

    print("\nSetting up systemd services...")

    # Write systemd unit files
    for name, content in [
        ("ee-it-crawler.service", SYSTEMD_CRAWLER),
        ("ee-it-crawler.timer", SYSTEMD_CRAWLER_TIMER),
        ("ee-it-web.service", SYSTEMD_WEB),
    ]:
        sftp = client.open_sftp()
        with sftp.open(f"/etc/systemd/system/{name}", "w") as f:
            f.write(content)
        sftp.close()

    run(client, "systemctl daemon-reload")

    # Run initial crawl
    print("\nRunning initial crawl...")
    run(client, f"cd {REMOTE_DIR} && {REMOTE_DIR}/venv/bin/python -m ee_it_jobs.cli scrape")

    # Start services
    print("\nStarting services...")
    run(client, "systemctl enable --now ee-it-crawler.timer")
    run(client, "systemctl restart ee-it-web.service")
    run(client, "systemctl enable ee-it-web.service")

    print("\nChecking status...")
    run(client, "systemctl status ee-it-web.service --no-pager", check=False)

    client.close()
    print(f"\n{'='*50}")
    print(f"DONE! Veebiliides: http://{HOST}")
    print(f"Crawler jookseb iga paev kell 06:00")
    print(f"{'='*50}")


if __name__ == "__main__":
    main()
