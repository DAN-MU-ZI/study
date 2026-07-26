import "./styles.css";

const DEMO_POOL_CAP = 12;
const INITIAL_POOL_SIZE = 9;
const LOW_WATERMARK = 4;
const MAX_RESERVE_QTY = 4;
const OPERATION_DELAY = 760;

const app = document.querySelector("#app");

const state = {
  busy: false,
  reserveQty: 2,
  activeSql: "reserve",
  activeNote: "대기 중",
  lockOwnerByUnitId: new Map(),
  skipHintUnitIds: new Set(),
  recentUnitIds: new Set(),
  recentReservedUnitIds: new Set(),
  recentClaimedUnitIds: new Set(),
  recentInsertedUnitIds: new Set(),
  recentReleasedUnitIds: new Set(),
  replenishmentLocked: false,
  sequence: 1001,
  nowStep: 0,
  ledger: {
    shop_id: 1,
    inventory_item_id: 501,
    location_id: 77,
    total_quantity: 24,
    claimed_quantity: 0,
  },
  replenishment: {
    shop_id: 1,
    inventory_item_id: 501,
    location_id: 77,
    next_unit_id: INITIAL_POOL_SIZE + 1,
    target_pool_size: DEMO_POOL_CAP,
    low_watermark: LOW_WATERMARK,
  },
  availableUnits: makeUnits(1, INITIAL_POOL_SIZE),
  reservedRows: [],
  claimedRows: [],
  releaseBacklog: [],
  timeline: [],
};

function makeUnits(start, count) {
  return Array.from({ length: count }, (_, index) => ({
    unit_id: start + index,
    created_at: "demo",
  }));
}

function init() {
  logEvent("초기화", "reservation_units pool을 9개 row로 시작");
  render();
}

function resetState() {
  state.busy = false;
  state.reserveQty = 2;
  state.activeSql = "reserve";
  state.activeNote = "대기 중";
  state.lockOwnerByUnitId = new Map();
  state.skipHintUnitIds = new Set();
  state.recentUnitIds = new Set();
  state.recentReservedUnitIds = new Set();
  state.recentClaimedUnitIds = new Set();
  state.recentInsertedUnitIds = new Set();
  state.recentReleasedUnitIds = new Set();
  state.replenishmentLocked = false;
  state.sequence = 1001;
  state.nowStep = 0;
  state.ledger = {
    shop_id: 1,
    inventory_item_id: 501,
    location_id: 77,
    total_quantity: 24,
    claimed_quantity: 0,
  };
  state.replenishment = {
    shop_id: 1,
    inventory_item_id: 501,
    location_id: 77,
    next_unit_id: INITIAL_POOL_SIZE + 1,
    target_pool_size: DEMO_POOL_CAP,
    low_watermark: LOW_WATERMARK,
  };
  state.availableUnits = makeUnits(1, INITIAL_POOL_SIZE);
  state.reservedRows = [];
  state.claimedRows = [];
  state.releaseBacklog = [];
  state.timeline = [];
  logEvent("초기화", "모든 데모 상태를 되돌림");
  render();
}

