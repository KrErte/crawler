"""Fix port 80 conflict on server."""
import paramiko

HOST = "37.60.225.35"
USER = "root"
PASSWORD = "irval556"


def run(client, cmd):
    print(f"  $ {cmd}")
    stdin, stdout, stderr = client.exec_command(cmd, timeout=30)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    code = stdout.channel.recv_exit_status()
    if out.strip():
        safe = out.strip().encode("ascii", errors="replace").decode()
        print(f"    {safe}")
    if err.strip():
        safe = err.strip().encode("ascii", errors="replace").decode()
        print(f"    {safe}")
    return out.strip()


client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(HOST, username=USER, password=PASSWORD, timeout=15)

# Check what's on port 80
run(client, "ss -tlnp | grep ':80 '")

# Stop apache/nginx if running
run(client, "systemctl stop apache2 2>/dev/null; systemctl disable apache2 2>/dev/null; echo done")
run(client, "systemctl stop nginx 2>/dev/null; systemctl disable nginx 2>/dev/null; echo done")

# Also kill any leftover process on port 80
run(client, "fuser -k 80/tcp 2>/dev/null; echo done")

# Install missing Playwright deps and restart
run(client, "apt-get install -y -qq libxfixes3 libx11-6 libx11-xcb1 libxcb1 libxext6 libxrender1 libxkbcommon0 libdrm2 > /dev/null 2>&1; echo deps installed")
run(client, "/opt/ee-it-jobs/venv/bin/playwright install-deps chromium 2>&1 | tail -3")

# Restart web service
run(client, "systemctl restart ee-it-web.service")
run(client, "sleep 2 && systemctl status ee-it-web.service --no-pager -l")

client.close()
print(f"\nCheck: http://{HOST}")
