#!/bin/bash
# 端到端验证脚本：在运行中的后端上演练完整业务闭环与 8 类特殊场景
set -u
BASE=http://localhost:8080
PASS=0; FAIL=0

login() { curl -s "$BASE/api/auth/login" -X POST -H "Content-Type: application/json" \
  -d "{\"username\":\"$1\",\"password\":\"123456\"}" | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])"; }

check() { # desc expected actual
  if [ "$2" = "$3" ]; then echo "  ✅ $1 ($3)"; PASS=$((PASS+1));
  else echo "  ❌ $1 期望[$2] 实际[$3]"; FAIL=$((FAIL+1)); fi
}

ADMIN=$(login admin); TEACHER=$(login teacher.wang); SCREENER=$(login screener.chen)
DOCTOR=$(login doctor.zhou); OPTOM=$(login optom.wu); OPTICIAN=$(login optician.sun); EDU=$(login edu)
H="-H Authorization:Bearer"

api() { curl -s -H "Authorization: Bearer $1" "${@:2}"; }

echo "== 1. 登录与账号体系 =="
N=$(api $ADMIN "$BASE/api/users" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "种子账号数量=10" 10 "$N"

echo "== 2. 学生档案 =="
T=$(api $ADMIN "$BASE/api/students?size=50" | python3 -c "import sys,json;print(json.load(sys.stdin)['total'])")
check "学生总数=14" 14 "$T"

echo "== 3. CSV 导入（含错误行 → IMPORT_ERROR） =="
printf '姓名,性别,出生日期,年级,班级,学籍号,家长姓名,家长电话,既往视力档案\n导入甲,男,2017-02-02,五年级,1班,I901,甲父,13800009001,无\n坏行,男\n' > /tmp/imp.csv
IMP=$(curl -s -H "Authorization: Bearer $TEACHER" -F "file=@/tmp/imp.csv" -F "schoolId=7" "$BASE/api/students/import")
check "导入成功1行" 1 "$(echo $IMP|python3 -c 'import sys,json;print(json.load(sys.stdin)["success"])')"
check "导入失败1行" 1 "$(echo $IMP|python3 -c 'import sys,json;print(json.load(sys.stdin)["failed"])')"
IE=$(api $TEACHER "$BASE/api/issues?issueType=IMPORT_ERROR" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "导入错误问题登记" 1 "$IE"

echo "== 4. 请假场景（ABSENT） =="
AB=$(api $TEACHER "$BASE/api/issues?issueType=ABSENT&status=OPEN" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "请假待补筛问题" 1 "$AB"

echo "== 5. 现场异常录入 → 自动家长确认单+短信队列+电话问题 =="
BATCH=$(api $SCREENER "$BASE/api/screening/batches" | python3 -c "import sys,json;print(json.load(sys.stdin)[0]['id'])")
SID=$(api $TEACHER "$BASE/api/students?kw=%E5%86%AF&status=AWAIT_CONFIRM&size=10" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['items'][0]['id'])")
RES=$(curl -s -H "Authorization: Bearer $SCREENER" -H "Content-Type: application/json" -X POST "$BASE/api/screening/records" -d "{\"batchId\":$BATCH,\"studentId\":$SID,\"nakedOd\":4.6,\"nakedOs\":4.6,\"sphOd\":-1.5,\"sphOs\":-1.5,\"cylOd\":-0.5,\"cylOs\":-0.5,\"deviceCalibNo\":\"CAL-E2E-1\",\"hoursOutdoor\":0.5,\"screenHours\":3.5}")
check "新筛查记录已建" true "$(echo "$RES" | python3 -c 'import sys,json;json.load(sys.stdin);print("true")')"
STATUS=$(api $TEACHER "$BASE/api/students/$SID" | python3 -c "import sys,json;print(json.load(sys.stdin)['student']['status'])")
check "学生状态=待家长确认" AWAIT_CONFIRM "$STATUS"
STREAM=$(redis-cli XLEN notify:sms-gateway)
check "短信已推入 Redis 网关队列" true "$([ "${STREAM:-0}" -ge 1 ] && echo true || echo false)"

echo "== 6. 家长暂缓 + 再次提醒 =="
CID=$(api $TEACHER "$BASE/api/screening/confirmations?status=PENDING" | python3 -c "
import sys,json
d=json.load(sys.stdin); print([x['confirmation']['id'] for x in d if x['confirmation']['studentId']==$SID][0])")
curl -s -H "Authorization: Bearer $TEACHER" -H "Content-Type: application/json" -X POST "$BASE/api/screening/confirmations/$CID/decision" -d '{"choice":"POSTPONE","reason":"先观察两个月"}' >/dev/null
check "暂缓后状态" ON_HOLD "$(api $TEACHER "$BASE/api/students/$SID"|python3 -c 'import sys,json;print(json.load(sys.stdin)["student"]["status"])')"
curl -s -H "Authorization: Bearer $TEACHER" -X POST "$BASE/api/screening/confirmations/$CID/remind" >/dev/null

echo "== 7. 家长改选县医院 → 学校复核 → 转诊 =="
curl -s -H "Authorization: Bearer $TEACHER" -H "Content-Type: application/json" -X POST "$BASE/api/screening/confirmations/$CID/decision" -d '{"choice":"COUNTY_HOSPITAL"}' >/dev/null
RV=$(curl -s -H "Authorization: Bearer $TEACHER" -H "Content-Type: application/json" -X POST "$BASE/api/screening/confirmations/$CID/review" -d '{"approved":true,"note":"同意转诊"}')
check "复核通过生成转诊单" true "$(echo "$RV" | python3 -c 'import sys,json;print("true" if "referralId" in json.load(sys.stdin) else "false")')"
check "复核后状态=待复诊" AWAIT_REVISIT "$(api $TEACHER "$BASE/api/students/$SID"|python3 -c 'import sys,json;print(json.load(sys.stdin)["student"]["status"])')"

echo "== 8. 复诊回写（差异预警 + 复查计划） =="
DG=$(curl -s -H "Authorization: Bearer $DOCTOR" -H "Content-Type: application/json" -X POST "$BASE/api/revisit/diagnoses" -d "{\"studentId\":$SID,\"visitDate\":\"2026-10-07\",\"trueMyopia\":true,\"needMydriasis\":true,\"mydriasisDone\":true,\"degree\":\"MEDIUM\",\"rxSphOd\":-3.0,\"rxSphOs\":-2.75,\"rxCylOd\":-0.75,\"rxCylOs\":-0.5,\"revisitCycleMonths\":6,\"subsidyEligible\":true,\"advice\":\"配镜\"}")
check "复诊诊断已建" true "$(echo "$DG" | python3 -c 'import sys,json;print("true" if "id" in json.load(sys.stdin) else "false")')"
check "复诊后状态" REVISIT_DONE "$(api $DOCTOR "$BASE/api/students/$SID"|python3 -c 'import sys,json;print(json.load(sys.stdin)["student"]["status"])')"
DIV=$(api $DOCTOR "$BASE/api/issues?issueType=RESULT_DIVERGENCE&status=OPEN" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "筛查-复诊差异问题>=1" true "$([ "$DIV" -ge 1 ] && echo true || echo false)"
FU=$(api $TEACHER "$BASE/api/followups?studentId=$SID" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "复查计划已生成" 1 "$FU"

echo "== 9. 配镜 → 补助自动申请 → 审核 → 发放 =="
GL=$(curl -s -H "Authorization: Bearer $OPTICIAN" -H "Content-Type: application/json" -X POST "$BASE/api/glasses" -d "{\"studentId\":$SID,\"price\":460}")
SUB=$(echo $GL|python3 -c 'import sys,json;print(json.load(sys.stdin)["subsidyId"])')
check "补助自动发起" true "$([ -n "$SUB" ] && echo true || echo false)"
curl -s -H "Authorization: Bearer $EDU" -H "Content-Type: application/json" -X POST "$BASE/api/subsidies/$SUB/decision" -d '{"action":"APPROVE","note":"符合标准"}' >/dev/null
curl -s -H "Authorization: Bearer $EDU" -X POST "$BASE/api/subsidies/$SUB/pay" >/dev/null
check "补助发放后状态" SUBSIDY_PAID "$(api $EDU "$BASE/api/students/$SID"|python3 -c 'import sys,json;print(json.load(sys.stdin)["student"]["status"])')"

echo "== 10. 配镜不适 → 调试 → 确认 =="
GID=$(api $OPTICIAN "$BASE/api/glasses?studentId=$SID" | python3 -c "import sys,json;print(json.load(sys.stdin)[0]['id'])")
curl -s -H "Authorization: Bearer $TEACHER" -H "Content-Type: application/json" -X POST "$BASE/api/glasses/$GID/discomfort" -d '{"note":"戴镜头晕"}' >/dev/null
curl -s -H "Authorization: Bearer $OPTICIAN" -H "Content-Type: application/json" -X POST "$BASE/api/glasses/$GID/readjust" -d '{"note":"重新调整镜架与瞳距"}' >/dev/null
curl -s -H "Authorization: Bearer $TEACHER" -X POST "$BASE/api/glasses/$GID/confirm" >/dev/null
GST=$(api $OPTICIAN "$BASE/api/glasses?studentId=$SID" | python3 -c "import sys,json;print(json.load(sys.stdin)[0]['status'])")
check "配镜状态=确认舒适" CONFIRMED_OK "$GST"
DC=$(api $TEACHER "$BASE/api/issues?issueType=GLASSES_DISCOMFORT" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "配镜不适问题已登记>=1" true "$([ "$DC" -ge 1 ] && echo true || echo false)"

echo "== 11. 学校干预批量 → 执行 → 部门核验 =="
SIDS=$(api $TEACHER "$BASE/api/students?size=50" | python3 -c "import sys,json;print(','.join(str(x['id']) for x in json.load(sys.stdin)['items'][:3]))")
IDS_JSON="[$(echo $SIDS | sed 's/,/,/g')]"
IV=$(curl -s -H "Authorization: Bearer $TEACHER" -H "Content-Type: application/json" -X POST "$BASE/api/interventions" -d "{\"type\":\"OUTDOOR_ACTIVITY\",\"title\":\"每日2小时户外活动\",\"studentIds\":$IDS_JSON,\"plannedDate\":\"2026-10-07\"}")
check "批量干预=3条" 3 "$(echo $IV|python3 -c 'import sys,json;print(json.load(sys.stdin)["created"])')"
IVID=$(echo $IV|python3 -c 'import sys,json;print(json.load(sys.stdin)["ids"][0])')
curl -s -H "Authorization: Bearer $TEACHER" -H "Content-Type: application/json" -X POST "$BASE/api/interventions/$IVID/execute" -d '{"effectNote":"已执行"}' >/dev/null
curl -s -H "Authorization: Bearer $EDU" -H "Content-Type: application/json" -X POST "$BASE/api/interventions/$IVID/verify" -d '{"effectNote":"核验通过"}' >/dev/null
IVS=$(api $EDU "$BASE/api/interventions" | python3 -c "import sys,json;d=json.load(sys.stdin);print(len([x for x in d if x['intervention']['status']=='VERIFIED']))")
check "核验记录数>=1" true "$([ "$IVS" -ge 1 ] && echo true || echo false)"

# 给本次闭环学生单独安排并核验一条干预（时间线需要）
IV2=$(curl -s -H "Authorization: Bearer $TEACHER" -H "Content-Type: application/json" -X POST "$BASE/api/interventions" -d "{\"type\":\"SEAT_ADJUSTMENT\",\"title\":\"座位调至前排\",\"studentId\":$SID,\"plannedDate\":\"2026-10-07\"}")
IV2ID=$(echo $IV2|python3 -c 'import sys,json;print(json.load(sys.stdin)["ids"][0])')
curl -s -H "Authorization: Bearer $TEACHER" -H "Content-Type: application/json" -X POST "$BASE/api/interventions/$IV2ID/execute" -d '{"effectNote":"已调座"}' >/dev/null
curl -s -H "Authorization: Bearer $EDU" -H "Content-Type: application/json" -X POST "$BASE/api/interventions/$IV2ID/verify" -d '{"effectNote":"通过"}' >/dev/null

echo "== 12. 自行外配 / 补助争议 场景 =="
SE=$(api $TEACHER "$BASE/api/issues?issueType=SELF_GLASSES" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "自行外配问题存在" 1 "$SE"
SD=$(api $EDU "$BASE/api/subsidies?status=DISPUTED" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "补助争议单存在" 1 "$SD"

echo "== 13. 半年复查逾期扫描（MISSED_REVIEW） =="
BEFORE=$(api $TEACHER "$BASE/api/issues?issueType=MISSED_REVIEW" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
SCAN=$(curl -s -H "Authorization: Bearer $EDU" -X POST "$BASE/api/followups/scan-overdue")
NEW=$(echo $SCAN|python3 -c 'import sys,json;print(json.load(sys.stdin)["newlyMissed"])')
check "扫描发现逾期>=1" true "$([ "$NEW" -ge 1 ] && echo true || echo false)"
AFTER=$(api $TEACHER "$BASE/api/issues?issueType=MISSED_REVIEW" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "逾期问题已自动登记" true "$([ "$AFTER" -gt "$BEFORE" ] && echo true || echo false)"

echo "== 14. 时间线串联完整环节 =="
TL=$(api $TEACHER "$BASE/api/students/$SID")
for ev in SCREENING_ABNORMAL PARENT_CHOICE SCHOOL_REVIEW_APPROVE REVISIT_RESULT RESULT_DIVERGENCE GLASSES_FITTED SUBSIDY_PAID INTERVENTION_PLANNED FOLLOWUP_PLANNED; do
  echo $TL | grep -q "$ev" && { echo "  ✅ 时间线含 $ev"; PASS=$((PASS+1)); } || { echo "  ❌ 时间线缺 $ev"; FAIL=$((FAIL+1)); }
done

echo "== 15. 监管统计 =="
OV=$(api $EDU "$BASE/api/stats/overview")
check "真实复诊率字段存在" true "$(echo "$OV" | python3 -c 'import sys,json;print("true" if "realRevisitRate" in json.load(sys.stdin) else "false")')"
RK=$(api $EDU "$BASE/api/stats/school-ranking" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "学校排名数据=2" 2 "$RK"
AR=$(api $EDU "$BASE/api/stats/by-area" | python3 -c "import sys,json;print(len(json.load(sys.stdin)))")
check "片区统计=2" 2 "$AR"
TR=$(api $EDU "$BASE/api/stats/trend" | python3 -c "import sys,json;print(len(json.load(sys.stdin)['monthly'])>=1)")
check "月度趋势有数据" True "$TR"

echo
echo "通过 $PASS / 失败 $FAIL"
[ $FAIL -eq 0 ]