function render() {
  const availableCount = state.availableUnits.length;
  const reservedCount = state.reservedRows.length;
  const poolStatus = availableCount <= LOW_WATERMARK ? "low" : "ok";

  app.innerHTML = `
    <div class="shell">
      <header class="topbar">
        <div>
          <p class="eyebrow">Shopify inventory reservations</p>
          <h1>MySQL 재고 예약 DB 시각화</h1>
          <p class="lead">
            재고 1개를 row 1개로 두고, 결제 시작 시 row lock을 잡은 뒤 예약 테이블로 이동시키는 구조를 보여준다.
          </p>
        </div>
        <a class="source-link" href="https://shopify.engineering/scaling-inventory-reservations" target="_blank" rel="noreferrer">
          Shopify 원문
        </a>
      </header>

      <section class="metrics" aria-label="현재 상태 요약">
        ${metricCard("total_quantity", state.ledger.total_quantity, "inventory_ledger")}
        ${metricCard("claimed_quantity", state.ledger.claimed_quantity, "inventory_ledger")}
        ${metricCard("reservation_units", availableCount, "available pool", poolStatus)}
        ${metricCard("reserved_quantities", reservedCount, "payment hold")}
        ${metricCard("next_unit_id", state.replenishment.next_unit_id, "replenishment_state")}
      </section>

      <main class="dashboard-grid">
        <section class="panel control-panel" aria-label="시뮬레이션 제어">
          <div class="panel-heading">
            <div>
              <p class="section-label">Controls</p>
              <h2>결제 이벤트</h2>
            </div>
            <button class="ghost-button" data-action="reset">Reset</button>
          </div>

          <div class="quantity-control">
            <label for="reserveQty">요청 수량</label>
            <input id="reserveQty" type="number" min="1" max="${MAX_RESERVE_QTY}" value="${state.reserveQty}" ${state.busy ? "disabled" : ""} />
          </div>

          <div class="action-grid">
            <button class="action-button primary" data-action="reserve" ${state.busy ? "disabled" : ""}>결제 시작: Reserve</button>
            <button class="action-button" data-action="concurrent" ${state.busy ? "disabled" : ""}>동시 결제 요청</button>
            <button class="action-button success" data-action="claim" ${state.busy || !reservedCount ? "disabled" : ""}>결제 성공: Claim</button>
            <button class="action-button warn" data-action="release" ${state.busy || !reservedCount ? "disabled" : ""}>결제 실패/만료: Release</button>
            <button class="action-button refill" data-action="replenish" ${state.busy ? "disabled" : ""}>Pool 보충: Replenish</button>
          </div>

          <div class="sql-panel">
            <div class="sql-title">
              <span>SQL focus</span>
              <span class="badge">${state.busy ? "transaction active" : "ready"}</span>
            </div>
            ${sqlSnippet()}
          </div>
        </section>

        <section class="panel visual-panel" aria-label="DB row 이동 시각화">
          <div class="stage-header">
            <div>
              <p class="section-label">Visual flow</p>
              <h2>${state.activeNote}</h2>
            </div>
            <div class="stage-tags">
              <span class="pool-badge ${poolStatus}">${poolStatus === "low" ? "low watermark" : "pool ready"}</span>
              <span class="pool-badge neutral">demo cap ${DEMO_POOL_CAP}</span>
            </div>
          </div>

          <div class="pipeline">
            ${renderPoolLane()}
            ${renderLockLane()}
            ${renderReservedLane()}
            ${renderOutcomeLane()}
          </div>

          <div class="support-row">
            ${renderLedgerCard()}
            ${renderReplenishmentCard()}
          </div>
        </section>

        <aside class="inspector-column" aria-label="DB 상태와 로그">
          <section class="panel tables-panel" aria-label="DB 테이블 상태">
            <div class="panel-heading">
              <div>
                <p class="section-label">DB state</p>
                <h2>실제 테이블 row 변화</h2>
              </div>
            </div>
            ${renderDbTables()}
          </section>

          <section class="panel timeline-panel" aria-label="이벤트 타임라인">
            <div class="panel-heading">
              <div>
                <p class="section-label">Event timeline</p>
                <h2>트랜잭션 로그</h2>
              </div>
            </div>
            <ol class="timeline">
              ${renderTimeline()}
            </ol>
          </section>
        </aside>
      </main>
    </div>
  `;

  bindEvents();
}

function metricCard(label, value, context, status = "") {
  return `
    <article class="metric ${status}">
      <span>${context}</span>
      <strong>${value}</strong>
      <p>${label}</p>
    </article>
  `;
}

function renderPoolLane() {
  const rows = state.availableUnits
    .map((unit) => {
      const owner = state.lockOwnerByUnitId.get(unit.unit_id);
      const status = owner ? "locked" : "available";
      const skipped = state.skipHintUnitIds.has(unit.unit_id) ? " skipped" : "";
      const recent = state.recentUnitIds.has(unit.unit_id) ? " recent" : "";
      return unitChip(unit.unit_id, status, owner ? `${owner} lock` : "available", `${skipped}${recent}`);
    })
    .join("");

  return `
    <article class="lane pool-lane">
      <div class="lane-title">
        <span>1</span>
        <div>
          <h3>reservation_units</h3>
          <p>예약 가능한 재고 row</p>
        </div>
      </div>
      <div class="unit-grid pool-grid">
        ${rows || emptyLane("pool empty")}
      </div>
    </article>
  `;
}

