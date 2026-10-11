#!/usr/bin/env bash
# Run a shell command on the EC2 instance through AWS SSM and wait for it.
# Usage:  INSTANCE_ID=i-... ssm-run.sh "<command>"
# Env:    SSM_COMMENT (label in SSM history), MAX_WAIT (seconds, default 1200)
set -euo pipefail

CMD=${1:?usage: ssm-run.sh "<command>"}
: "${INSTANCE_ID:?EC2_INSTANCE_ID secret is missing}"
MAX_WAIT=${MAX_WAIT:-1200}

PARAMS=$(jq -nc --arg c "$CMD" '{commands: [$c]}')
CMD_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --comment "${SSM_COMMENT:-github-actions}" \
  --parameters "$PARAMS" \
  --query Command.CommandId --output text)
echo "SSM command: $CMD_ID ($CMD)"

STATUS=Pending
END=$((SECONDS + MAX_WAIT))
while [ "$SECONDS" -lt "$END" ]; do
  sleep 10
  STATUS=$(aws ssm get-command-invocation \
    --command-id "$CMD_ID" --instance-id "$INSTANCE_ID" \
    --query Status --output text 2>/dev/null || echo Pending)
  echo "Status: $STATUS"
  case "$STATUS" in Success|Failed|Cancelled|TimedOut) break ;; esac
done

echo "----- output from VM -----"
aws ssm get-command-invocation \
  --command-id "$CMD_ID" --instance-id "$INSTANCE_ID" \
  --query '[StandardOutputContent,StandardErrorContent]' --output text || true

[ "$STATUS" = "Success" ]
