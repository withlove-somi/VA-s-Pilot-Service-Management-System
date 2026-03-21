const express = require("express");
const cors = require("cors");
const mongoose = require("mongoose");
const path = require("path");


const app = express();
const PORT = Number(process.env.PORT) || 5000;
// Use Atlas when provided, fallback to local for development.
const MONGO_URI = process.env.MONGO_URI || "mongodb://127.0.0.1:27017/va_psm";

app.use(cors());
app.use(express.json({ limit: "25mb" }));
app.use(express.static(path.join(__dirname, 'public')));


function normalizeEmail(email) {
  return String(email || "").trim().toLowerCase();
}

function clientError(res, message, status = 400) {
  return res.status(status).json({ ok: false, error: message });
}

function parseNumberLike(value, defaultValue = 0) {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const cleaned = value.replace(/[^0-9.-]/g, "");
    const parsed = Number(cleaned);
    if (Number.isFinite(parsed)) return parsed;
  }
  return defaultValue;
}

const userSchema = new mongoose.Schema(
  {
    fullname: { type: String, trim: true },
    name: { type: String, trim: true },
    email: {
      type: String,
      required: true,
      unique: true,
      lowercase: true,
      trim: true,
    },
    password: { type: String, required: true },
    role: { type: String, enum: ["customer", "admin"], default: "customer" },
    verified: { type: Boolean, default: false },
    phoneNumber: { type: String, trim: true },
    profileImage: { type: String },
    warningLevel: { type: Number, default: 0 },
    banned: { type: Boolean, default: false },
    adminActions: [
      {
        type: { type: String, enum: ["flag", "ban"], required: true },
        level: { type: Number },
        reason: { type: String, trim: true },
        adminEmail: { type: String, trim: true, lowercase: true },
        createdAt: { type: Date, default: Date.now },
      },
    ],
  },
  { timestamps: true }
);

const orderSchema = new mongoose.Schema(
  {
    id: { type: String, required: true, unique: true, trim: true },
    customerName: { type: String, trim: true },
    customerEmail: { type: String, required: true, lowercase: true, trim: true },
    customerVerified: { type: Boolean, default: false },
    pilotLoginMethod: { type: String, trim: true },
    pilotLoginEmail: { type: String, trim: true },
    pilotLoginPassword: { type: String },
    pilotLoginNumber: { type: String, trim: true },
    game: { type: String, trim: true },
    ign: { type: String, trim: true },
    serverId: { type: String, trim: true },
    accountId: { type: String, trim: true },
    currentRank: { type: String, trim: true },
    targetRank: { type: String, trim: true },
    starsNeeded: { type: Number, default: 0 },
    totalPrice: { type: Number, default: 0 },
    notes: { type: String, trim: true },
    paymentMode: { type: String, trim: true },
    status: { type: String, default: "Order Request" },
    paymentReference: { type: String, trim: true },
    receiptImage: { type: String },
    paymentDate: { type: Date },
    latestTransactionId: { type: mongoose.Schema.Types.ObjectId, ref: "Transaction" },
    declineReason: { type: String, trim: true },
  },
  { timestamps: true }
);

const notificationSchema = new mongoose.Schema(
  {
    userEmail: { type: String, required: true, lowercase: true, trim: true },
    title: { type: String, required: true, trim: true },
    message: { type: String, required: true, trim: true },
    type: { type: String, default: "general", trim: true },
    read: { type: Boolean, default: false },
  },
  { timestamps: true }
);

const verificationRequestSchema = new mongoose.Schema(
  {
    id: { type: String, required: true, unique: true, trim: true },
    userEmail: { type: String, required: true, lowercase: true, trim: true },
    phoneNumber: { type: String, trim: true },
    govIdType: { type: String, trim: true },
    govIdNumber: { type: String, trim: true },
    govIdPhoto: { type: String },
    selfieWithId: { type: String },
    status: {
      type: String,
      enum: ["Pending", "Approved", "Rejected"],
      default: "Pending",
    },
    reason: { type: String, trim: true },
  },
  { timestamps: true }
);