function renderLockLane() {
  const lockedUnits = state.availableUnits.filter((unit) => state.lockOwnerByUnitId.has(unit.unit_id));
  const grouped = lockedUnits.reduce((acc, unit) => {
    const owner = state.lockOwnerByUnitId.get(unit.unit_id);
    acc[owner] = acc[owner] || [];
    acc[owner].push(unit);
    return acc;
  }, {});

  const groups = Object.entries(grouped)
    .map(([owner, units]) => `
      <div class="lock-group">
        <strong>${owner}</strong>
        <div class="lock-unit-row">
          ${units.map((unit) => unitChip(unit.unit_id, "locked", "row lock", "small")).join("")}
        </div>
      </div>
    `)
    .join("");

  return `
    <article class="lane lock-lane">
      <div class="lane-title">
        <span>2</span>
        <div>
          <h3>SELECT ... FOR UPDATE</h3>
          <p>SKIP LOCKED로 잠긴 row 회피</p>
        </div>
      </div>
      <div class="lane-body lock-body">
        ${groups || emptyLane("아직 lock 없음")}
        ${state.skipHintUnitIds.size ? `<p class="skip-note">T2는 흐리게 표시된 T1 row를 건너뛰었다.</p>` : ""}
      </div>
    </article>
  `;
}

function renderReservedLane() {
  const rows = state.reservedRows
    .map((row) => {
      const recent = state.recentReservedUnitIds.has(row.unit_id) ? " recent" : "";
      return unitChip(row.unit_id, "reserved", row.reservation_id, recent);
    })
    .join("");

  return `
    <article class="lane reserved-lane">
      <div class="lane-title">
        <span>3</span>
        <div>
          <h3>reserved_quantities</h3>
          <p>결제 대기 예약 row</p>
        </div>
      </div>
      <div class="unit-grid reserved-grid">
        ${rows || emptyLane("예약 row 없음")}
      </div>
    </article>
  `;
}

function renderOutcomeLane() {
  const claimed = state.claimedRows
    .slice(-6)
    .map((row) => {
      const recent = state.recentClaimedUnitIds.has(row.unit_id) ? " recent" : "";
      return unitChip(row.unit_id, "claimed", "claimed", recent);
    })
    .join("");

  const released = state.releaseBacklog
    .slice(-6)
    .map((row) => {
      const recent = state.recentReleasedUnitIds.has(row.unit_id) ? " recent" : "";
      return unitChip(row.unit_id, "released", "refill needed", recent);
    })
    .join("");

  return `
    <article class="lane outcome-lane">
      <div class="lane-title">
        <span>4</span>
        <div>
          <h3>Claim / Release</h3>
          <p>확정 차감 또는 보충 대기</p>
        </div>
      </div>
      <div class="outcome-columns">
        <div>
          <strong>claimed</strong>
          <div class="mini-stack">${claimed || emptyLane("없음")}</div>
        </div>
        <div>
          <strong>release backlog</strong>
          <div class="mini-stack">${released || emptyLane("없음")}</div>
        </div>
      </div>
    </article>
  `;
}

function renderLedgerCard() {
  const sellable = state.ledger.total_quantity - state.ledger.claimed_quantity;
  return `
    <article class="support-card ledger-card">
      <div>
        <p class="section-label">inventory_ledger</p>
        <h3>원장 기준 실제 재고</h3>
      </div>
      <div class="ledger-bars">
        <div>
          <span>total</span>
          <strong>${state.ledger.total_quantity}</strong>
        </div>
        <div>
          <span>claimed</span>
          <strong>${state.ledger.claimed_quantity}</strong>
        </div>
        <div>
          <span>sellable left</span>
          <strong>${sellable}</strong>
        </div>
      </div>
    </article>
  `;
}

