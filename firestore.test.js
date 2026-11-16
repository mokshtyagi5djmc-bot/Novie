const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validReportPayload(uid, overrides = {}) {
  const now = new Date();
  return {
    userId: uid,
    title: "Q1 SaaS Cash Flow",
    sourceFileName: "saas_q1.xlsx",
    sheetName: "CashFlow",
    baseCurrencyCode: "USD",
    totalInflow: 120000,
    totalOutflow: 75000,
    entryCount: 1,
    entries: [
      {
        periodLabel: "2026-01",
        dateMillis: 1767225600000,
        category: "Subscriptions",
        type: "INFLOW",
        amount: 120000,
        note: "MRR",
      },
    ],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot list or create reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/cashFlowReports`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/cashFlowReports`).doc("report_1").set(validReportPayload(ALICE_UID))
  );
});

test("Authenticated user can create, read, update, and delete own report", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection(`users/${ALICE_UID}/cashFlowReports`).doc("report_1");
  const payload = validReportPayload(ALICE_UID);

  await assertSucceeds(docRef.set(payload));
  await assertSucceeds(docRef.get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/cashFlowReports`).get());
  await assertSucceeds(
    docRef.update({
      title: "Updated Q1 SaaS Cash Flow",
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("Cross-user access is strictly denied", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection(`users/${ALICE_UID}/cashFlowReports`)
      .doc("alice_report")
      .set(validReportPayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/cashFlowReports`).get());
  await assertFails(bobDb.collection(`users/${ALICE_UID}/cashFlowReports`).doc("alice_report").get());
  await assertFails(
    bobDb
      .collection(`users/${ALICE_UID}/cashFlowReports`)
      .doc("alice_report")
      .update({ title: "Hijacked", updatedAt: new Date() })
  );
  await assertFails(bobDb.collection(`users/${ALICE_UID}/cashFlowReports`).doc("alice_report").delete());
});

test("Shadow update with ghost field and identity spoofing are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/cashFlowReports`)
      .doc("ghost_1")
      .set(validReportPayload(ALICE_UID, { isAdmin: true }))
  );
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/cashFlowReports`)
      .doc("spoof_1")
      .set(validReportPayload(BOB_UID))
  );
});

test("Negative amounts and invalid IDs are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/cashFlowReports`)
      .doc("bad_amount")
      .set(validReportPayload(ALICE_UID, { totalInflow: -50 }))
  );
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/cashFlowReports`)
      .doc("invalid id with spaces")
      .set(validReportPayload(ALICE_UID))
  );
});