const chatMessageSchema = new mongoose.Schema(
  {
    userEmail: { type: String, required: true, lowercase: true, trim: true },
    sender: {
      type: String,
      enum: ["customer", "admin", "system"],
      default: "customer",
    },
    message: { type: String, required: true, trim: true },
    read: { type: Boolean, default: false },
  },
  { timestamps: true }
);

const feedbackSchema = new mongoose.Schema(
  {
    userId: { type: String, trim: true },
    userEmail: { type: String, lowercase: true, trim: true },
    name: { type: String, trim: true },
    email: { type: String, lowercase: true, trim: true },
    rating: { type: Number, min: 1, max: 5 },
    message: { type: String, required: true, trim: true },
  },
  { timestamps: true }
);

const transactionSchema = new mongoose.Schema(
  {
    orderId: { type: String, required: true, trim: true },
    userEmail: { type: String, required: true, lowercase: true, trim: true },
    amount: { type: Number, default: 0 },
    paymentReference: { type: String, trim: true },
    receiptImage: { type: String },
    status: {
      type: String,
      enum: ["Submitted", "Approved", "Rejected"],
      default: "Submitted",
    },
    adminEmail: { type: String, trim: true, lowercase: true },
    reviewedAt: { type: Date },
  },
  { timestamps: true }
);

const User = mongoose.model("User", userSchema);
const Order = mongoose.model("Order", orderSchema);
const Notification = mongoose.model("Notification", notificationSchema);
const VerificationRequest = mongoose.model(
  "VerificationRequest",
  verificationRequestSchema
);
const ChatMessage = mongoose.model("ChatMessage", chatMessageSchema);
const Feedback = mongoose.model("Feedback", feedbackSchema);
const Transaction = mongoose.model("Transaction", transactionSchema);

async function addNotification(userEmail, title, message, type = "general") {
  const email = normalizeEmail(userEmail);
  if (!email) return;
  await Notification.create({ userEmail: email, title, message, type });
}

app.get("/api/health", async (req, res) => {
  res.json({
    ok: true,
    service: "va-pilot-backend",
    dbState: mongoose.connection.readyState,
    dbName: mongoose.connection?.name || null,
  });
});

