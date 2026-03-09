"""Fix port 80 - stop Docker container and claim port."""
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

# Find and stop docker container on port 80
run(client, "docker ps --format '{{.ID}} {{.Ports}} {{.Names}}'")
run(client, "docker ps -q | xargs -r docker stop")
run(client, "systemctl stop docker 2>/dev/null; echo done")

# Restart our web service
run(client, "systemctl restart ee-it-web.service")
run(client, "sleep 2 && systemctl status ee-it-web.service --no-pager -l")

client.close()
print(f"\nCheck: http://{HOST}")
