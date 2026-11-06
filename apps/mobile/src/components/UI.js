import { useState } from "react";
import { ActivityIndicator, Image, Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { getApiBaseUrl } from "../services/apiClient";
import { theme } from "../constants/theme";

export function Button({ title, onPress, variant = "primary", disabled = false, loading = false, style }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: disabled || loading }}
      onPress={onPress}
      disabled={disabled || loading}
      style={({ pressed }) => [
        styles.button,
        variant === "secondary" && styles.secondary,
        variant === "quiet" && styles.quiet,
        (disabled || loading) && styles.disabled,
        pressed && !disabled && styles.pressed,
        style,
      ]}
    >
      {loading ? <ActivityIndicator color={variant === "primary" ? "#fff" : theme.colors.accentDark} /> : <Text style={[styles.buttonText, variant !== "primary" && styles.secondaryText]}>{title}</Text>}
    </Pressable>
  );
}

export function Field({ label, error, multiline, style, ...props }) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        accessibilityLabel={label}
        placeholderTextColor="#a29c93"
        style={[styles.input, multiline && styles.multiline, style]}
        multiline={multiline}
        textAlignVertical={multiline ? "top" : "center"}
        {...props}
      />
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );
}

export function Panel({ children, style }) {
  return <View style={[styles.panel, style]}>{children}</View>;
}

export function CostumeImage({ uri, style, resizeMode = "cover" }) {
  const [failed, setFailed] = useState(false);
  if (!uri || failed) {
    return <View style={[styles.imageFallback, style]}><Text style={styles.fallbackMark}>DS</Text><Text style={styles.fallbackText}>BOUTIQUE EDIT</Text></View>;
  }
  const resolvedUri = /^https?:\/\//i.test(uri)
    ? uri
    : new URL(uri, `${getApiBaseUrl().replace(/\/+$/, "")}/`).toString();
  return <Image source={{ uri: resolvedUri }} style={style} resizeMode={resizeMode} onError={() => setFailed(true)} />;
}

const styles = StyleSheet.create({
  button: { minHeight: 48, paddingHorizontal: 18, borderRadius: theme.radius.sm, backgroundColor: theme.colors.accentDark, alignItems: "center", justifyContent: "center" },
  secondary: { backgroundColor: theme.colors.pale },
  quiet: { backgroundColor: "transparent", borderWidth: 1, borderColor: theme.colors.border },
  disabled: { opacity: 0.48 },
  pressed: { opacity: 0.82 },
  buttonText: { color: "#fff", fontSize: 14, fontWeight: "700" },
  secondaryText: { color: theme.colors.accentDark },
  field: { gap: 7 },
  label: { color: theme.colors.ink, fontSize: 13, fontWeight: "600" },
  input: { minHeight: 48, borderColor: theme.colors.border, borderWidth: 1, borderRadius: theme.radius.sm, paddingHorizontal: 14, backgroundColor: theme.colors.surface, color: theme.colors.ink, fontSize: 15 },
  multiline: { minHeight: 116, paddingTop: 12 },
  error: { color: theme.colors.error, fontSize: 12 },
  panel: { padding: theme.spacing.md, borderRadius: theme.radius.md, backgroundColor: theme.colors.surface, borderWidth: 1, borderColor: theme.colors.border, gap: 9 },
  imageFallback: { backgroundColor: theme.colors.pale, alignItems: "center", justifyContent: "center", gap: 7 },
  fallbackMark: { fontSize: 28, color: theme.colors.accent, fontWeight: "700" },
  fallbackText: { fontSize: 9, color: theme.colors.muted, letterSpacing: 1.5 },
});