async function handleRegister(req, res) {
  try {
    const fullname = String(req.body.fullname || req.body.name || "").trim();
    const email = normalizeEmail(req.body.email);
    const password = String(req.body.password || "").trim();

    if (!fullname) return clientError(res, "fullname is required");
    if (!email) return clientError(res, "email is required");
    if (!password) return clientError(res, "password is required");

    const exists = await User.findOne({ email }).lean();
    if (exists) return clientError(res, "Email already registered", 409);

    const user = await User.create({
      fullname,
      name: fullname,
      email,
      password,
      role: "customer",
      verified: false,
    });

    res.status(201).json({
      ok: true,
      user: {
        id: user._id,
        fullname: user.fullname,
        name: user.name,
        email: user.email,
        verified: user.verified,
        role: user.role,
      },
    });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
}

app.post("/api/auth/register", handleRegister);
// Compatibility endpoint for older frontend code.
app.post("/api/register", handleRegister);

app.post("/api/auth/login", async (req, res) => {
  try {
    const email = normalizeEmail(req.body.email);
    const password = String(req.body.password || req.body.passcode || "");

    if (!email || !password) return clientError(res, "email and password are required");

    const user = await User.findOne({ email }).lean();
    if (!user || user.password !== password) {
      return clientError(res, "Invalid credentials", 401);
    }

    res.json({
      ok: true,
      user: {
        id: user._id,
        fullname: user.fullname,
        name: user.name,
        email: user.email,
        verified: Boolean(user.verified),
        role: user.role,
      },
    });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.patch("/api/auth/password", async (req, res) => {
  try {
    const email = normalizeEmail(req.body.email);
    const newPassword = String(req.body.newPassword || "").trim();
    if (!email || !newPassword) {
      return clientError(res, "email and newPassword are required");
    }

    const user = await User.findOneAndUpdate(
      { email },
      { password: newPassword },
      { new: true }
    ).lean();

    if (!user) return clientError(res, "User not found", 404);

    res.json({
      ok: true,
      user: {
        id: user._id,
        fullname: user.fullname,
        name: user.name,
        email: user.email,
        verified: Boolean(user.verified),
        role: user.role,
      },
    });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.post("/api/admin/login", async (req, res) => {
  try {
    const email = normalizeEmail(req.body.email);
    const password = String(req.body.password || "");
    if (!email || !password) return clientError(res, "email and password are required");

    const user = await User.findOne({ email, role: "admin" }).lean();
    if (!user || user.password !== password) {
      return clientError(res, "Invalid admin credentials", 401);
    }

    res.json({
      ok: true,
      admin: {
        id: user._id,
        fullname: user.fullname,
        email: user.email,
        role: user.role,
      },
    });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.get("/api/users", async (req, res) => {
  try {
    const users = await User.find({}, "-password").sort({ createdAt: -1 }).lean();
    res.json({ ok: true, users });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.get("/api/users/:email", async (req, res) => {
  try {
    const email = normalizeEmail(req.params.email);
    const user = await User.findOne({ email }, "-password").lean();
    if (!user) return clientError(res, "User not found", 404);
    res.json({ ok: true, user });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.patch("/api/users/:email/profile-image", async (req, res) => {
  try {
    const email = normalizeEmail(req.params.email);
    const profileImage = String(req.body.profileImage || "");
    if (!profileImage) return clientError(res, "profileImage is required");

    const user = await User.findOneAndUpdate(
      { email },
      { profileImage },
      { new: true, projection: "-password" }
    ).lean();

    if (!user) return clientError(res, "User not found", 404);

    res.json({ ok: true, user });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.patch("/api/users/:email/verification", async (req, res) => {
  try {
    const email = normalizeEmail(req.params.email);
    const verified = Boolean(req.body.verified);

    const user = await User.findOneAndUpdate(
      { email },
      { verified },
      { new: true, projection: "-password" }
    ).lean();

    if (!user) return clientError(res, "User not found", 404);

    await Order.updateMany({ customerEmail: email }, { customerVerified: verified });
    res.json({ ok: true, user });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.patch("/api/users/:email/admin-action", async (req, res) => {
  try {
    const email = normalizeEmail(req.params.email);
    const action = String(req.body.action || "").trim().toLowerCase();
    const reason = String(req.body.reason || "").trim();
    const adminEmail = normalizeEmail(req.body.adminEmail || "");

    if (!email) return clientError(res, "email is required");
    if (!["flag", "ban"].includes(action)) {
      return clientError(res, "action must be flag or ban");
    }
    if (!reason) return clientError(res, "reason is required");

    const user = await User.findOne({ email });
    if (!user) return clientError(res, "User not found", 404);

    if (action === "flag") {
      if (user.banned) return clientError(res, "User is already banned", 409);
      const currentLevel = Number(user.warningLevel || 0);
      if (currentLevel >= 3) {
        return clientError(res, "Third warning already served", 409);
      }
      const nextLevel = currentLevel + 1;
      user.warningLevel = nextLevel;
      user.adminActions = user.adminActions || [];
      user.adminActions.push({
        type: "flag",
        level: nextLevel,
        reason,
        adminEmail: adminEmail || undefined,
      });
    } else {
      if (user.banned) return clientError(res, "User is already banned", 409);
      user.banned = true;
      user.adminActions = user.adminActions || [];
      user.adminActions.push({
        type: "ban",
        reason,
        adminEmail: adminEmail || undefined,
      });
    }

    await user.save();
    const payload = user.toObject();
    delete payload.password;
    res.json({ ok: true, user: payload });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.get("/api/orders", async (req, res) => {
  try {
    const filter = {};
    if (req.query.customerEmail) {
      filter.customerEmail = normalizeEmail(req.query.customerEmail);
    }
    if (req.query.status) filter.status = String(req.query.status);

    const orders = await Order.find(filter).sort({ createdAt: -1 }).lean();
    res.json({ ok: true, orders });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.post("/api/orders", async (req, res) => {
  try {
    const email = normalizeEmail(req.body.customerEmail || req.body.customer_email);
    if (!email) return clientError(res, "customerEmail is required");

    const orderId = String(req.body.id || `ORD-${Date.now()}`).trim();
    const payload = {
      ...req.body,
      id: orderId,
      customerEmail: email,
      customerName: String(req.body.customerName || req.body.clientName || "").trim(),
      customerVerified: Boolean(req.body.customerVerified),
      starsNeeded: parseNumberLike(req.body.starsNeeded, 0),
      totalPrice: parseNumberLike(req.body.totalPrice, 0),
    };

    const order = await Order.create(payload);

    await addNotification(
      email,
      "Order Submitted",
      `Order ${order.id} has been created and is pending review.`,
      "order"
    );

    res.status(201).json({ ok: true, order });
  } catch (error) {
    if (error && error.code === 11000) {
      return clientError(res, "Order id already exists", 409);
    }
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.get("/api/transactions", async (req, res) => {
  try {
    const filter = {};
    if (req.query.orderId) filter.orderId = String(req.query.orderId).trim();
    if (req.query.userEmail) filter.userEmail = normalizeEmail(req.query.userEmail);
    if (req.query.status) filter.status = String(req.query.status).trim();

    const transactions = await Transaction.find(filter)
      .sort({ createdAt: -1 })
      .lean();
    res.json({ ok: true, transactions });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.post("/api/transactions", async (req, res) => {
  try {
    const orderId = String(req.body.orderId || "").trim();
    if (!orderId) return clientError(res, "orderId is required");

    const order = await Order.findOne({ id: orderId }).lean();
    if (!order) return clientError(res, "Order not found", 404);

    const paymentReference = String(req.body.paymentReference || req.body.receiptNumber || "").trim();
    const receiptImage = req.body.receiptImage || "";
    if (!paymentReference || !receiptImage) {
      return clientError(res, "paymentReference and receiptImage are required");
    }

    const transaction = await Transaction.create({
      orderId,
      userEmail: normalizeEmail(order.customerEmail),
      amount: parseNumberLike(req.body.amount, order.totalPrice || 0),
      paymentReference,
      receiptImage,
      status: "Submitted",
    });

    const updatedOrder = await Order.findOneAndUpdate(
      { id: orderId },
      {
        status: "Payment Sent",
        paymentReference,
        receiptImage,
        paymentDate: new Date(),
        latestTransactionId: transaction._id,
      },
      { new: true }
    ).lean();

    await addNotification(
      order.customerEmail,
      "Payment Submitted",
      `Payment proof submitted for order ${orderId}.`,
      "payment"
    );

    res.status(201).json({ ok: true, transaction, order: updatedOrder });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.patch("/api/transactions/:id/status", async (req, res) => {
  try {
    const id = String(req.params.id || "").trim();
    const status = String(req.body.status || "").trim();
    if (!["Approved", "Rejected", "Submitted"].includes(status)) {
      return clientError(res, "status must be Approved, Rejected, or Submitted");
    }

    const updates = {
      status,
      reviewedAt: status === "Submitted" ? undefined : new Date(),
      adminEmail: normalizeEmail(req.body.adminEmail || ""),
    };

    const transaction = await Transaction.findByIdAndUpdate(id, updates, {
      new: true,
    }).lean();

    if (!transaction) return clientError(res, "Transaction not found", 404);

    if (status === "Approved") {
      await Order.updateOne(
        { id: transaction.orderId },
        { status: "Payment Verified" }
      );
    }
    if (status === "Rejected") {
      await Order.updateOne(
        { id: transaction.orderId },
        { status: "Disapproved" }
      );
    }

    res.json({ ok: true, transaction });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.patch("/api/orders/:id/status", async (req, res) => {
  try {
    const id = String(req.params.id || "").trim();
    const status = String(req.body.status || "").trim();
    if (!status) return clientError(res, "status is required");

    const order = await Order.findOneAndUpdate(
      { id },
      { status, declineReason: req.body.declineReason || undefined },
      { new: true }
    ).lean();

    if (!order) return clientError(res, "Order not found", 404);

    if (status === "Payment Verified" || status === "Disapproved") {
      const latest = await Transaction.findOne({ orderId: id })
        .sort({ createdAt: -1 })
        .lean();
      if (latest) {
        await Transaction.updateOne(
          { _id: latest._id },
          {
            status: status === "Payment Verified" ? "Approved" : "Rejected",
            reviewedAt: new Date(),
          }
        );
      }
    }

    await addNotification(
      order.customerEmail,
      "Order Update",
      `Order ${order.id} status changed to ${status}.`,
      "order"
    );

    res.json({ ok: true, order });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.patch("/api/orders/:id/payment", async (req, res) => {
  try {
    const id = String(req.params.id || "").trim();
    const paymentReference = String(req.body.paymentReference || req.body.receiptNumber || "").trim();
    const receiptImage = req.body.receiptImage || "";

    if (!paymentReference) return clientError(res, "paymentReference is required");

    const order = await Order.findOneAndUpdate(
      { id },
      {
        paymentReference,
        receiptImage,
        paymentDate: new Date(),
        status: "Payment Sent",
      },
      { new: true }
    ).lean();

    if (!order) return clientError(res, "Order not found", 404);

    const transaction = await Transaction.create({
      orderId: id,
      userEmail: normalizeEmail(order.customerEmail),
      amount: parseNumberLike(order.totalPrice, 0),
      paymentReference,
      receiptImage,
      status: "Submitted",
    });

    await Order.updateOne(
      { id },
      { latestTransactionId: transaction._id }
    );

    await addNotification(
      order.customerEmail,
      "Payment Submitted",
      `Payment proof submitted for order ${order.id}.`,
      "payment"
    );

    res.json({ ok: true, order, transaction });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.get("/api/notifications", async (req, res) => {
  try {
    const email = normalizeEmail(req.query.userEmail);
    const filter = email ? { userEmail: email } : {};
    const notifications = await Notification.find(filter)
      .sort({ createdAt: -1 })
      .lean();
    res.json({ ok: true, notifications });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.post("/api/notifications", async (req, res) => {
  try {
    const userEmail = normalizeEmail(req.body.userEmail);
    const title = String(req.body.title || "").trim();
    const message = String(req.body.message || "").trim();
    const type = String(req.body.type || "general").trim();
    if (!userEmail || !title || !message) {
      return clientError(res, "userEmail, title, and message are required");
    }
    const notification = await Notification.create({
      userEmail,
      title,
      message,
      type,
    });
    res.status(201).json({ ok: true, notification });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.patch("/api/notifications/:id/read", async (req, res) => {
  try {
    const notification = await Notification.findByIdAndUpdate(
      req.params.id,
      { read: Boolean(req.body.read !== false) },
      { new: true }
    ).lean();

    if (!notification) return clientError(res, "Notification not found", 404);
    res.json({ ok: true, notification });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.get("/api/verifications", async (req, res) => {
  try {
    const filter = {};
    if (req.query.userEmail) filter.userEmail = normalizeEmail(req.query.userEmail);
    if (req.query.status) filter.status = String(req.query.status);
    const requests = await VerificationRequest.find(filter)
      .sort({ createdAt: -1 })
      .lean();
    res.json({ ok: true, requests });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.post("/api/verifications", async (req, res) => {
  try {
    const userEmail = normalizeEmail(req.body.userEmail);
    if (!userEmail) return clientError(res, "userEmail is required");

    const payload = {
      ...req.body,
      id: String(req.body.id || `VR-${Date.now()}`).trim(),
      userEmail,
      status: "Pending",
      reason: "",
    };

    const request = await VerificationRequest.create(payload);
    await addNotification(
      userEmail,
      "Verification Submitted",
      "Your verification request is under review.",
      "verification"
    );
    res.status(201).json({ ok: true, request });
  } catch (error) {
    if (error && error.code === 11000) {
      return clientError(res, "Verification request id already exists", 409);
    }
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.patch("/api/verifications/:id/status", async (req, res) => {
  try {
    const id = String(req.params.id || "").trim();
    const status = String(req.body.status || "").trim();
    const reason = String(req.body.reason || "").trim();
    if (!["Approved", "Rejected", "Pending"].includes(status)) {
      return clientError(res, "status must be Approved, Rejected, or Pending");
    }

    const request = await VerificationRequest.findOneAndUpdate(
      { id },
      { status, reason },
      { new: true }
    ).lean();

    if (!request) return clientError(res, "Verification request not found", 404);

    if (status === "Approved" || status === "Rejected") {
      const approved = status === "Approved";
      await User.updateOne({ email: request.userEmail }, { verified: approved });
      await Order.updateMany(
        { customerEmail: request.userEmail },
        { customerVerified: approved }
      );
      await addNotification(
        request.userEmail,
        `Verification ${status}`,
        approved
          ? "Your account verification was approved."
          : `Your account verification was rejected.${reason ? ` Reason: ${reason}` : ""}`,
        "verification"
      );
    }

    res.json({ ok: true, request });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.get("/api/messages", async (req, res) => {
  try {
    const userEmail = normalizeEmail(req.query.userEmail);
    if (!userEmail) return clientError(res, "userEmail is required");
    const messages = await ChatMessage.find({ userEmail })
      .sort({ createdAt: 1 })
      .lean();
    res.json({ ok: true, messages });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.post("/api/messages", async (req, res) => {
  try {
    const userEmail = normalizeEmail(req.body.userEmail);
    const sender = String(req.body.sender || "customer").trim();
    const message = String(req.body.message || "").trim();
    if (!userEmail || !message) return clientError(res, "userEmail and message are required");

    const chatMessage = await ChatMessage.create({ userEmail, sender, message });
    res.status(201).json({ ok: true, message: chatMessage });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.post("/api/feedback", async (req, res) => {
  try {
    const message = String(req.body.message || "").trim();
    if (!message) return clientError(res, "message is required");

    const feedback = await Feedback.create({
      userId: String(req.body.userId || "").trim() || undefined,
      userEmail: normalizeEmail(req.body.userEmail || req.body.email || ""),
      name: String(req.body.name || "").trim() || undefined,
      email: normalizeEmail(req.body.email || ""),
      rating: parseNumberLike(req.body.rating, undefined),
      message,
    });

    res.status(201).json({ ok: true, feedback });
  } catch (error) {
    res.status(500).json({ ok: false, error: error.message });
  }
});

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

async function ensureCollections() {
  await Promise.all([
    User.createCollection(),
    Order.createCollection(),
    Notification.createCollection(),
    VerificationRequest.createCollection(),
    ChatMessage.createCollection(),
    Feedback.createCollection(),
    Transaction.createCollection(),
  ]);
}

async function seedAdminIfMissing() {
  const adminEmail = normalizeEmail(process.env.ADMIN_EMAIL || "admin@vapilot.local");
  const adminPassword = String(process.env.ADMIN_PASSWORD || "admin123");
  const exists = await User.findOne({ email: adminEmail, role: "admin" }).lean();
  if (!exists) {
    await User.create({
      fullname: "System Admin",
      name: "System Admin",
      email: adminEmail,
      password: adminPassword,
      role: "admin",
      verified: true,
    });
    console.log(`Seeded admin account: ${adminEmail}`);
  }
}

async function start() {
  try {
    await mongoose.connect(MONGO_URI);
    await ensureCollections();
    await seedAdminIfMissing();
    app.listen(PORT, "0.0.0.0", () => {
      console.log(`Server running on port ${PORT}`);
      console.log(`MongoDB connected successfully! (URI hidden for security)`);
      console.log(`MongoDB database: ${mongoose.connection?.name || "unknown"}`);
    });
  } catch (error) {
    console.error("Startup failed:", error.message);
    process.exit(1);
  }
}

start();
