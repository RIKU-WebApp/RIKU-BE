"""Generate a read-only snapshot query, or turn its mysql --batch output into local reports."""
import argparse
import collections
import html
import json
from datetime import date, timedelta
from pathlib import Path

root = Path(__file__).resolve().parents[2]
assets = root / "src/main/resources/badges"
catalog = json.loads((assets / "series.json").read_text(encoding="utf-8"))
labels = {}
rules = []
for family in catalog:
    prefix = family.get("prefix", family["id"])
    tiers = family["tiers"]
    milestone = family["id"] in ("verifiedKm","personalKm") and tiers[-1]=="명예"
    assert 1 <= len(tiers) <= (6 if milestone else 5) and family["levels"] == sorted(set(family["levels"]))
    for target, key, tier in zip(family["levels"], family.get("codeLevels", family["levels"]), tiers, strict=True):
        code = f"{prefix}_{key}"
        rules.append({"metric": family["id"], "code": code, "goal": target})
        labels[code] = f"{family['name']} {tier}"

def podium_query(with_ledger=False):
    # Enumerate periods from the same read-only snapshot, including any newly imported older data.
    current = next(line.split(":",1)[1].split("#")[0].strip() for line in (assets.parent/"application.yml").read_text(encoding="utf8").splitlines() if line.strip().startswith("start-date:"))
    starts=sorted({date.fromisoformat(s) for s in [*(assets/"semester-starts.txt").read_text().splitlines(),current]})
    terms="".join(f" UNION ALL SELECT 'SEMESTER',DATE('{a}'),DATE('{b}'),1 WHERE DATE('{b}')<=DATE(TIMESTAMPADD(HOUR,9,UTC_TIMESTAMP()))" for a,b in zip(starts,starts[1:]))
    if with_ledger:
        history=" UNION ".join(f"SELECT DATE('{d}') AS starts_on" for d in starts)
        terms=f""" UNION ALL SELECT 'SEMESTER',starts_on,ends_before,1 FROM (
          SELECT starts_on,LEAD(starts_on) OVER(ORDER BY starts_on) AS ends_before
          FROM ({history} UNION SELECT starts_on FROM badge_semester_start) starts
        ) terms WHERE ends_before<=DATE(TIMESTAMPADD(HOUR,9,UTC_TIMESTAMP()))"""
    template=(assets/"rank-podium.sql").read_text(encoding="utf8")
    for expr in ["TIMESTAMPADD(HOUR,-9,periods.starts_on)","TIMESTAMPADD(HOUR,-9,periods.ends_before)","periods.min_members"]:
        template=template.replace("?",expr,1)
    return """WITH RECURSIVE months(m) AS (
      SELECT CAST(DATE_FORMAT(MIN(TIMESTAMPADD(HOUR,9,created_at)),'%Y-%m-01') AS DATE) FROM user_point
      UNION ALL SELECT m+INTERVAL 1 MONTH FROM months WHERE m+INTERVAL 1 MONTH<=DATE(TIMESTAMPADD(HOUR,9,UTC_TIMESTAMP()))
    ), periods(kind,starts_on,ends_before,min_members) AS (
      SELECT 'MONTH',m,m+INTERVAL 1 MONTH,20 FROM months WHERE m+INTERVAL 1 MONTH<=DATE(TIMESTAMPADD(HOUR,9,UTC_TIMESTAMP()))
    """+terms+""") SELECT podium.member,periods.kind,periods.starts_on,periods.ends_before-INTERVAL 1 DAY AS achieved_on,podium.place
    FROM periods JOIN LATERAL ("""+template+""") podium ON TRUE ORDER BY member,achieved_on DESC,kind"""

def snapshot_sql(with_ledger=False):
    compact = json.dumps(rules).encode().hex()
    sql = (assets / "eligibility.sql").read_text(encoding="utf-8")
    sql = sql.replace("__VISITS__", (assets/"verified-visits.sql").read_text(encoding="utf8").replace("__MEMBER_FILTER__",""))
    sql = sql.replace("__MONTHLY_RANKS__",(assets/"monthly-ranks.sql").read_text(encoding="utf-8").replace("__MONTH_KEY__", "EXTRACT(YEAR_MONTH FROM TIMESTAMPADD(HOUR,9,UTC_TIMESTAMP()))"))
    sql=sql.replace("__RECORD_PEAKS__",(assets/"record-peaks.sql").read_text(encoding="utf8") if with_ledger else "")
    assert sql.count("JSON_TABLE(?") == 1
    return sql.replace("JSON_TABLE(?", f"JSON_TABLE(CONVERT(UNHEX('{compact}') USING utf8mb4)")

begin="SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;\nSTART TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY;\n"

def audit_query():
    snapshot=snapshot_sql(True)
    return begin+"""
      SELECT COUNT(DISTINCT s.user_id) AS active_accounts,COUNT(s.code) AS expected_grants,
             COUNT(g.badge_code) AS matched_grants,COUNT(s.code)-COUNT(g.badge_code) AS missing_grants
      FROM ("""+snapshot+""") s LEFT JOIN automatic_badge_grant g ON g.user_id=s.user_id AND g.badge_code=s.code
      WHERE s.status='ACTIVE';
      SELECT COUNT(*) AS podium_candidates,
        COALESCE(SUM(CASE WHEN COALESCE(p.settled,FALSE)=FALSE THEN 1 ELSE 0 END),0) AS awaiting_settlement,
        COALESCE(SUM(CASE WHEN p.settled AND (h.place_no IS NULL OR h.place_no<>w.place) THEN 1 ELSE 0 END),0) AS differs_from_frozen_result
      FROM ("""+podium_query(True)+""") w LEFT JOIN rank_honor_period p ON p.kind=w.kind AND p.starts_on=w.starts_on
      LEFT JOIN rank_honor h ON h.kind=w.kind AND h.starts_on=w.starts_on AND h.user_id=w.member;
      COMMIT;"""

