const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_karas";
const UNCLE_UID = "uncle_mina";
const OTHER_UID = "other_user";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

test("Unauthenticated user: cannot read classes or deacons", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("classes").get());
  await assertFails(unauthDb.collection("deacons").get());
});

test("Super Admin: can create and update a class", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "karasreda146@gmail.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("classes").doc("class_1").set({
      name: "فصل الملاك ميخائيل",
      stage: "المرحلة الابتدائية",
      description: "فصل إعداد الشمامسة الصغار",
      assignedUncleIds: [UNCLE_UID],
      assignedUncleNames: ["أنكل مينا"],
      studentCount: 0,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Non-admin user: cannot create a class", async () => {
  const userDb = testEnv.authenticatedContext(UNCLE_UID, {
    email: "uncle.mina@example.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    userDb.collection("classes").doc("class_2").set({
      name: "فصل مارمرقس",
      stage: "المرحلة الإعدادية",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Uncle: can add a deacon if assigned to class", async () => {
  // Seed uncle user doc with assigned class
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(UNCLE_UID).set({
      userId: UNCLE_UID,
      email: "uncle.mina@example.com",
      displayName: "أنكل مينا",
      role: "uncle",
      assignedClassIds: ["class_1"],
    });
  });

  const uncleDb = testEnv.authenticatedContext(UNCLE_UID).firestore();

  await assertSucceeds(
    uncleDb.collection("deacons").doc("deacon_1").set({
      fullName: "كيرلس رامي نبيل",
      phone: "01234567890",
      parentPhone: "01123456789",
      rank: "إبصالتس",
      classId: "class_1",
      className: "فصل الملاك ميخائيل",
      grade: "الصف الثالث الابتدائي",
      birthDate: "2015-05-12",
      address: "شارع الكنيسة - الإخلاص",
      notes: "شماس منتظم ومجتهد في الألحان",
      addedByUid: UNCLE_UID,
      assignedUncleIds: [UNCLE_UID],
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Uncle: cannot add deacon to unassigned class", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(UNCLE_UID).set({
      userId: UNCLE_UID,
      email: "uncle.mina@example.com",
      displayName: "أنكل مينا",
      role: "uncle",
      assignedClassIds: ["class_1"],
    });
  });

  const uncleDb = testEnv.authenticatedContext(UNCLE_UID).firestore();

  await assertFails(
    uncleDb.collection("deacons").doc("deacon_2").set({
      fullName: "يوحنا سامي",
      phone: "01000000000",
      rank: "إبصالتس",
      classId: "unassigned_class",
      addedByUid: UNCLE_UID,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});
