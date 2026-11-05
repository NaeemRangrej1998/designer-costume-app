import { StatusBar, StyleSheet, Text, View } from "react-native";

export default function App() {
  return (
    <View style={styles.container}>
      <Text style={styles.eyebrow}>THE BOUTIQUE EXPERIENCE</Text>
      <Text style={styles.title}>Designer Costume Studio</Text>
      <Text style={styles.description}>
        A new way to explore designer costume is taking shape.
      </Text>
      <StatusBar barStyle="dark-content" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#f8f6f2",
    paddingHorizontal: 28,
  },
  eyebrow: {
    marginBottom: 16,
    color: "#92400e",
    fontSize: 12,
    fontWeight: "700",
    letterSpacing: 3,
    textAlign: "center",
  },
  title: {
    color: "#24211f",
    fontSize: 36,
    fontWeight: "700",
    textAlign: "center",
  },
  description: {
    maxWidth: 340,
    marginTop: 20,
    color: "#57534e",
    fontSize: 17,
    lineHeight: 26,
    textAlign: "center",
  },
});