def query(with_ledger=False):
    return (begin + snapshot_sql(with_ledger) +
            ";\nSELECT COUNT(*) AS accounts, UTC_TIMESTAMP() AS snapshot_time FROM users;\n"+podium_query(with_ledger)+";\nCOMMIT;")

def render(tsv, out):
    lines = iter(tsv.splitlines())
    assert next(lines) == "user_id\tstatus\tcode"
    members = {}
    for line in lines:
        if line == "accounts\tsnapshot_time":
            break
        uid, status, code = line.split("\t")
        m = members.setdefault(int(uid), {"status": status, "codes": []})
        if code != "NULL":
            assert code in labels and code not in m["codes"]
            m["codes"].append(code)
    accounts, timestamp = next(lines).split("\t")
    assert len(members) == int(accounts)
    podiums=collections.defaultdict(list)
    header=next(lines,None)
    if header is not None:
        assert header=="member\tkind\tstarts_on\tachieved_on\tplace"
        for line in lines:
            member,kind,start,end,place=line.split("\t")
            assert int(member) in members and int(place) in (1,2,3)
            podiums[int(member)].append(dict(kind=kind,start=start,achievedOn=end,place=int(place)))
    active = [m for m in members.values() if m["status"] == "ACTIVE"]
    count = collections.Counter(c for m in active for c in m["codes"])
    eligible = sum(bool(m["codes"]) for m in active)
    summary = dict(snapshot_utc=timestamp, accounts=len(members), active=len(active),
                   eligible_members=eligible, active_without_badges=len(active)-eligible,
                   excluded_accounts=len(members)-len(active), qualifying_grants=sum(count.values()))
    out.mkdir(exist_ok=True, parents=True)
    results = [dict(member_id=i, account_status=m["status"], eligible_codes=m["codes"] if m["status"]=="ACTIVE" else [],
                    historical_codes=m["codes"],projected_podiums=podiums.get(i,[])) for i,m in members.items()]
    (out/"members.json").write_text(json.dumps({"summary":summary,"badges":labels,"members":results},ensure_ascii=False,indent=2),encoding="utf-8")
    rows = []
    for i,m in members.items():
        codes = m["codes"]
        titles = " / ".join(labels[c] for c in sorted(codes,key=list(labels).index))
        ok = m["status"]=="ACTIVE"
        places="<br>".join(f"{'월간' if x['kind']=='MONTH' else '학기'} {x['place']}위 ({x['start']} ~ {x['achievedOn']})" for x in podiums.get(i,[]))
        rows.append(f'<tr><th>{i}</th><td>{"활성" if ok else "지급 제외"}</td><td>{len(codes) if ok else 0}</td><td>{places or "—"}</td><td>{html.escape(titles) if ok else "—"}</td></tr>')
    body = f'''<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width">
    <title>RIKU 회원별 배지 산출표</title><style>*{{box-sizing:border-box}}body{{max-width:1100px;margin:30px auto;padding:0 20px;font:16px/1.5 system-ui;color:#253e33}}small{{color:#54695a}}input{{padding:9px;width:200px}}table{{border-collapse:collapse;width:100%;margin:12px 0}}td,th{{text-align:left;padding:8px;border-bottom:1px solid #ddd}}th,td:nth-child(2){{white-space:nowrap}}thead{{background:#e8f0e9}}details{{margin:20px 0}}</style>
    <h1>회원별 배지 산출표</h1><small>{timestamp} UTC · 운영 데이터 조회 시점 · 미지급</small>
    <p>전체 {len(members)}명 / 활성 {len(active)}명 / 지급 대상 {eligible}명 / 지급 예정 {sum(count.values()):,}개</p>
    <details><summary>배지별 대상 인원 ({len(labels)}종)</summary><table><tbody>{''.join(f"<tr><th>{title}</th><td>{count[c]}명</td></tr>" for c,title in labels.items())}</tbody></table></details>
    <label>회원 번호 <input id="search" type="search" placeholder="내부 회원 번호"></label>
    <table><thead><tr><th>회원 번호</th><th>상태</th><th>배지 수</th><th>기간별 입상 예상</th><th>지급 예정 배지</th></tr></thead><tbody id="members">{''.join(rows)}</tbody></table>
    <script>document.querySelector('#search').oninput=e=>{{const q=e.target.value.trim();for(const r of document.querySelectorAll('#members tr')) r.hidden=!!q&&r.cells[0].textContent!==q}}</script></html>'''
    (out/"index.html").write_text(body,encoding="utf-8")
    print(json.dumps(summary))

if __name__ == "__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--sql",action="store_true")
    parser.add_argument("--input",type=Path)
    parser.add_argument("--out",type=Path,default=root/"build/badge-review")
    parser.add_argument("--with-ledger",action="store_true",help="include durable record peaks after the badge schema is installed")
    parser.add_argument("--audit-sql",action="store_true",help="read-only counts of missing grants and unsettled podiums (installed schema)")
    args=parser.parse_args()
    if args.audit_sql: print(audit_query())
    elif args.sql: print(query(args.with_ledger))
    elif args.input: render(args.input.read_text(encoding="utf-8-sig"),args.out)
    else: parser.error("choose --sql or --input")