function renderReplenishmentCard() {
  const inserted = Array.from(state.recentInsertedUnitIds)
    .map((unitId) => unitChip(unitId, "available", "inserted", "small recent"))
    .join("");

  return `
    <article class="support-card replenish-card ${state.replenishmentLocked ? "locked-state" : ""}">
      <div>
        <p class="section-label">replenishment_state</p>
        <h3>pool 보충 제어 row</h3>
      </div>
      <div class="replenish-data">
        <span>next_unit_id</span>
        <strong>${state.replenishment.next_unit_id}</strong>
        <span>target / low</span>
        <strong>${state.replenishment.target_pool_size} / ${state.replenishment.low_watermark}</strong>
        <span>lock</span>
        <strong>${state.replenishmentLocked ? "locked" : "free"}</strong>
      </div>
      <div class="insert-preview">
        ${inserted || "<span>최근 보충 row 없음</span>"}
      </div>
    </article>
  `;
}

function unitChip(unitId, status, label, extraClass = "") {
  return `
    <div class="unit ${status} ${extraClass}" title="unit_id ${unitId}">
      <span>#${unitId}</span>
      <small>${label}</small>
    </div>
  `;
}

function emptyLane(message) {
  return `
    <div class="empty-lane">
      ${message}
    </div>
  `;
}

function renderDbTables() {
  return `
    <div class="table-grid">
      ${tableBlock("inventory_ledger", ["shop_id", "item_id", "location_id", "total", "claimed"], [[
        state.ledger.shop_id,
        state.ledger.inventory_item_id,
        state.ledger.location_id,
        state.ledger.total_quantity,
        state.ledger.claimed_quantity,
      ]])}
      ${tableBlock("reservation_units", ["shop_id", "item_id", "location_id", "unit_id"], state.availableUnits.map((unit) => [
        state.ledger.shop_id,
        state.ledger.inventory_item_id,
        state.ledger.location_id,
        markUnit(unit.unit_id),
      ]))}
      ${tableBlock("reserved_quantities", ["reservation_id", "unit_id", "status", "expires_at"], state.reservedRows.map((row) => [
        row.reservation_id,
        markReserved(row.unit_id),
        row.status,
        row.expires_at,
      ]))}
      ${tableBlock("replenishment_state", ["next_unit_id", "target", "low_watermark", "lock"], [[
        state.replenishment.next_unit_id,
        state.replenishment.target_pool_size,
        state.replenishment.low_watermark,
        state.replenishmentLocked ? "locked" : "free",
      ]], state.replenishmentLocked ? "locked-table" : "")}
    </div>
  `;
}

function tableBlock(title, headers, rows, extraClass = "") {
  const bodyRows = rows.length
    ? rows
        .map((row) => `
          <tr>
            ${row.map((cell) => `<td>${cell}</td>`).join("")}
          </tr>
        `)
        .join("")
    : `<tr><td colspan="${headers.length}" class="empty-cell">row 없음</td></tr>`;

  return `
    <div class="table-block ${extraClass}">
      <h3>${title}</h3>
      <div class="table-scroll">
        <table>
          <thead>
            <tr>${headers.map((header) => `<th>${header}</th>`).join("")}</tr>
          </thead>
          <tbody>${bodyRows}</tbody>
        </table>
      </div>
    </div>
  `;
}

function markUnit(unitId) {
  const className = state.recentInsertedUnitIds.has(unitId) || state.recentUnitIds.has(unitId) ? "cell-mark" : "";
  return `<span class="${className}">${unitId}</span>`;
}

function markReserved(unitId) {
  const className = state.recentReservedUnitIds.has(unitId) ? "cell-mark reserved-mark" : "";
  return `<span class="${className}">${unitId}</span>`;
}

