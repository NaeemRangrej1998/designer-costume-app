import { useState } from "react";
import { KeyboardAvoidingView, Platform, Text } from "react-native";
import { Screen, Heading, Notice } from "../components/Screen";
import { Button, Field } from "../components/UI";
import { getApiErrorMessage } from "../services/apiClient";
import { useAuth } from "../context/AuthContext";
import { theme } from "../constants/theme";

function validEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

function validPassword(value) {
  const byteLength = utf8ByteLength(value);
  return value.length >= 8 && byteLength <= 72 &&
    /[A-Z]/.test(value) && /[a-z]/.test(value) &&
    /\d/.test(value) && /[^A-Za-z0-9\s]/.test(value);
}

function AuthForm({ children }) {
  return (
    <Screen>
      <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={{ gap: 16 }}>
        {children}
      </KeyboardAvoidingView>
    </Screen>
  );
}

export function LoginScreen({ navigation, route }) {
  const { signIn, signOut } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit() {
    setError("");
    if (!validEmail(email)) { setError("Enter a valid email address."); return; }
    if (!password || utf8ByteLength(password) > 72) { setError("Enter a password of no more than 72 UTF-8 bytes."); return; }
    setLoading(true);
    try {
      const user = await signIn(email.trim(), password);
      if (!user.roles?.includes("CUSTOMER")) {
        await signOut();
        setError("This application is for customer accounts. Please use the admin web application.");
        return;
      }
      continueAfterAuth(navigation, route.params);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Sign in failed. Check your credentials."));
    } finally { setLoading(false); }
  }

  return (
    <AuthForm>
      <Heading eyebrow="DESIGNER COSTUME STUDIO" title="Welcome back" description="Sign in to your customer account." />
      <Field label="Email address" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" />
      <Field label="Password" value={password} onChangeText={setPassword} secureTextEntry autoComplete="password" />
      {error ? <Notice error>{error}</Notice> : null}
      <Button title="Sign in" onPress={submit} loading={loading} />
      <Button title="Create customer account" variant="secondary" onPress={() => navigation.navigate("Register", route.params)} />
      <Button title="Forgot password?" variant="quiet" onPress={() => navigation.navigate("ForgotPassword")} />
      <Text style={{ color: theme.colors.muted, fontSize: 12, lineHeight: 18 }}>Refresh tokens are stored in the device secure store. Access tokens stay in memory.</Text>
    </AuthForm>
  );
}

export function RegisterScreen({ navigation, route }) {
  const { register } = useAuth();
  const [form, setForm] = useState({ fullName: "", email: "", phoneNumber: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const update = (key, value) => setForm((current) => ({ ...current, [key]: value }));

  async function submit() {
    setError("");
    if (!form.fullName.trim()) { setError("Enter your full name."); return; }
    if (!validEmail(form.email)) { setError("Enter a valid email address."); return; }
    if (form.phoneNumber.length > 32) { setError("Phone number must be 32 characters or fewer."); return; }
    if (!validPassword(form.password)) { setError("Use 8–72 UTF-8 bytes with uppercase, lowercase, a number, and a special character."); return; }
    setLoading(true);
    try {
      await register({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phoneNumber: form.phoneNumber.trim() || null,
        password: form.password,
      });
      continueAfterAuth(navigation, route.params);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "Account registration failed."));
    } finally { setLoading(false); }
  }

  return (
    <AuthForm>
      <Heading eyebrow="JOIN THE BOUTIQUE" title="Create an account" description="Register for a customer account." />
      <Field label="Full name" value={form.fullName} onChangeText={(value) => update("fullName", value)} autoComplete="name" maxLength={150} />
      <Field label="Email address" value={form.email} onChangeText={(value) => update("email", value)} autoCapitalize="none" keyboardType="email-address" autoComplete="email" maxLength={320} />
      <Field label="Phone number (optional)" value={form.phoneNumber} onChangeText={(value) => update("phoneNumber", value)} keyboardType="phone-pad" autoComplete="tel" maxLength={32} />
      <Field label="Password" value={form.password} onChangeText={(value) => update("password", value)} secureTextEntry autoComplete="new-password" />
      <Text style={{ color: theme.colors.muted, fontSize: 12 }}>8–72 UTF-8 bytes; uppercase, lowercase, number, and special character.</Text>
      {error ? <Notice error>{error}</Notice> : null}
      <Button title="Create customer account" onPress={submit} loading={loading} />
      <Button title="Already have an account? Sign in" variant="secondary" onPress={() => navigation.navigate("Login", route.params)} />
    </AuthForm>
  );
}

export function ForgotPasswordScreen() {
  const { requestReset, submitReset } = useAuth();
  const [email, setEmail] = useState("");
  const [token, setToken] = useState("");
  const [password, setPassword] = useState("");
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function requestLink() {
    setError(""); setNotice("");
    if (!validEmail(email)) { setError("Enter a valid email address."); return; }
    setLoading(true);
    try {
      const response = await requestReset(email.trim());
      if (response?.developmentResetToken) setToken(response.developmentResetToken);
      setNotice(response?.message || "If the account exists, password reset instructions will be sent.");
    } catch (requestError) { setError(getApiErrorMessage(requestError, "Password reset could not be requested.")); }
    finally { setLoading(false); }
  }

  async function changePassword() {
    setError(""); setNotice("");
    if (!token.trim()) { setError("Enter the reset token from your email."); return; }
    if (!validPassword(password)) { setError("Use 8–72 UTF-8 bytes with uppercase, lowercase, a number, and a special character."); return; }
    setLoading(true);
    try {
      await submitReset(token.trim(), password);
      setNotice("Password reset successfully. You can now sign in.");
      setToken(""); setPassword("");
    } catch (requestError) { setError(getApiErrorMessage(requestError, "Password reset failed.")); }
    finally { setLoading(false); }
  }

  return (
    <AuthForm>
      <Heading eyebrow="ACCOUNT SECURITY" title="Reset your password" description="Request reset instructions, then use the token from your email to set a new password." />
      <Field label="Account email" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="email" />
      <Button title="Request reset instructions" variant="secondary" onPress={requestLink} loading={loading} />
      <Field label="Reset token" value={token} onChangeText={setToken} autoCapitalize="none" />
      <Field label="New password" value={password} onChangeText={setPassword} secureTextEntry autoComplete="new-password" />
      <Button title="Reset password" onPress={changePassword} loading={loading} />
      {notice ? <Notice>{notice}</Notice> : null}
      {error ? <Notice error>{error}</Notice> : null}
    </AuthForm>
  );
}

function continueAfterAuth(navigation, params) {
  if (params?.returnTo) {
    navigation.navigate(params.returnTo, params.returnParams);
  } else if (navigation.canGoBack()) {
    navigation.goBack();
  } else {
    navigation.navigate("MainTabs", { screen: "Home" });
  }
}

function utf8ByteLength(value) {
  return Array.from(value).reduce((total, character) => {
    const codePoint = character.codePointAt(0);
    return total + (codePoint <= 0x7f ? 1 : codePoint <= 0x7ff ? 2 : codePoint <= 0xffff ? 3 : 4);
  }, 0);
}
