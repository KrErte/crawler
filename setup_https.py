import paramiko
import sys
import time
import os
from dotenv import load_dotenv

load_dotenv(".env.deploy")

HOST = os.environ.get("DEPLOY_HOST", "37.60.225.35")
USER = os.environ.get("DEPLOY_USER", "root")
PASS = os.environ.get("DEPLOY_PASSWORD")

if not PASS:
    print("ERROR: DEPLOY_PASSWORD environment variable is required. Set it in .env.deploy or export it.")
    sys.exit(1)

def run_cmd(ssh, cmd, timeout=60):
    print(f"\n{'='*60}")
    print(f"RUNNING: {cmd}")
    print('='*60)
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=timeout)
    exit_code = stdout.channel.recv_exit_status()
    out = stdout.read().decode("utf-8", errors="replace").strip()
    err = stderr.read().decode("utf-8", errors="replace").strip()
    if out:
        print(f"STDOUT:\n{out}")
    if err:
        print(f"STDERR:\n{err}")
    print(f"EXIT CODE: {exit_code}")
    return out, err, exit_code

def main():
    print(f"Connecting to {HOST} as {USER}...")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        ssh.connect(HOST, username=USER, password=PASS, timeout=15)
    except Exception as e:
        print(f"ERROR: Failed to connect: {e}")
        sys.exit(1)
    print("Connected successfully.")

    # Step 1: Install certbot
    print("\n" + "#"*60)
    print("# STEP 1: Install certbot and python3-certbot-nginx")
    print("#"*60)
    out, err, rc = run_cmd(ssh, "apt-get update -qq && apt-get install -y certbot python3-certbot-nginx", timeout=120)
    if rc != 0:
        print("WARNING: certbot installation may have had issues.")

    # Step 2: Check hostname / domain
    print("\n" + "#"*60)
    print("# STEP 2: Check hostname and domain")
    print("#"*60)
    hostname_out, _, _ = run_cmd(ssh, "hostname -f")

    print("\nChecking nginx server_name configuration...")
    nginx_domain_out, _, _ = run_cmd(ssh, "grep -r 'server_name' /etc/nginx/sites-enabled/ 2>/dev/null || echo 'No sites-enabled configs found'")

    # Determine if we have a usable domain
    domain = None
    if nginx_domain_out:
        for line in nginx_domain_out.splitlines():
            line = line.strip().rstrip(';')
            if 'server_name' in line:
                parts = line.split()
                for part in parts[1:]:
                    part = part.rstrip(';').strip()
                    if part and part != '_' and part != 'localhost' and not part.replace('.','').isdigit():
                        domain = part
                        break
            if domain:
                break

    if not domain and hostname_out:
        h = hostname_out.strip()
        if h and h != 'localhost' and not h.replace('.','').isdigit() and '.' in h:
            domain = h

    # Step 3: Check current nginx config
    print("\n" + "#"*60)
    print("# STEP 3: Check current nginx configuration")
    print("#"*60)
    run_cmd(ssh, "cat /etc/nginx/sites-enabled/ee-it-jobs 2>/dev/null || echo 'File /etc/nginx/sites-enabled/ee-it-jobs not found'")
    run_cmd(ssh, "nginx -t 2>&1")

    # Step 4: Decide
    print("\n" + "#"*60)
    print("# STEP 4: HTTPS Setup Decision")
    print("#"*60)

    if domain:
        print(f"\nDomain found: {domain}")
        print("Proceeding with Lets Encrypt certificate issuance...")
        certbot_cmd = f"certbot --nginx -d {domain} --non-interactive --agree-tos --email admin@eeitjobs.ee"
        out, err, rc = run_cmd(ssh, certbot_cmd, timeout=120)
        if rc == 0:
            print(f"\nSUCCESS: HTTPS has been set up for {domain}")
            run_cmd(ssh, "nginx -t 2>&1")
            run_cmd(ssh, "systemctl reload nginx")
        else:
            print(f"\nFAILED: certbot returned exit code {rc}")
    else:
        print(f"\nCANNOT SET UP HTTPS - NO DOMAIN NAME FOUND")
        print(f"")
        print(f"  Hostname reported: {hostname_out}")
        print(f"  Nginx server_name: {nginx_domain_out}")
        print(f"")
        print(f"Lets Encrypt does NOT issue SSL certificates for bare IP addresses.")
        print(f"To enable HTTPS, you need to:")
        print(f"  1. Register a domain name (e.g., eeitjobs.ee)")
        print(f"  2. Point its DNS A record to {HOST}")
        print(f"  3. Update the nginx server_name directive to use the domain")
        print(f"  4. Re-run this script or run certbot manually")

    ssh.close()
    print("\nDone. SSH connection closed.")

if __name__ == "__main__":
    main()
