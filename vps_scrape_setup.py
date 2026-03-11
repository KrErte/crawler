import paramiko
import json
import time
import os
import sys
from dotenv import load_dotenv

load_dotenv(".env.deploy")

VPS_HOST = os.environ.get("DEPLOY_HOST", "37.60.225.35")
VPS_USER = os.environ.get("DEPLOY_USER", "root")
VPS_PASS = os.environ.get("DEPLOY_PASSWORD")

if not VPS_PASS:
    print("ERROR: DEPLOY_PASSWORD environment variable is required. Set it in .env.deploy or export it.")
    sys.exit(1)

SEP = "=" * 70

def run_cmd(ssh, cmd, description=""):
    if description:
        print()
        print(SEP)
        print("  " + description)
        print(SEP)
    print("[CMD] " + cmd)
    stdin, stdout, stderr = ssh.exec_command(cmd, timeout=120)
    out = stdout.read().decode("utf-8", errors="replace").strip()
    err = stderr.read().decode("utf-8", errors="replace").strip()
    if out:
        print("[STDOUT]")
        print(out)
    if err:
        print("[STDERR]")
        print(err)
    return out, err

def main():
    print("Connecting to " + VPS_HOST + " as " + VPS_USER + "...")
    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(VPS_HOST, port=22, username=VPS_USER, password=VPS_PASS, timeout=30)
    print("Connected successfully.")

    admin_email = os.environ.get("ADMIN_EMAIL", "admin@eeitjobs.ee")
    admin_password = os.environ.get("ADMIN_PASSWORD", "Admin2024!")

    try:
        # Step 1: Register admin user
        reg_payload = json.dumps({"email": admin_email, "password": admin_password, "firstName": "Admin", "lastName": "User"})
        reg_cmd = "curl -s -X POST http://localhost:8080/api/auth/register -H 'Content-Type: application/json' -d '" + reg_payload + "'"
        out, err = run_cmd(ssh, reg_cmd, "STEP 1: Register admin user via API")

        # Step 2: Update user to admin in PostgreSQL
        psql_cmd = "sudo -u postgres psql -d ee_it_jobs -c " + chr(34) + "UPDATE users SET is_admin = true WHERE email = " + chr(39) + admin_email + chr(39) + ";" + chr(34)
        out, err = run_cmd(ssh, psql_cmd, "STEP 2: Set user as admin in PostgreSQL")

        # Step 3: Login to get JWT token
        login_payload = json.dumps({"email": admin_email, "password": admin_password})
        login_cmd = "curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '" + login_payload + "'"
        out, err = run_cmd(ssh, login_cmd, "STEP 3: Login to get JWT token")

        token = None
        try:
            resp = json.loads(out)
            token = resp.get("accessToken") or resp.get("token")
            if token:
                print("[TOKEN] Extracted: " + token[:40] + "..." + token[-20:])
            else:
                print("[WARNING] token field not found. Keys: " + str(list(resp.keys())))
        except Exception as e:
            print("[ERROR] Failed to parse JSON: " + str(e))

        if not token:
            print("[FATAL] Cannot proceed without token.")
            return

        # Step 4: Trigger scrape
        scrape_cmd = "curl -s -X POST http://localhost:8080/api/scrape/trigger -H 'Authorization: Bearer " + token + "'"
        out, err = run_cmd(ssh, scrape_cmd, "STEP 4: Trigger scrape")

        # Step 5: Wait 30s, check status
        print()
        print(SEP)
        print("  Waiting 30 seconds before checking scrape status...")
        print(SEP)
        time.sleep(30)
        out, err = run_cmd(ssh, "curl -s http://localhost:8080/api/scrape/status", "STEP 5: Check scrape status (after 30s)")

        # Step 6: Wait 60s, check status + jobs
        print()
        print(SEP)
        print("  Waiting 60 seconds before final checks...")
        print(SEP)
        time.sleep(60)
        out, err = run_cmd(ssh, "curl -s http://localhost:8080/api/scrape/status", "STEP 6a: Check scrape status (after 90s total)")
        out, err = run_cmd(ssh, "curl -s 'http://localhost:8080/api/jobs?page=0&size=1' | head -c 200", "STEP 6b: Check jobs API (first 200 chars)")
        out, err = run_cmd(ssh, 'sudo -u postgres psql -d ee_it_jobs -c "SELECT count(*) FROM jobs;"', "STEP 6c: Count jobs in database")

        # Step 7: Check logs
        out, err = run_cmd(ssh, "journalctl -u ee-it-jobs.service --since '5 minutes ago' --no-pager | grep -iE 'error|scrape|completed' | tail -30", "STEP 7: Check logs for scraper activity/errors")

    finally:
        ssh.close()
        print()
        print(SEP)
        print("  SSH connection closed. All steps completed.")
        print(SEP)

if __name__ == "__main__":
    main()