function sqlSnippet() {
  return `
    <pre class="sql"><code><span class="${sqlClass("reserve")}">SELECT unit_id
FROM reservation_units
WHERE shop_id = 1
  AND inventory_item_id = 501
  AND location_id = 77
ORDER BY unit_id
LIMIT :quantity
FOR UPDATE SKIP LOCKED;</span>

<span class="${sqlClass("delete")}">DELETE FROM reservation_units
WHERE (shop_id, inventory_item_id, location_id, unit_id)
IN (:locked_units);</span>

<span class="${sqlClass("insert")}">INSERT INTO reserved_quantities
  (reservation_id, shop_id, inventory_item_id, location_id, unit_id, status, expires_at)
VALUES (:reservation_id, 1, 501, 77, :unit_id, 'RESERVED', :expires_at);</span>

<span class="${sqlClass("claim")}">UPDATE inventory_ledger
SET claimed_quantity = claimed_quantity + :quantity
WHERE shop_id = 1 AND inventory_item_id = 501 AND location_id = 77;</span>

<span class="${sqlClass("release")}">DELETE FROM reserved_quantities
WHERE reservation_id = :reservation_id;</span>

<span class="${sqlClass("replenish")}">SELECT next_unit_id
FROM replenishment_state
WHERE shop_id = 1 AND inventory_item_id = 501 AND location_id = 77
FOR UPDATE;</span></code></pre>
  `;
}

function sqlClass(name) {
  return state.activeSql === name ? "sql-line active" : "sql-line";
}

function renderTimeline() {
  return state.timeline
    .slice(0, 10)
    .map((entry) => `
      <li>
        <span>${entry.time}</span>
        <strong>${entry.title}</strong>
        <p>${entry.message}</p>
      </li>
    `)
    .join("");
}

function bindEvents() {
  document.querySelector("[data-action='reserve']").addEventListener("click", reserve);
  document.querySelector("[data-action='concurrent']").addEventListener("click", concurrentReserve);
  document.querySelector("[data-action='claim']").addEventListener("click", claimReservation);
  document.querySelector("[data-action='release']").addEventListener("click", releaseReservation);
  document.querySelector("[data-action='replenish']").addEventListener("click", replenishPool);
  document.querySelector("[data-action='reset']").addEventListener("click", resetState);
  document.querySelector("#reserveQty").addEventListener("change", (event) => {
    const parsed = Number(event.target.value);
    state.reserveQty = clamp(Number.isFinite(parsed) ? parsed : 1, 1, MAX_RESERVE_QTY);
    render();
  });
}

async function reserve() {
  const quantity = state.reserveQty;
  if (!hasEnoughAvailable(quantity)) {
    state.activeNote = "pool row 부족";
    logEvent("Reserve rejected", `pool row가 ${quantity}개보다 적다. 먼저 Replenish를 실행`);
    render();
    return;
  }

  state.busy = true;
  clearTransientMarks();
  const selected = takeUnlockedUnits(quantity);
  selected.forEach((unit) => state.lockOwnerByUnitId.set(unit.unit_id, "T1"));
  state.recentUnitIds = new Set(selected.map((unit) => unit.unit_id));
  state.activeSql = "reserve";
  state.activeNote = "1. Reserve: 선택된 row에 lock이 걸림";
  logEvent("Reserve requested", `T1이 unit ${unitList(selected)} row를 lock`);
  render();

  await pause();
  state.activeSql = "delete";
  state.activeNote = "2. DELETE: lock을 잡은 row를 pool에서 제거";
  logEvent("DELETE FROM reservation_units", `lock을 잡은 ${selected.length}개 row를 available pool에서 제거`);
  render();

  await pause();
  const reservationId = nextReservationId("rsv");
  moveUnitsToReserved(selected, reservationId);
  state.activeSql = "insert";
  state.activeNote = "3. INSERT: reserved_quantities에 결제 대기 row 생성";
  logEvent("INSERT INTO reserved_quantities", `${reservationId}에 unit ${unitList(selected)} 예약 기록`);
  state.busy = false;
  state.lockOwnerByUnitId.clear();
  render();
}

