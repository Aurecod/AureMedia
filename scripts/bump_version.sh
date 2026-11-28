#!/usr/bin/env bash
# AureMedia 版本号递增脚本（百进制：第三段 0-99，满 99 进位第二段；第二段满 99 进位第一段）
# versionCode = 第一段*10000 + 第二段*100 + 第三段
#
# 用法:
#   bash scripts/bump_version.sh               # 打印下一版本（不写文件）
#   bash scripts/bump_version.sh --apply       # 递增并写回 app/build.gradle
#   bash scripts/bump_version.sh --set 1.2.3   # 指定版本（--apply 时写回）
set -euo pipefail

GRADLE="${AURE_BUILD_GRADLE:-app/build.gradle}"
APPLY=0
SET_VERSION=""

while [ $# -gt 0 ]; do
  case "$1" in
    --apply) APPLY=1; shift ;;
    --dry-run) APPLY=0; shift ;;
    --set) SET_VERSION="$2"; shift 2 ;;
    *) echo "usage: bump_version.sh [--apply] [--set X.Y.Z]"; exit 2 ;;
  esac
done

VN="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*"\([^"]*\)".*/\1/p' "$GRADLE" | head -1)"
VC="$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$GRADLE" | head -1)"
if [ -z "$VN" ]; then
  echo "ERROR: cannot read versionName from $GRADLE" >&2
  exit 1
fi

if [ -n "$SET_VERSION" ]; then
  NEW_VN="$SET_VERSION"
else
  IFS='.' read -r MA MI PA <<< "$VN"
  MA="${MA:-0}"; MI="${MI:-0}"; PA="${PA:-0}"
  PA=$((PA + 1))
  if [ "$PA" -gt 99 ]; then PA=0; MI=$((MI + 1)); fi
  if [ "$MI" -gt 99 ]; then MI=0; MA=$((MA + 1)); fi
  NEW_VN="$MA.$MI.$PA"
fi

if ! printf '%s' "$NEW_VN" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  echo "ERROR: invalid version '$NEW_VN' (expected X.Y.Z)" >&2
  exit 1
fi

IFS='.' read -r MA MI PA <<< "$NEW_VN"
NEW_VC=$((MA * 10000 + MI * 100 + PA))

if [ -n "$VC" ] && [ "$NEW_VC" -le "$VC" ] && [ -z "$SET_VERSION" ]; then
  echo "ERROR: new versionCode $NEW_VC must be > current $VC" >&2
  exit 1
fi

echo "old=${VN} (code ${VC:-?}) new=${NEW_VN} (code ${NEW_VC})" >&2

if [ "$APPLY" -eq 1 ]; then
  sed -i \
    -e "s/^\([[:space:]]*versionName[[:space:]]*\)\"[^\"]*\"/\1\"$NEW_VN\"/" \
    -e "s/^\([[:space:]]*versionCode[[:space:]]*\)[0-9][0-9]*/\1$NEW_VC/" \
    "$GRADLE"
  echo "written: versionName=$NEW_VN versionCode=$NEW_VC" >&2
fi

echo "$NEW_VN"
