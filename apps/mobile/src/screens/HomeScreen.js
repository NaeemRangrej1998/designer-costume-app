import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from "react-native";
import { Screen, Heading } from "../components/Screen";
import { Button, Panel } from "../components/UI";
import { CategoryCard, CostumeCard } from "../components/CatalogCards";
import { getCategories, getCostumes } from "../services/catalogService";
import { getApiErrorMessage } from "../services/apiClient";
import { theme } from "../constants/theme";

export default function HomeScreen({ navigation }) {
  const [categories, setCategories] = useState([]);
  const [costumes, setCostumes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setError("");
    try {
      const [categoryData, costumeData] = await Promise.all([
        getCategories({ page: 0, size: 8 }),
        getCostumes({ page: 0, size: 8 }),
      ]);
      setCategories(categoryData.content || []);
      setCostumes(costumeData.content || []);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, "The boutique collection could not be loaded."));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  return (
    <Screen refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={theme.colors.accent} />}>
      <View style={styles.brandRow}>
        <View><Text style={styles.brandMark}>DS</Text></View>
        <View style={styles.brandText}><Text style={styles.brandName}>Designer Costume</Text><Text style={styles.brandSub}>STUDIO</Text></View>
        <Pressable onPress={() => navigation.navigate("Profile")} style={styles.profileButton}><Text style={styles.profileIcon}>○</Text></Pressable>
      </View>
      <View style={styles.hero}>
        <Text style={styles.eyebrow}>DRESS FOR YOUR MOMENT</Text>
        <Text style={styles.heroTitle}>Find Your{"\n"}Perfect Celebration Look</Text>
        <Text style={styles.heroBody}>Discover designer outfits for your special moments.</Text>
        <Button title="Explore collection  →" onPress={() => navigation.navigate("Costumes")} style={styles.heroButton} />
      </View>
      {error ? <Panel><Text style={styles.error}>{error}</Text><Button title="Try again" variant="secondary" onPress={load} /></Panel> : null}
      <View style={styles.sectionHeader}>
        <Heading eyebrow="A WORLD OF OCCASIONS" title="Featured categories" />
        <Pressable onPress={() => navigation.navigate("Categories")}><Text style={styles.link}>See all  →</Text></Pressable>
      </View>
      {loading ? <ActivityIndicator color={theme.colors.accent} /> : categories.length ? (
        <FlatList horizontal data={categories.slice(0, 6)} keyExtractor={(item) => item.id} showsHorizontalScrollIndicator={false} contentContainerStyle={styles.horizontalList}
          renderItem={({ item }) => <CategoryCard category={item} horizontal onPress={() => navigation.navigate("Costumes", { categoryId: item.id, categoryName: item.name })} />} />
      ) : <Text style={styles.muted}>No active categories are available right now.</Text>}
      <View style={styles.sectionHeader}>
        <Heading eyebrow="THE BOUTIQUE SELECTION" title="Featured costumes" />
        <Pressable onPress={() => navigation.navigate("Costumes")}><Text style={styles.link}>View all  →</Text></Pressable>
      </View>
      {loading ? <ActivityIndicator color={theme.colors.accent} /> : costumes.length ? (
        <FlatList horizontal data={costumes.slice(0, 6)} keyExtractor={(item) => item.id} showsHorizontalScrollIndicator={false} contentContainerStyle={styles.horizontalList}
          renderItem={({ item }) => <CostumeCard costume={item} horizontal onPress={() => navigation.navigate("CostumeDetails", { costumeId: item.id })} />} />
      ) : <Text style={styles.muted}>No active costumes are available to show yet.</Text>}
      <Panel style={styles.tryPanel}>
        <Text style={styles.eyebrow}>A NEW WAY TO EXPLORE</Text>
        <Text style={styles.panelTitle}>Prepare for a future virtual try-on</Text>
        <Text style={styles.muted}>Choose a costume and preview your photo locally. AI processing is not enabled yet.</Text>
        <Button title="Explore try-on preparation" variant="secondary" onPress={() => navigation.navigate("TryOn")} />
      </Panel>
      <Panel>
        <Text style={styles.panelTitle}>The studio difference</Text>
        <Text style={styles.muted}>Designer collections · Multiple available colors and sizes · Personalized boutique assistance.</Text>
        <Button title="Contact the boutique" variant="quiet" onPress={() => navigation.navigate("Contact")} />
      </Panel>
    </Screen>
  );
}

const styles = StyleSheet.create({
  brandRow: { flexDirection: "row", alignItems: "center", gap: 10, marginBottom: 4 },
  brandMark: { width: 42, height: 42, textAlign: "center", textAlignVertical: "center", backgroundColor: "#d8bc93", borderRadius: 14, color: "#fff", fontWeight: "800", overflow: "hidden" },
  brandText: { flex: 1, gap: 1 },
  brandName: { color: theme.colors.ink, fontSize: 15, fontWeight: "700" },
  brandSub: { color: theme.colors.muted, fontSize: 9, letterSpacing: 2 },
  profileButton: { width: 42, height: 42, borderRadius: 21, alignItems: "center", justifyContent: "center", backgroundColor: theme.colors.pale },
  profileIcon: { fontSize: 24, color: theme.colors.accentDark },
  hero: { padding: 23, borderRadius: theme.radius.lg, backgroundColor: "#3d3228", gap: 14 },
  eyebrow: { color: theme.colors.accent, fontSize: 10, fontWeight: "700", letterSpacing: 1.8 },
  heroTitle: { color: "#fff", fontSize: 30, lineHeight: 37, fontWeight: "700" },
  heroBody: { color: "#e5ded4", fontSize: 14, lineHeight: 21 },
  heroButton: { alignSelf: "flex-start", backgroundColor: "#a48258" },
  sectionHeader: { flexDirection: "row", alignItems: "flex-end", justifyContent: "space-between", marginTop: 8 },
  link: { color: theme.colors.accentDark, fontSize: 12, fontWeight: "700", paddingBottom: 10 },
  horizontalList: { gap: 12, paddingRight: 20 },
  muted: { color: theme.colors.muted, fontSize: 14, lineHeight: 21 },
  error: { color: theme.colors.error },
  tryPanel: { backgroundColor: "#efe9df" },
  panelTitle: { color: theme.colors.ink, fontSize: 18, fontWeight: "700" },
});