async function concurrentReserve() {
  const quantity = state.reserveQty;
  if (!hasEnoughAvailable(quantity * 2)) {
    state.activeNote = "동시 요청 데모 row 부족";
    logEvent("Concurrent reserve rejected", `동시 요청 데모에는 available row ${quantity * 2}개가 필요`);
    render();
    return;
  }

  state.busy = true;
  clearTransientMarks();
  const firstSelection = takeUnlockedUnits(quantity);
  firstSelection.forEach((unit) => state.lockOwnerByUnitId.set(unit.unit_id, "T1"));
  state.recentUnitIds = new Set(firstSelection.map((unit) => unit.unit_id));
  state.activeSql = "reserve";
  state.activeNote = "1. T1이 먼저 row lock을 잡음";
  logEvent("T1 Reserve requested", `T1이 unit ${unitList(firstSelection)} row를 lock`);
  render();

  await pause();
  const secondSelection = takeUnlockedUnits(quantity);
  secondSelection.forEach((unit) => state.lockOwnerByUnitId.set(unit.unit_id, "T2"));
  state.skipHintUnitIds = new Set(firstSelection.map((unit) => unit.unit_id));
  state.recentUnitIds = new Set([...firstSelection, ...secondSelection].map((unit) => unit.unit_id));
  state.activeNote = "2. T2는 T1 lock row를 SKIP LOCKED로 건너뜀";
  logEvent("SELECT ... SKIP LOCKED", `T2는 T1 lock row를 건너뛰고 unit ${unitList(secondSelection)} 선택`);
  render();

  await pause();
  state.activeSql = "delete";
  state.activeNote = "3. 두 트랜잭션이 각자 확보한 row를 pool에서 제거";
  logEvent("DELETE FROM reservation_units", "T1, T2가 각자 확보한 row를 pool에서 제거");
  render();

  await pause();
  moveUnitsToReserved(firstSelection, nextReservationId("t1"));
  moveUnitsToReserved(secondSelection, nextReservationId("t2"));
  state.activeSql = "insert";
  state.activeNote = "4. 서로 다른 unit row가 reserved_quantities로 이동";
  logEvent("INSERT INTO reserved_quantities", "두 요청 모두 서로 다른 unit row로 예약 생성");
  state.busy = false;
  state.lockOwnerByUnitId.clear();
  state.skipHintUnitIds.clear();
  render();
}

async function claimReservation() {
  const group = oldestReservationGroup();
  if (!group) {
    state.activeNote = "Claim할 예약 row 없음";
    logEvent("Claim skipped", "reserved_quantities에 결제 대기 row가 없음");
    render();
    return;
  }

  state.busy = true;
  clearTransientMarks();
  state.activeSql = "claim";
  state.activeNote = "Claim: 결제 성공 row를 원장 차감으로 확정";
  state.recentReservedUnitIds = new Set(group.rows.map((row) => row.unit_id));
  logEvent("Claim requested", `${group.reservationId} 결제 성공. 원장 차감 준비`);
  render();

  await pause();
  state.ledger.claimed_quantity += group.rows.length;
  state.reservedRows = state.reservedRows.filter((row) => row.reservation_id !== group.reservationId);
  const claimedAt = demoTime();
  group.rows.forEach((row) => {
    state.claimedRows.push({
      reservation_id: row.reservation_id,
      unit_id: row.unit_id,
      claimed_at: claimedAt,
    });
  });
  state.recentClaimedUnitIds = new Set(group.rows.map((row) => row.unit_id));
  logEvent("inventory_ledger updated", `claimed_quantity가 ${state.ledger.claimed_quantity}로 증가`);
  state.busy = false;
  render();
}

async function releaseReservation() {
  const group = oldestReservationGroup();
  if (!group) {
    state.activeNote = "Release할 예약 row 없음";
    logEvent("Release skipped", "reserved_quantities에 해제할 row가 없음");
    render();
    return;
  }

  state.busy = true;
  clearTransientMarks();
  state.activeSql = "release";
  state.activeNote = "Release: 예약 row를 제거하고 보충 대기 상태로 이동";
  state.recentReservedUnitIds = new Set(group.rows.map((row) => row.unit_id));
  logEvent("Release requested", `${group.reservationId} 결제 실패 또는 TTL 만료`);
  render();

  await pause();
  state.reservedRows = state.reservedRows.filter((row) => row.reservation_id !== group.reservationId);
  const releasedAt = demoTime();
  group.rows.forEach((row) => {
    state.releaseBacklog.push({
      reservation_id: row.reservation_id,
      unit_id: row.unit_id,
      released_at: releasedAt,
    });
  });
  state.recentReleasedUnitIds = new Set(group.rows.map((row) => row.unit_id));
  logEvent("reserved_quantities deleted", `unit ${unitList(group.rows)}는 보충 필요 상태로 이동`);
  state.busy = false;
  render();
}

