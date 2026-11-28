#!/usr/bin/env bash
# AureMedia build 计数递增脚本（同版本每次 build +1）
# 用法:
#   bash scripts/bump_build.sh               # 打印下一 buildSeq（不写文件）
#   bash scripts/bump_build.sh --apply       # 递增并写回 app/build.gradle
set -euo pipefail

GRADLE="${AURE_BUILD_GRADLE:-app/build.gradle}"
APPLY=0
while [ $# -gt 0 ]; do
  case "$1" in
    --apply) APPLY=1; shift ;;
    --dry-run) APPLY=0; shift ;;
    *) echo "usage: bump_build.sh [--apply]"; exit 2 ;;
  esac
done

SEQ="$(sed -n 's/^[[:space:]]*def buildSeq[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$GRADLE" | head -1)"
SEQ="${SEQ:-0}"
NEW_SEQ=$((SEQ + 1))

if [ "$APPLY" = "1" ]; then
  sed -i "s/^\([[:space:]]*def buildSeq[[:space:]]*=[[:space:]]*\)[0-9][0-9]*/\1$NEW_SEQ/" "$GRADLE"
fi
echo "$NEW_SEQ"
