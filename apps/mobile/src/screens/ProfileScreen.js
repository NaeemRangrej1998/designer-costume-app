import { ActivityIndicator, Alert, Text, View } from "react-native";
import { Screen, Heading, Notice } from "../components/Screen";
import { Button, Panel } from "../components/UI";
import { useAuth } from "../context/AuthContext";
import { theme } from "../constants/theme";

export default function ProfileScreen({ navigation }) {
  const { user, status, signOut, restoreError } = useAuth();
  if (status === "loading") return <Screen><ActivityIndicator color={theme.colors.accent} /><Notice>Restoring your account…</Notice></Screen>;
  if (!user) {
    return (
      <Screen>
        <Heading eyebrow="YOUR ACCOUNT" title="Welcome to the studio" description="Sign in or create a customer account to prepare for future personalized experiences." />
        {restoreError ? <Notice error>{restoreError}</Notice> : null}
        <Button title="Sign in" onPress={() => navigation.navigate("Login")} />
        <Button title="Create customer account" variant="secondary" onPress={() => navigation.navigate("Register")} />
      </Screen>
    );
  }
  return (
    <Screen>
      <Heading eyebrow="YOUR ACCOUNT" title="Profile" description="Your customer account information." />
      <Panel>
        <ProfileField label="Name" value={user.fullName} />
        <ProfileField label="Email" value={user.email} />
        <ProfileField label="Phone" value={user.phoneNumber || "Not provided"} />
      </Panel>
      <Button title="Contact the boutique" variant="quiet" onPress={() => navigation.navigate("Contact")} />
      <Button title="Sign out" variant="secondary" onPress={() => signOut().catch((error) => Alert.alert("Signed out on this device", error.message || "The server could not invalidate the remote session."))} />
    </Screen>
  );
}

function ProfileField({ label, value }) {
  return <View style={{ gap: 4 }}><Text style={{ color: theme.colors.muted, fontSize: 11 }}>{label}</Text><Text selectable style={{ color: theme.colors.ink, fontSize: 15, fontWeight: "600" }}>{value || "Not provided"}</Text></View>;
}
