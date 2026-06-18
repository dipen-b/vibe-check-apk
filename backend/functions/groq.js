/**
 * Groq AI conversation enhancement — intelligent opening messages & suggestions.
 *
 * Callable functions:
 *   - generateOpeningSuggestions: AI-generated mood-aware opening messages
 *   - generateReplySuggestions: AI-generated contextual reply options
 *
 * Powered by Groq API for fast, quality conversational AI.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

const db = admin.firestore();

// Groq API configuration (set via Firebase Secrets or environment)
const GROQ_API_KEY = process.env.GROQ_API_KEY || "";
const GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

/**
 * Require authentication for callable functions.
 */
function requireAuth(request) {
  const uid = request.auth && request.auth.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Sign in required.");
  return uid;
}

/**
 * Call Groq API with prompt and return text completion.
 */
async function callGroqAPI(systemPrompt, userPrompt) {
  if (!GROQ_API_KEY) {
    logger.warn("GROQ_API_KEY not configured; returning fallback suggestions");
    return null;
  }

  try {
    const response = await fetch(GROQ_API_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${GROQ_API_KEY}`,
      },
      body: JSON.stringify({
        model: "mixtral-8x7b-32768", // Fast, high-quality model
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: userPrompt },
        ],
        temperature: 0.7,
        max_tokens: 200,
      }),
    });

    if (!response.ok) {
      const error = await response.text();
      logger.error("Groq API error", { status: response.status, error });
      return null;
    }

    const data = await response.json();
    const completion = data.choices?.[0]?.message?.content || "";
    return completion;
  } catch (error) {
    logger.error("Groq API call failed", { error: error.message });
    return null;
  }
}

/**
 * Parse suggestions from Groq response (expects JSON format).
 */
function parseSuggestions(response) {
  if (!response) return null;

  try {
    // Try to parse JSON array
    const parsed = JSON.parse(response);
    if (Array.isArray(parsed) && parsed.length > 0) {
      return parsed.filter((s) => typeof s === "string" && s.length > 0).slice(0, 3);
    }
  } catch (e) {
    logger.debug("Could not parse suggestions as JSON; trying line-split");
    // Fallback: split by newlines
    const lines = response
      .split("\n")
      .map((s) => s.trim())
      .filter((s) => s.length > 0 && !s.startsWith("-") && !s.startsWith("•"))
      .slice(0, 3);
    if (lines.length > 0) return lines;
  }

  return null;
}

/**
 * Generate AI opening message suggestions based on moods.
 * Returns 3 contextually-relevant suggestions.
 */
exports.generateOpeningSuggestions = onCall(async (request) => {
  const uid = requireAuth(request);
  const { peerMood, userMood } = request.data || {};

  if (typeof peerMood !== "string" || typeof userMood !== "string") {
    throw new HttpsError("invalid-argument", "peerMood and userMood are required (string).");
  }

  // Groq prompt for opening suggestions
  const systemPrompt = `You are a friendly, empathetic conversation starter for an anonymous mood-matching chat app.
The user has matched with someone based on shared emotional wavelengths.
Generate 3 short, natural opening messages (2-10 words each) that:
- Match the vibe and mood of both participants
- Are warm, non-judgmental, and genuine
- Invite connection without being too forward
- Avoid asking "how are you" (cliché)

Return ONLY a JSON array of strings, one suggestion per element. Example: ["hey, rough day?", "that sounds tough", "same vibe here"]`;

  const userPrompt = `You're feeling ${userMood.toLowerCase()} and just matched with someone feeling ${peerMood.toLowerCase()}.
Generate 3 opening messages for them.`;

  const response = await callGroqAPI(systemPrompt, userPrompt);
  const suggestions = parseSuggestions(response);

  if (suggestions && suggestions.length > 0) {
    logger.info("Generated opening suggestions", { uid, peerMood, userMood, count: suggestions.length });
    return { suggestions };
  }

  // Fallback suggestions if Groq fails
  logger.warn("Using fallback suggestions", { uid, peerMood, userMood });
  return {
    suggestions: [
      `hey, sounds like we're in a similar place`,
      `i get that vibe`,
      `let's talk about it`,
    ],
  };
});

/**
 * Generate AI reply suggestions based on chat context.
 * Returns 3 contextually-relevant response options.
 *
 * Future: call this mid-chat to suggest thoughtful responses.
 */
exports.generateReplySuggestions = onCall(async (request) => {
  const uid = requireAuth(request);
  const { sessionId, lastMessage } = request.data || {};

  if (typeof lastMessage !== "string" || !sessionId) {
    throw new HttpsError("invalid-argument", "sessionId and lastMessage are required.");
  }

  // Fetch chat context from Firestore
  let chatHistory = [];
  try {
    const messagesSnap = await db
      .collection("chatSessions")
      .doc(sessionId)
      .collection("messages")
      .orderBy("sentAt", "desc")
      .limit(5)
      .get();

    chatHistory = messagesSnap.docs
      .reverse()
      .map((d) => ({
        from: d.data().senderUid === uid ? "you" : "them",
        text: d.data().text,
      }));
  } catch (error) {
    logger.debug("Could not fetch chat history", { error: error.message });
  }

  // Groq prompt for reply suggestions
  const systemPrompt = `You are helping someone respond thoughtfully in an anonymous mood-matching chat.
Generate 3 short, authentic reply options (3-15 words each) that:
- Feel genuine and empathetic
- Continue the conversation naturally
- Are supportive without being preachy
- Match the tone of the peer's message

Return ONLY a JSON array of strings. Example: ["yeah totally", "what helped for you?", "same here"]`;

  const contextText = chatHistory
    .map((m) => `${m.from}: ${m.text}`)
    .join("\n");

  const userPrompt = `Chat so far:\n${contextText}\n\nTheir last message: "${lastMessage}"\n\nGenerate 3 thoughtful replies.`;

  const response = await callGroqAPI(systemPrompt, userPrompt);
  const suggestions = parseSuggestions(response);

  if (suggestions && suggestions.length > 0) {
    logger.info("Generated reply suggestions", { sessionId, uid, count: suggestions.length });
    return { suggestions };
  }

  // Fallback suggestions
  logger.warn("Using fallback reply suggestions", { sessionId, uid });
  return {
    suggestions: [
      "yeah, totally get that",
      "what helped you last time?",
      "you're not alone in this",
    ],
  };
});
