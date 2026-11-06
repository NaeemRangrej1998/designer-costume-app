import { ScrollView, StyleSheet, Text, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { theme } from "../constants/theme";

export function Screen({ children, scroll = true, contentStyle, ...props }) {
  const Content = scroll ? ScrollView : View;
  return (
    <SafeAreaView style={styles.safe} edges={["top", "left", "right"]}>
      <Content
        style={styles.content}
        contentContainerStyle={scroll ? [styles.container, contentStyle] : undefined}
        keyboardShouldPersistTaps="handled"
        {...props}
      >
        {children}
      </Content>
    </SafeAreaView>
  );
}

export function Heading({ eyebrow, title, description }) {
  return (
    <View style={styles.heading}>
      {eyebrow ? <Text style={styles.eyebrow}>{eyebrow.toUpperCase()}</Text> : null}
      <Text style={styles.title}>{title}</Text>
      {description ? <Text style={styles.description}>{description}</Text> : null}
    </View>
  );
}

export function Notice({ children, error = false }) {
  return <Text accessibilityRole="alert" style={[styles.notice, error && styles.error]}>{children}</Text>;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: theme.colors.background },
  content: { flex: 1 },
  container: { padding: theme.spacing.lg, paddingBottom: 48, gap: theme.spacing.md },
  heading: { gap: 7, marginBottom: 8 },
  eyebrow: { color: theme.colors.accent, fontSize: 11, fontWeight: "700", letterSpacing: 2 },
  title: { color: theme.colors.ink, fontSize: 30, fontWeight: "700", letterSpacing: -0.5 },
  description: { color: theme.colors.muted, fontSize: 15, lineHeight: 23 },
  notice: { color: theme.colors.muted, fontSize: 14, lineHeight: 21, paddingVertical: 8 },
  error: { color: theme.colors.error },
});