async function replenishPool() {
  const desired = state.replenishment.target_pool_size - state.availableUnits.length;
  const capacity = availableCapacity();
  const insertCount = Math.max(0, Math.min(desired, capacity));

  if (insertCount === 0) {
    state.activeNote = "보충할 row 없음";
    logEvent("Replenish skipped", "target pool에 도달했거나 원장 기준 추가 가능 수량이 없음");
    render();
    return;
  }

  state.busy = true;
  clearTransientMarks();
  state.activeSql = "replenish";
  state.activeNote = "Replenish: 보충 제어 row를 lock";
  state.replenishmentLocked = true;
  logEvent("Replenish requested", "replenishment_state row를 lock하고 next_unit_id 조회");
  render();

  await pause();
  const startId = state.replenishment.next_unit_id;
  const newUnits = makeUnits(startId, insertCount);
  state.availableUnits = [...state.availableUnits, ...newUnits];
  state.replenishment.next_unit_id += insertCount;
  state.releaseBacklog = state.releaseBacklog.slice(insertCount);
  state.recentInsertedUnitIds = new Set(newUnits.map((unit) => unit.unit_id));
  state.replenishmentLocked = false;
  state.activeNote = "Replenish: 새 reservation_units row가 pool에 추가됨";
  logEvent("reservation_units inserted", `unit ${unitList(newUnits)} row를 pool에 보충`);
  state.busy = false;
  render();
}

function hasEnoughAvailable(quantity) {
  return state.availableUnits.filter((unit) => !state.lockOwnerByUnitId.has(unit.unit_id)).length >= quantity;
}

function takeUnlockedUnits(quantity) {
  return state.availableUnits
    .filter((unit) => !state.lockOwnerByUnitId.has(unit.unit_id))
    .slice(0, quantity);
}

function moveUnitsToReserved(units, reservationId) {
  const unitIds = new Set(units.map((unit) => unit.unit_id));
  state.availableUnits = state.availableUnits.filter((unit) => !unitIds.has(unit.unit_id));
  const expiresAt = demoTime(5);
  units.forEach((unit) => {
    state.reservedRows.push({
      reservation_id: reservationId,
      shop_id: state.ledger.shop_id,
      inventory_item_id: state.ledger.inventory_item_id,
      location_id: state.ledger.location_id,
      unit_id: unit.unit_id,
      status: "RESERVED",
      expires_at: expiresAt,
    });
  });
  state.recentReservedUnitIds = new Set(units.map((unit) => unit.unit_id));
}

function oldestReservationGroup() {
  if (!state.reservedRows.length) {
    return null;
  }

  const reservationId = state.reservedRows[0].reservation_id;
  return {
    reservationId,
    rows: state.reservedRows.filter((row) => row.reservation_id === reservationId),
  };
}

function availableCapacity() {
  return Math.max(
    0,
    state.ledger.total_quantity -
      state.ledger.claimed_quantity -
      state.reservedRows.length -
      state.availableUnits.length,
  );
}

function nextReservationId(prefix) {
  const id = `${prefix}-${state.sequence}`;
  state.sequence += 1;
  return id;
}

function logEvent(title, message) {
  state.nowStep += 1;
  state.timeline.unshift({
    time: `T+${String(state.nowStep).padStart(2, "0")}`,
    title,
    message,
  });
}

function unitList(units) {
  return units.map((unit) => `#${unit.unit_id}`).join(", ");
}

function demoTime(minutesToAdd = 0) {
  const baseMinutes = 10 + state.nowStep + minutesToAdd;
  const minutes = String(baseMinutes % 60).padStart(2, "0");
  return `12:${minutes}:00`;
}

function clearTransientMarks() {
  state.recentUnitIds = new Set();
  state.recentReservedUnitIds = new Set();
  state.recentClaimedUnitIds = new Set();
  state.recentInsertedUnitIds = new Set();
  state.recentReleasedUnitIds = new Set();
  state.skipHintUnitIds = new Set();
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max);
}

function pause() {
  return new Promise((resolve) => {
    window.setTimeout(resolve, OPERATION_DELAY);
  });
}

init();
