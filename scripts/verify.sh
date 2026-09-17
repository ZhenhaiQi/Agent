#!/usr/bin/env bash
#
# 阶段 1 回归验证脚本
#
# 用法：
#   1) 先在一个终端用「真 key」启动服务：
#        export DEEPSEEK_API_KEY=sk-xxxx
#        cd ~/Desktop/agent && mvn spring-boot:run
#   2) 另开一个终端跑本脚本：
#        ./scripts/verify.sh
#        BASE=http://localhost:8081 ./scripts/verify.sh     # 指定端口
#
# 退出码：0 = 全部通过；1 = 有失败项
#
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
MARKER="|__STATUS__|"
PASS=0
FAIL=0

# call <METHOD> <PATH> [JSON_BODY]
# 结果写入全局 HTTP_CODE / BODY
call() {
  local method="$1" path="$2" data="${3:-}" out
  if [ -n "$data" ]; then
    out=$(curl -s -m 60 -w "${MARKER}%{http_code}" -X "$method" "$BASE$path" \
            -H "Content-Type: application/json" -d "$data")
  else
    out=$(curl -s -m 60 -w "${MARKER}%{http_code}" -X "$method" "$BASE$path")
  fi
  HTTP_CODE="${out##*${MARKER}}"
  BODY="${out%%${MARKER}*}"
}

# check <名称> <期望状态码> <响应中应包含的字串(可空)>
check() {
  local name="$1" want="$2" needle="${3:-}" preview
  preview=$(printf '%s' "$BODY" | tr -d '\n' | cut -c1-140)

  if [ "$HTTP_CODE" != "$want" ]; then
    echo "❌ $name"
    echo "   期望状态码 ${want}，实际 ${HTTP_CODE}"
    echo "   响应: $preview"
    hint "$name" "$HTTP_CODE"
    FAIL=$((FAIL + 1))
    return 1
  fi
  if [ -n "$needle" ] && ! printf '%s' "$BODY" | grep -q -- "$needle"; then
    echo "❌ $name"
    echo "   状态码 ${want} 正确，但响应里没有 \"${needle}\""
    echo "   响应: $preview"
    FAIL=$((FAIL + 1))
    return 1
  fi
  echo "✅ $name  [$HTTP_CODE]  $preview"
  PASS=$((PASS + 1))
}

# 针对常见失败给出可操作提示
hint() {
  case "$2" in
    502) echo "   → 上游鉴权失败：确认启动服务那个终端 export 了 DEEPSEEK_API_KEY" ;;
    402) echo "   → 上游账户余额不足，去 DeepSeek 控制台充值" ;;
    504) echo "   → 连不上上游：检查网络 / 代理" ;;
    000) echo "   → 完全连不上服务，确认服务已启动、端口正确" ;;
  esac
}

echo "目标服务：$BASE"
echo

# 连通性预检
if ! curl -s -o /dev/null -m 5 -X POST "$BASE/api/chat" \
       -H "Content-Type: application/json" -d '{}'; then
  echo "❌ 连不上 $BASE"
  echo "   请先启动服务：export DEEPSEEK_API_KEY=sk-xxx && mvn spring-boot:run"
  exit 1
fi

echo "── ① 对话（核心：返回中文文本 = 链路跑通）"
call POST /api/chat '{"message":"用一句话解释什么是 Agent"}'
check "POST /api/chat" 200

echo
echo "── ② 结构化输出（文本 → JSON）"
call POST /api/extract '{"text":"张三今年30岁，住在杭州"}'
check "POST /api/extract" 200 "name"

echo
echo "── ③ 参数校验（缺字段应被拒）"
call POST /api/chat '{}'
check "POST /api/chat 空参数" 400 "参数校验失败"

echo
echo "── ④ 方法不允许（GET 已下线）"
call GET /api/chat
check "GET /api/chat" 405

echo
echo "──────────────────────────────"
if [ "$FAIL" -eq 0 ]; then
  echo "✅ 全部通过（${PASS}/${PASS}）"
  exit 0
else
  echo "❌ 通过 $PASS 项，失败 $FAIL 项"
  exit 1
fi
